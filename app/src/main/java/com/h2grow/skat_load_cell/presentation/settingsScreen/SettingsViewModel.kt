package com.h2grow.skat_load_cell.presentation.settingsScreen

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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val loadCellManager: SkatLoadCellManager,
) : ViewModel() {

    private val feedback = MutableStateFlow<Feedback?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        loadCellManager.telemetry,
        loadCellManager.isConnected,
        feedback,
    ) { telemetry, isConnected, fb ->
        SettingsUiState(
            isConnected = isConnected,
            hx711Ok = telemetry.hx711Ok,
            ina226Ok = telemetry.ina226Ok,
            ina226CalOk = telemetry.ina226CalOk,
            ina226Addr = telemetry.ina226Addr,
            scale = telemetry.scale,
            escPulseUs = telemetry.escPulseUs,
            escMinUs = telemetry.escMinUs,
            escMaxUs = telemetry.escMaxUs,
            motorsArmed = telemetry.motorsArmed,
            busVoltage = telemetry.busVoltage,
            shuntMv = telemetry.shuntMv,
            shuntOhm = telemetry.shuntOhm,
            currentSign = telemetry.currentSign,
            hx711Raw = telemetry.hx711Raw,
            isBusy = fb?.busy == true,
            statusMessage = fb?.message,
            isError = fb?.isError == true,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun tare() = runCommand("Обнуление…") { loadCellManager.tare() }

    fun calibrate(grams: Float) {
        if (grams <= 0f) {
            showMessage("Введите массу больше 0 г", isError = true)
            return
        }
        runCommand("Калибровка…") { loadCellManager.calibrate(grams) }
    }

    fun resetScale() = runCommand("Сброс шкалы…") { loadCellManager.reset() }

    fun recalibrateIna226() = runCommand("Калибровка INA226…") { loadCellManager.recalibrateIna226() }

    fun zeroCurrent() = runCommand("Обнуление тока…") { loadCellManager.zeroCurrent() }

    fun setCurrentInverted(inverted: Boolean) {
        val sign = if (inverted) -1 else 1
        runCommand("Смена знака тока…") { loadCellManager.setCurrentSign(sign) }
    }

    fun applyShunt(extOhm: Float, brdOhm: Float, includeBoard: Boolean) {
        runCommand("Применение шунта…") {
            loadCellManager.setShunt(extOhm, brdOhm, includeBoard)
        }
    }

    private fun runCommand(status: String, block: suspend () -> com.h2grow.skat_load_cell.domain.model.CommandResult) {
        viewModelScope.launch {
            feedback.value = Feedback(busy = true, message = status, isError = false)
            try {
                val result = block()
                if (result.ok) {
                    feedback.value = Feedback(
                        busy = false,
                        message = result.cmd?.let { "OK: $it" } ?: "Готово",
                        isError = false,
                    )
                } else {
                    feedback.value = Feedback(
                        busy = false,
                        message = result.error ?: "Ошибка",
                        isError = true,
                    )
                }
            } catch (e: Exception) {
                feedback.value = Feedback(
                    busy = false,
                    message = e.message ?: "Ошибка",
                    isError = true,
                )
            }
        }
    }

    private fun showMessage(message: String, isError: Boolean) {
        feedback.value = Feedback(busy = false, message = message, isError = isError)
    }

    private data class Feedback(
        val busy: Boolean,
        val message: String?,
        val isError: Boolean,
    )
}
