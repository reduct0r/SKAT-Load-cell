package com.h2grow.skat_load_cell.presentation.settingsScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.h2grow.skat_load_cell.data.ble.SkatLoadCellManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.json.JSONObject
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
            forceGrams = telemetry.forceGrams,
            scale = telemetry.scale,
            escPulseUs = telemetry.escPulseUs,
            escMinUs = telemetry.escMinUs,
            escMaxUs = telemetry.escMaxUs,
            motorsArmed = telemetry.motorsArmed,
            busVoltage = telemetry.busVoltage,
            shuntMv = telemetry.shuntMv,
            shuntOhm = telemetry.shuntOhm,
            shuntExtOhm = telemetry.shuntExtOhm,
            currentSign = telemetry.currentSign,
            forceSign = telemetry.forceSign,
            busVScaleE4 = telemetry.busVScaleE4,
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

    fun tare() = runCommand("Выполняется обнуление…") { loadCellManager.tare() }

    fun calibrate(grams: Float) {
        if (grams <= 0f) {
            showMessage("Укажите эталонную массу больше 0 г", isError = true)
            return
        }
        runCommand("Выполняется калибровка…") { loadCellManager.calibrate(grams) }
    }

    fun resetScale() = runCommand("Выполняется сброс…") { loadCellManager.reset() }

    fun recalibrateIna226() = runCommand("Выполняется перекалибровка…") { loadCellManager.recalibrateIna226() }

    fun zeroCurrent() = runCommand("Выполняется обнуление тока…") { loadCellManager.zeroCurrent() }

    fun setCurrentInverted(inverted: Boolean) {
        val sign = if (inverted) -1 else 1
        runCommand("Применяется настройка…") { loadCellManager.setCurrentSign(sign) }
    }

    fun setForceInverted(inverted: Boolean) {
        val sign = if (inverted) -1 else 1
        runCommand("Применяется настройка…") { loadCellManager.setForceSign(sign) }
    }

    fun applyShunt(extOhm: Float) {
        runCommand("Сохранение параметров шунта…") { loadCellManager.setShunt(extOhm) }
    }

    fun calibrateBusVoltage(refVolts: Float) {
        if (refVolts < 1f) {
            showMessage("Укажите эталонное напряжение не менее 1 В", isError = true)
            return
        }
        runCommand("Выполняется калибровка напряжения…") { loadCellManager.calibrateBusVoltage(refVolts) }
    }

    private fun runCommand(status: String, block: suspend () -> com.h2grow.skat_load_cell.domain.model.CommandResult) {
        viewModelScope.launch {
            feedback.value = Feedback(busy = true, message = status, isError = false)
            try {
                val result = block()
                if (result.ok) {
                    val detail = formatCommandFeedback(result.rawJson)
                    feedback.value = Feedback(
                        busy = false,
                        message = detail ?: "Операция выполнена.",
                        isError = false,
                    )
                } else {
                    feedback.value = Feedback(
                        busy = false,
                        message = result.error ?: "Операция не выполнена",
                        isError = true,
                    )
                }
            } catch (e: Exception) {
                feedback.value = Feedback(
                    busy = false,
                    message = e.message ?: "Операция не выполнена",
                    isError = true,
                )
            }
        }
    }

    private fun showMessage(message: String, isError: Boolean) {
        feedback.value = Feedback(busy = false, message = message, isError = isError)
    }

    private fun formatCommandFeedback(rawJson: String): String? = try {
        val obj = JSONObject(rawJson)
        when (obj.optString("cmd")) {
            "calibrate" -> {
                val ref = obj.optDouble("grams", 0.0)
                val measured = obj.optDouble("force_g", 0.0)
                "Калибровка массы выполнена. Эталон: ${ref.toInt()} г, показание: ${
                    "%.1f".format(measured)
                } г."
            }
            "tare" -> "Нулевая точка массы установлена."
            "reset" -> "Шкала массы восстановлена по умолчанию."
            "calibrate_bus_v" -> {
                val ref = obj.optDouble("ref_v", 0.0)
                "Калибровка напряжения выполнена. Эталон: ${"%.2f".format(ref)} В."
            }
            "set_shunt" -> "Параметры шунта сохранены."
            "zero_current" -> "Нулевая точка тока установлена."
            "recal_ina226" -> "Перекалибровка датчика тока выполнена."
            "set_current_sign" -> "Направление отсчёта тока изменено."
            "set_force_sign" -> "Направление отсчёта силы изменено."
            else -> null
        }
    } catch (_: Exception) {
        null
    }

    private data class Feedback(
        val busy: Boolean,
        val message: String?,
        val isError: Boolean,
    )
}
