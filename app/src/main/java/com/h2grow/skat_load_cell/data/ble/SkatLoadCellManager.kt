package com.h2grow.skat_load_cell.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import com.h2grow.skat_load_cell.domain.model.CommandResult
import com.h2grow.skat_load_cell.domain.model.Telemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withTimeout
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.ble.ktx.asFlow
import no.nordicsemi.android.ble.ktx.getCharacteristic
import no.nordicsemi.android.ble.ktx.state.ConnectionState
import no.nordicsemi.android.ble.ktx.stateAsFlow
import no.nordicsemi.android.ble.ktx.suspend
import org.json.JSONObject

@OptIn(ExperimentalCoroutinesApi::class)
class SkatLoadCellManager(
    context: Context,
) : BleManager(context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var telemetryCharacteristic: BluetoothGattCharacteristic? = null
    private var commandCharacteristic: BluetoothGattCharacteristic? = null
    private var responseCharacteristic: BluetoothGattCharacteristic? = null

    private val responseChannel = Channel<String>(capacity = 1)

    private val _telemetry = MutableStateFlow(Telemetry())
    val telemetry: StateFlow<Telemetry> = _telemetry.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = stateAsFlow()
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ConnectionState.Disconnected(
                ConnectionState.Disconnected.Reason.UNKNOWN,
            ),
        )

    override fun getMinLogPriority(): Int = Log.INFO

    override fun log(priority: Int, message: String) {
        Log.println(priority, TAG, message)
    }

    override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
        val service = gatt.getService(SkatLoadCellSpec.SERVICE) ?: return false

        telemetryCharacteristic = service.getCharacteristic(
            SkatLoadCellSpec.TELEMETRY,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
        )
        commandCharacteristic = service.getCharacteristic(
            SkatLoadCellSpec.COMMAND,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
        )
        responseCharacteristic = service.getCharacteristic(
            SkatLoadCellSpec.RESPONSE,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
        )

        return telemetryCharacteristic != null &&
            commandCharacteristic != null &&
            responseCharacteristic != null
    }

    override fun initialize() {
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
        telemetryCharacteristic = null
        commandCharacteristic = null
        responseCharacteristic = null
        _telemetry.value = Telemetry()
        while (responseChannel.tryReceive().isSuccess) { /* drain */ }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        connect(device)
            .retry(3, 300)
            .useAutoConnect(false)
            .timeout(15_000)
            .enqueue()
    }

    @SuppressLint("MissingPermission")
    suspend fun connectToDeviceAndWait(device: BluetoothDevice) {
        connect(device)
            .retry(3, 300)
            .useAutoConnect(false)
            .timeout(15_000)
            .suspend()
    }

    @SuppressLint("MissingPermission")
    fun disconnectDevice() {
        if (isReady) {
            disconnect().enqueue()
        } else {
            cancelQueue()
        }
    }

    fun release() {
        scope.cancel()
        val wasConnected = isReady
        cancelQueue()
        if (wasConnected) {
            disconnect().enqueue()
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
        parseTelemetryJson(json)?.let { _telemetry.value = it }
    }

    private fun parseTelemetryJson(json: String): Telemetry? = try {
        val obj = JSONObject(json)
        Telemetry(
            forceGrams = obj.optDouble("force_g", 0.0).toFloat(),
            currentAmps = obj.optDouble("current_a", 0.0).toFloat(),
            busVoltage = obj.optDouble("bus_v", 0.0).toFloat(),
            hx711Ok = obj.optBoolean("hx711_ok", false),
            ina226Ok = obj.optBoolean("ina226_ok", false),
            ina226CalOk = obj.optBoolean("ina226_cal", false),
            ina226Addr = obj.optInt("ina226_addr", 0),
            i2cScan = obj.optString("i2c_scan", ""),
            scale = obj.optDouble("scale", 0.0).toFloat(),
        )
    } catch (e: Exception) {
        log(Log.WARN, "Bad telemetry JSON: $json (${e.message})")
        null
    }

    private fun parseCommandResult(rawJson: String): CommandResult {
        return try {
            val obj = JSONObject(rawJson)
            val ok = obj.optBoolean("ok", false)

            if (ok && obj.optString("type") == "data") {
                parseTelemetryJson(rawJson)?.let { _telemetry.value = it }
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

    private companion object {
        const val TAG = "SkatLoadCellManager"
        const val COMMAND_TIMEOUT_MS = 5_000L
    }
}
