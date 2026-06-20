package com.h2grow.skat_load_cell.presentation.mainScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.h2grow.skat_load_cell.data.ble.SkatLoadCellManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MainViewModel @Inject constructor(
    private val loadCellManager: SkatLoadCellManager,
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = combine(
        loadCellManager.telemetry,
        loadCellManager.isConnected,
    ) { telemetry, isConnected ->
        MainUiState(
            isConnected = isConnected,
            deviceName = if (isConnected) loadCellManager.connectedDeviceName else null,
            tractionForce = telemetry.forceGrams,
            current = telemetry.currentAmps,
            voltage = telemetry.busVoltage,
            hx711Ok = telemetry.hx711Ok,
            ina226Ok = telemetry.ina226Ok,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState(),
    )
}
