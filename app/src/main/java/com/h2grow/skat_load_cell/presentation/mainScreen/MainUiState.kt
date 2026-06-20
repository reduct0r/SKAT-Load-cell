package com.h2grow.skat_load_cell.presentation.mainScreen

data class MainUiState(
    val isConnected: Boolean = false,
    val deviceName: String? = null,
    /** Сила тяги, Н — основное значение */
    val forceNewtons: Float = 0f,
    /** Эквивалентная масса с датчика, г */
    val massGrams: Float = 0f,
    val current: Float = 0f,
    val voltage: Float = 0f,
    val hx711Ok: Boolean = false,
    val ina226Ok: Boolean = false,
    val motorsArmed: Boolean = false,
    val motorPwmPercent: Float = 0f,
    val motorPwmRaw: Int = 0,
    val escPulseUs: Int = 1000,
)
