package com.h2grow.skat_load_cell.presentation.mainScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.h2grow.skat_load_cell.data.ble.SkatLoadCellManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val loadCellManager: SkatLoadCellManager,
) : ViewModel() {

    private val throttlePercent = MutableStateFlow(0f)

    init {
        viewModelScope.launch {
            loadCellManager.isConnected.collect { connected ->
                if (!connected) {
                    throttlePercent.value = 0f
                }
            }
        }
    }

    val uiState: StateFlow<MainUiState> = combine(
        loadCellManager.telemetry,
        loadCellManager.isConnected,
        throttlePercent,
    ) { telemetry, isConnected, throttle ->
        val armed = isConnected && telemetry.motorsArmed
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
            throttlePercent = if (armed) throttle else 0f,
            motorPwmPercent = telemetry.motorPwmPercent,
            motorPwmRaw = telemetry.escPulseUs,
            escPulseUs = telemetry.escPulseUs,
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
                throttlePercent.value = 0f
                loadCellManager.disarmMotors()
            }
        }
    }

    fun setMotorPwm(percent: Float) {
        val clamped = percent.coerceIn(0f, 100f)
        throttlePercent.value = clamped
        loadCellManager.sendMotorPwm(clamped)
    }

    fun flushMotorPwm() {
        loadCellManager.flushPendingMotorPwm()
    }
}
