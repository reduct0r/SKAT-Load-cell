package com.h2grow.skat_load_cell.presentation.mainScreen

data class MainUiState(
    val isConnected: Boolean = false,
    val weight: Float = 0f,
    val current: Float = 0f,
    val voltage: Float = 0f
)