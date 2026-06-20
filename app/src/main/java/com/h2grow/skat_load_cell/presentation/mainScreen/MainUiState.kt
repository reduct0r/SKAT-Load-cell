package com.h2grow.skat_load_cell.presentation.mainScreen

data class MainUiState(
    val isConnected: Boolean = false,
    val deviceName: String? = null,
    val tractionForce: Float = 0f, // ньютоны
    val current: Float = 0f,
    val voltage: Float = 0f,
    val hx711Ok: Boolean = false,
    val ina226Ok: Boolean = false,
)
