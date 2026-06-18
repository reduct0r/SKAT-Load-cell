package com.h2grow.skat_load_cell.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.h2grow.skat_load_cell.domain.model.ScannedDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat
import no.nordicsemi.android.support.v18.scanner.ScanCallback
import no.nordicsemi.android.support.v18.scanner.ScanFilter
import no.nordicsemi.android.support.v18.scanner.ScanResult
import no.nordicsemi.android.support.v18.scanner.ScanSettings

class BleScanner(
    private val context: Context,
) {

    sealed interface State {
        data object Idle : State
        data object Scanning : State
        data class Failed(val reason: String) : State
    }

    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private val adapter: BluetoothAdapter?
        get() = bluetoothManager.adapter

    private val scanner: BluetoothLeScannerCompat =
        BluetoothLeScannerCompat.getScanner()

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _devices = MutableStateFlow<List<ScannedDevice>>(emptyList())
    val devices: StateFlow<List<ScannedDevice>> = _devices.asStateFlow()

    private var scanCallback: ScanCallback? = null

    fun isReady(): Boolean = adapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun startScan(filters: List<ScanFilter> = emptyList()): Boolean {
        if (_state.value is State.Scanning) return true

        if (adapter?.isEnabled != true) {
            _state.value = State.Failed("Bluetooth выключен")
            return false
        }

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                publishResult(result)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach(::publishResult)
            }

            override fun onScanFailed(errorCode: Int) {
                _state.value = State.Failed(scanErrorMessage(errorCode))
                scanCallback = null
            }
        }

        scanCallback = callback
        _state.value = State.Scanning

        scanner.startScan(
            filters,
            scanSettings(),
            callback,
        )
        return true
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        val callback = scanCallback ?: run {
            _state.value = State.Idle
            return
        }

        scanner.stopScan(callback)
        scanCallback = null
        _state.value = State.Idle
    }

    fun clearResults() {
        _devices.value = emptyList()
    }

    @SuppressLint("MissingPermission")
    private fun publishResult(result: ScanResult) {
        val device = result.device
        val address = device.address
        val name = result.scanRecord?.deviceName
            ?: device.name
            ?: "Unknown"

        val scanned = ScannedDevice(
            device = device,
            name = name,
            rssi = result.rssi,
            lastSeenMs = System.currentTimeMillis(),
        )

        _devices.update { current ->
            current
                .filterNot { it.device.address == address }
                .plus(scanned)
                .sortedByDescending { it.rssi }
        }
    }

    private fun scanSettings(): ScanSettings =
        ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
            .setReportDelay(0L)
            .setUseHardwareBatchingIfSupported(false)
            .setUseHardwareFilteringIfSupported(true)
            .build()

    private fun scanErrorMessage(errorCode: Int): String = when (errorCode) {
        ScanCallback.SCAN_FAILED_ALREADY_STARTED -> "Сканирование уже запущено"
        ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED ->
            "Не удалось зарегистрировать callback (лимит приложений или нет разрешений)"
        ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> "BLE scan не поддерживается устройством"
        ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> "Внутренняя ошибка Bluetooth-стека"
        ScanCallback.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES ->
            "Недостаточно аппаратных ресурсов для сканирования"
        ScanCallback.SCAN_FAILED_SCANNING_TOO_FREQUENTLY ->
            "Слишком частые запуски сканирования — подождите"
        else -> "Ошибка сканирования: $errorCode"
    }
}
