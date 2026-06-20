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
import kotlinx.coroutines.launch

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
            forceNewtons = telemetry.forceNewtons,
            massGrams = telemetry.massGrams,
            current = telemetry.currentAmps,
            voltage = telemetry.busVoltage,
            hx711Ok = telemetry.hx711Ok,
            ina226Ok = telemetry.ina226Ok,
            motorsArmed = telemetry.motorsArmed,
            motorPwmPercent = telemetry.motorPwmPercent,
            motorPwmRaw = telemetry.motorPwmRaw,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState(),
    )

    fun setMotorsArmed(armed: Boolean) {
        viewModelScope.launch {
            if (armed) {
                loadCellManager.armMotors()
            } else {
                loadCellManager.disarmMotors()
            }
        }
    }

    fun setMotorPwm(percent: Float) {
        loadCellManager.sendMotorPwm(percent)
    }
}
