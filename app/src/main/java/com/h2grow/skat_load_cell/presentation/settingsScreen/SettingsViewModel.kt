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

    fun setForceInverted(inverted: Boolean) {
        val sign = if (inverted) -1 else 1
        runCommand("Смена знака силы…") { loadCellManager.setForceSign(sign) }
    }

    fun applyShunt(extOhm: Float) {
        runCommand("Сохранение шунта…") { loadCellManager.setShunt(extOhm) }
    }

    fun calibrateBusVoltage(refVolts: Float) {
        if (refVolts < 1f) {
            showMessage("Введите эталонное напряжение ≥ 1 В", isError = true)
            return
        }
        runCommand("Калибровка напряжения…") { loadCellManager.calibrateBusVoltage(refVolts) }
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
                        message = detail ?: (result.cmd?.let { "OK: $it" } ?: "Готово"),
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

    private fun formatCommandFeedback(rawJson: String): String? = try {
        val obj = JSONObject(rawJson)
        when (obj.optString("cmd")) {
            "calibrate" -> {
                val scale = obj.optDouble("scale", 0.0)
                val ref = obj.optDouble("grams", 0.0)
                val measured = obj.optDouble("force_g", 0.0)
                "Калибровка OK: scale=%.1f, эталон=%.0f г, сейчас=%.1f г".format(
                    scale, ref, measured,
                )
            }
            "tare" -> "Обнулено (offset сохранён на ESP32)"
            "calibrate_bus_v" -> {
                val scale = obj.optInt("bus_v_scale_e4", 0)
                val raw = if (obj.has("raw_v")) {
                    obj.optDouble("raw_v", 0.0)
                } else {
                    obj.optDouble("measured_v", 0.0)
                }
                val ref = obj.optDouble("ref_v", 0.0)
                "Напряжение OK: scale=%d (сырое %.2f В → эталон %.2f В)".format(
                    scale, raw, ref,
                )
            }
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
