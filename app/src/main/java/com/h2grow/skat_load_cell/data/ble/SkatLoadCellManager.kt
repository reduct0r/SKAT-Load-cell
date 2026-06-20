package com.h2grow.skat_load_cell.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.util.Log
import com.h2grow.skat_load_cell.domain.model.CommandResult
import com.h2grow.skat_load_cell.domain.model.Telemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withTimeout
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.ble.ktx.asFlow
import no.nordicsemi.android.ble.ktx.suspend
import org.json.JSONObject
import java.util.UUID
import kotlin.math.roundToInt

@OptIn(ExperimentalCoroutinesApi::class)
class SkatLoadCellManager(
    context: Context,
) : BleManager(context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var telemetryCharacteristic: BluetoothGattCharacteristic? = null
    private var commandCharacteristic: BluetoothGattCharacteristic? = null
    private var responseCharacteristic: BluetoothGattCharacteristic? = null

    private val responseChannel = kotlinx.coroutines.channels.Channel<String>(capacity = 1)

    private val _telemetry = MutableStateFlow(Telemetry())
    val telemetry: StateFlow<Telemetry> = _telemetry.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    var connectedDeviceName: String? = null
        private set

    private var pendingPwmPercent: Float? = null
    private var lastPwmSentAtMs = 0L
    private var lastSentPwmPercent: Float? = null
    private var pwmSendJob: Job? = null

    override fun getMinLogPriority(): Int = Log.INFO

    override fun log(priority: Int, message: String) {
        Log.println(priority, TAG, message)
    }

    override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
        val service = findSkatService(gatt)
        if (service == null) {
            log(Log.WARN, "SKAT service not found. Services: ${gatt.services.map { it.uuid }}")
            return false
        }

        log(Log.INFO, "SKAT service: ${service.uuid}")
        service.characteristics.forEach { characteristic ->
            log(Log.INFO, "  ${characteristic.uuid} props=0x${characteristic.properties.toString(16)}")
        }

        telemetryCharacteristic = service.findCharacteristic(SkatLoadCellSpec.TELEMETRY)
            ?: service.findCharacteristic(SkatLoadCellSpec.LEGACY_TELEMETRY)
        commandCharacteristic = service.findCharacteristic(SkatLoadCellSpec.COMMAND)
            ?: service.findCharacteristic(SkatLoadCellSpec.LEGACY_COMMAND)
        responseCharacteristic = service.findCharacteristic(SkatLoadCellSpec.RESPONSE)
            ?: service.findCharacteristic(SkatLoadCellSpec.LEGACY_RESPONSE)

        resolveCharacteristicsByProperties(service)

        val ok = telemetryCharacteristic != null &&
            commandCharacteristic != null &&
            responseCharacteristic != null

        if (!ok) {
            log(
                Log.WARN,
                "Missing characteristics. telemetry=${telemetryCharacteristic != null}, " +
                    "command=${commandCharacteristic != null}, response=${responseCharacteristic != null}",
            )
        }
        return ok
    }

    private fun findSkatService(gatt: BluetoothGatt): BluetoothGattService? {
        gatt.getService(SkatLoadCellSpec.SERVICE)?.let { return it }
        gatt.getService(SkatLoadCellSpec.LEGACY_SERVICE)?.let { return it }
        return gatt.services.firstOrNull { it.uuid !in SkatLoadCellSpec.STANDARD_BLE_SERVICES }
    }

    private fun resolveCharacteristicsByProperties(service: BluetoothGattService) {
        if (commandCharacteristic == null) {
            commandCharacteristic = service.characteristics.firstOrNull { characteristic ->
                val props = characteristic.properties
                val canWrite = props and (
                    BluetoothGattCharacteristic.PROPERTY_WRITE or
                        BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
                    ) != 0
                canWrite && props and BluetoothGattCharacteristic.PROPERTY_NOTIFY == 0
            }
        }

        val notifyCharacteristics = service.characteristics.filter { characteristic ->
            characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
        }

        if (telemetryCharacteristic == null && notifyCharacteristics.isNotEmpty()) {
            telemetryCharacteristic = notifyCharacteristics.first()
        }
        if (responseCharacteristic == null && notifyCharacteristics.size >= 2) {
            responseCharacteristic = notifyCharacteristics[1]
        }
    }

    override fun initialize() {
        _isConnected.value = true
        requestMtu(247).enqueue()

        telemetryCharacteristic?.let { characteristic ->
            setNotificationCallback(characteristic)
                .asFlow()
                .onEach { data -> parseTelemetry(data) }
                .launchIn(scope)

            enableNotifications(characteristic).enqueue()
            readCharacteristic(characteristic)
                .with { _, data -> parseTelemetry(data) }
                .enqueue()
        }

        responseCharacteristic?.let { characteristic ->
            setNotificationCallback(characteristic)
                .asFlow()
                .onEach { data ->
                    data.getStringValue(0)?.let { json ->
                        responseChannel.trySend(json)
                    }
                }
                .launchIn(scope)

            enableNotifications(characteristic).enqueue()
        }
    }

    override fun onServicesInvalidated() {
        pwmSendJob?.cancel()
        pwmSendJob = null
        pendingPwmPercent = null
        lastSentPwmPercent = null
        _isConnected.value = false
        telemetryCharacteristic = null
        commandCharacteristic = null
        responseCharacteristic = null
        _telemetry.value = Telemetry()
        connectedDeviceName = null
        while (responseChannel.tryReceive().isSuccess) { /* drain */ }
    }

    @SuppressLint("MissingPermission")
    suspend fun connectToDeviceAndWait(device: BluetoothDevice) {
        connectedDeviceName = device.name?.takeIf { it.isNotBlank() } ?: device.address

        if (isReady) {
            disconnect().suspend()
            delay(300)
        } else {
            cancelQueue()
        }

        connect(device)
            .retry(3, 300)
            .useAutoConnect(false)
            .timeout(15_000)
            .suspend()

        if (!isReady) {
            throw IllegalStateException("Устройство не готово после подключения")
        }

        try {
            requestData()
        } catch (e: Exception) {
            log(Log.WARN, "Первый запрос данных не удался: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnectDevice() {
        if (isReady) {
            disconnect().enqueue()
        } else {
            cancelQueue()
        }
    }

    suspend fun requestData(): CommandResult =
        sendCommand(JSONObject().put("cmd", "get"))

    suspend fun tare(): CommandResult =
        sendCommand(JSONObject().put("cmd", "tare"))

    suspend fun calibrate(grams: Float): CommandResult =
        sendCommand(
            JSONObject()
                .put("cmd", "calibrate")
                .put("grams", grams.toDouble()),
        )

    suspend fun setScale(scale: Float): CommandResult =
        sendCommand(
            JSONObject()
                .put("cmd", "set_scale")
                .put("scale", scale.toDouble()),
        )

    suspend fun reset(): CommandResult =
        sendCommand(JSONObject().put("cmd", "reset"))

    suspend fun calibrateBusVoltage(refVolts: Float): CommandResult =
        sendCommand(
            JSONObject()
                .put("cmd", "calibrate_bus_v")
                .put("ref_v", refVolts.toDouble()),
        )

    suspend fun recalibrateIna226(): CommandResult =
        sendCommand(JSONObject().put("cmd", "recal_ina226"))

    suspend fun zeroCurrent(): CommandResult =
        sendCommand(JSONObject().put("cmd", "zero_current"))

    suspend fun setCurrentSign(sign: Int): CommandResult =
        sendCommand(
            JSONObject()
                .put("cmd", "set_current_sign")
                .put("sign", sign),
        )

    suspend fun setShunt(extOhm: Float): CommandResult =
        sendCommand(
            JSONObject()
                .put("cmd", "set_shunt")
                .put("ext_ohm", extOhm.toDouble())
                .put("brd_ohm", 0.1)
                .put("include_brd", false),
        )

    suspend fun setForceSign(sign: Int): CommandResult =
        sendCommand(
            JSONObject()
                .put("cmd", "set_force_sign")
                .put("sign", sign),
        )

    suspend fun armMotors(): CommandResult =
        sendCommand(JSONObject().put("cmd", "arm"))

    suspend fun disarmMotors(): CommandResult =
        sendCommand(JSONObject().put("cmd", "disarm"))

    @SuppressLint("MissingPermission")
    fun sendMotorPwm(percent: Float) {
        if (!isReady) return
        val rounded = roundPwmPercent(percent.coerceIn(0f, 100f))
        pendingPwmPercent = rounded

        if (rounded == lastSentPwmPercent) return

        val now = System.currentTimeMillis()
        val elapsed = now - lastPwmSentAtMs
        if (elapsed >= PWM_MIN_INTERVAL_MS) {
            flushMotorPwm(force = false)
        } else if (pwmSendJob?.isActive != true) {
            pwmSendJob = scope.launch {
                delay(PWM_MIN_INTERVAL_MS - elapsed)
                flushMotorPwm(force = false)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun flushPendingMotorPwm() {
        pwmSendJob?.cancel()
        pwmSendJob = null
        flushMotorPwm(force = true)
    }

    @SuppressLint("MissingPermission")
    private fun flushMotorPwm(force: Boolean) {
        val pct = pendingPwmPercent ?: return
        val rounded = roundPwmPercent(pct)
        if (!force && rounded == lastSentPwmPercent) {
            pendingPwmPercent = null
            return
        }
        pendingPwmPercent = null
        val commandChar = commandCharacteristic ?: return
        if (!isReady) return

        lastPwmSentAtMs = System.currentTimeMillis()
        lastSentPwmPercent = rounded
        val payload = JSONObject()
            .put("cmd", "set_pwm")
            .put("pct", rounded.toDouble())
            .toString()

        writeCharacteristic(
            commandChar,
            Data.from(payload),
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE,
        ).enqueue()
    }

    private fun roundPwmPercent(pct: Float): Float =
        (pct * 100f).roundToInt() / 100f

    @SuppressLint("MissingPermission")
    private suspend fun sendCommand(payload: JSONObject): CommandResult {
        val commandChar = commandCharacteristic
            ?: return CommandResult(false, "", error = "not connected")

        while (responseChannel.tryReceive().isSuccess) { /* drop stale */ }

        writeCharacteristic(
            commandChar,
            Data.from(payload.toString()),
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
        ).suspend()

        val rawJson = withTimeout(COMMAND_TIMEOUT_MS) {
            responseChannel.receive()
        }

        return parseCommandResult(rawJson)
    }

    private fun parseTelemetry(data: Data) {
        val json = data.getStringValue(0) ?: return
        if (json.isBlank() || json == "{}") return
        parseTelemetryJson(json)?.let { _telemetry.value = it }
    }

    private fun parseTelemetryJson(json: String): Telemetry? = try {
        val obj = JSONObject(json)
        val forceGrams = obj.optDouble("force_g", 0.0).toFloat()
        val forceNewtons = if (obj.has("force_n")) {
            obj.optDouble("force_n", 0.0).toFloat()
        } else {
            forceGrams * GRAVITY_MS2
        }
        Telemetry(
            forceGrams = forceGrams,
            forceNewtons = forceNewtons,
            currentAmps = obj.optDouble("current_a", 0.0).toFloat(),
            busVoltage = obj.optDouble("bus_v", 0.0).toFloat(),
            hx711Ok = obj.optBoolean("hx711_ok", false),
            ina226Ok = obj.optBoolean("ina226_ok", false),
            ina226CalOk = obj.optBoolean("ina226_cal", false),
            ina226Addr = obj.optInt("ina226_addr", 0),
            i2cScan = obj.optString("i2c_scan", ""),
            scale = obj.optDouble("scale", 0.0).toFloat(),
            motorsArmed = obj.optBoolean("motors_armed", false),
            motorPwmPercent = obj.optDouble("motor_pwm_pct", 0.0).toFloat(),
            motorPwmRaw = obj.optInt("motor_pwm", obj.optInt("esc_pulse_us", 0)),
            escPulseUs = obj.optInt("esc_pulse_us", obj.optInt("motor_pwm", 0)),
            escMinUs = obj.optInt("esc_min_us", 1000),
            escMaxUs = obj.optInt("esc_max_us", 2000),
            shuntMv = obj.optDouble("shunt_mv", 0.0).toFloat(),
            shuntOhm = obj.optDouble("shunt_ohm", 0.005).toFloat(),
            shuntExtOhm = obj.optDouble("shunt_ext", obj.optDouble("shunt_ohm", 0.005)).toFloat(),
            includeBoardShunt = obj.optBoolean("include_brd", false),
            currentSign = obj.optInt("current_sign", 1),
            forceSign = obj.optInt("force_sign", 1),
            busVScaleE4 = obj.optInt("bus_v_scale_e4", 10000),
            hx711Raw = obj.optLong("hx711_raw", 0),
        )
    } catch (e: Exception) {
        log(Log.WARN, "Bad telemetry JSON: $json (${e.message})")
        null
    }

    private fun parseCommandResult(rawJson: String): CommandResult {
        return try {
            val obj = JSONObject(rawJson)
            val ok = obj.optBoolean("ok", false)

            if (ok) {
                when (obj.optString("type")) {
                    "data" -> parseTelemetryJson(rawJson)?.let { _telemetry.value = it }
                    else -> mergeCommandIntoTelemetry(obj)
                }
            }

            CommandResult(
                ok = ok,
                rawJson = rawJson,
                error = obj.optString("error").takeIf { it.isNotEmpty() },
                cmd = obj.optString("cmd").takeIf { it.isNotEmpty() },
            )
        } catch (e: Exception) {
            CommandResult(ok = false, rawJson = rawJson, error = e.message)
        }
    }

    private fun mergeCommandIntoTelemetry(obj: JSONObject) {
        val current = _telemetry.value
        _telemetry.value = current.copy(
            forceGrams = if (obj.has("force_g")) {
                obj.optDouble("force_g", current.forceGrams.toDouble()).toFloat()
            } else {
                current.forceGrams
            },
            forceNewtons = if (obj.has("force_g")) {
                obj.optDouble("force_g", current.forceGrams.toDouble()).toFloat() * GRAVITY_MS2
            } else {
                current.forceNewtons
            },
            scale = if (obj.has("scale")) {
                obj.optDouble("scale", current.scale.toDouble()).toFloat()
            } else {
                current.scale
            },
            hx711Raw = if (obj.has("counts")) {
                obj.optLong("counts", current.hx711Raw)
            } else {
                current.hx711Raw
            },
        )
    }

    private fun BluetoothGattService.findCharacteristic(uuid: UUID): BluetoothGattCharacteristic? =
        getCharacteristic(uuid) ?: characteristics.firstOrNull { it.uuid == uuid }

    private companion object {
        const val TAG = "SkatLoadCellManager"
        const val COMMAND_TIMEOUT_MS = 5_000L
        const val PWM_MIN_INTERVAL_MS = 33L
        const val GRAVITY_MS2 = 0.00980665f
    }
}
