package com.h2grow.skat_load_cell.presentation.scannerScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.h2grow.skat_load_cell.data.ble.BleScanner
import com.h2grow.skat_load_cell.data.ble.SkatLoadCellManager
import com.h2grow.skat_load_cell.domain.model.ScannedDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val bleScanner: BleScanner,
    private val loadCellManager: SkatLoadCellManager,
) : ViewModel() {

    private val _permissionsGranted = MutableStateFlow(false)
    private val _isConnecting = MutableStateFlow(false)
    private val _connectingAddress = MutableStateFlow<String?>(null)
    private val _localError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ScannerUiState> = combine(
        combine(
            bleScanner.devices,
            bleScanner.state,
            _permissionsGranted,
            _isConnecting,
        ) { devices, scanState, permissions, connecting ->
            PartialScannerState(
                devices = devices,
                scanState = scanState,
                permissionsGranted = permissions,
                isConnecting = connecting,
            )
        },
        _connectingAddress,
        _localError,
    ) { partial, connectingAddress, localError ->
        val scanError = (partial.scanState as? BleScanner.State.Failed)?.reason
        ScannerUiState(
            devices = partial.devices,
            isScanning = partial.scanState is BleScanner.State.Scanning,
            isConnecting = partial.isConnecting,
            connectingAddress = connectingAddress,
            permissionsGranted = partial.permissionsGranted,
            error = localError ?: scanError,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ScannerUiState(),
    )

    private val _connected = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val connected: SharedFlow<Unit> = _connected.asSharedFlow()

    fun onPermissionsResult(granted: Boolean) {
        _permissionsGranted.value = granted
        if (granted) {
            startScan()
        } else {
            _localError.value = "Нужны разрешения Bluetooth для сканирования"
        }
    }

    fun startScan() {
        if (!_permissionsGranted.value) return
        _localError.value = null
        if (!bleScanner.isReady()) {
            _localError.value = "Включите Bluetooth"
            return
        }
        bleScanner.clearResults()
        if (!bleScanner.startScan()) {
            _localError.value = "Не удалось запустить сканирование"
        }
    }

    fun stopScan() {
        bleScanner.stopScan()
    }

    fun connectToDevice(device: ScannedDevice) {
        if (_isConnecting.value) return

        viewModelScope.launch {
            _isConnecting.value = true
            _connectingAddress.value = device.device.address
            _localError.value = null
            bleScanner.stopScan()

            try {
                loadCellManager.connectToDeviceAndWait(device.device)
                _connected.emit(Unit)
            } catch (e: Exception) {
                _localError.value = e.message ?: "Ошибка подключения"
                startScan()
            } finally {
                _isConnecting.value = false
                _connectingAddress.value = null
            }
        }
    }

    override fun onCleared() {
        bleScanner.stopScan()
        super.onCleared()
    }

    private data class PartialScannerState(
        val devices: List<ScannedDevice>,
        val scanState: BleScanner.State,
        val permissionsGranted: Boolean,
        val isConnecting: Boolean,
    )
}
