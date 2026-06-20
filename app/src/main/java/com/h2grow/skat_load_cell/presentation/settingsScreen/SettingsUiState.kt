package com.h2grow.skat_load_cell.presentation.settingsScreen

data class SettingsUiState(
    val isConnected: Boolean = false,
    val hx711Ok: Boolean = false,
    val ina226Ok: Boolean = false,
    val ina226CalOk: Boolean = false,
    val ina226Addr: Int = 0,
    val scale: Float = 0f,
    val escPulseUs: Int = 1000,
    val escMinUs: Int = 1000,
    val escMaxUs: Int = 2000,
    val motorsArmed: Boolean = false,
    val busVoltage: Float = 0f,
    val shuntMv: Float = 0f,
    val shuntOhm: Float = 0.005f,
    val currentSign: Int = 1,
    val hx711Raw: Long = 0,
    val isBusy: Boolean = false,
    val statusMessage: String? = null,
    val isError: Boolean = false,
)
