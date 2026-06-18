package com.h2grow.skat_load_cell.presentation.scannerScreen

import com.h2grow.skat_load_cell.domain.model.ScannedDevice

data class ScannerUiState(
    val devices: List<ScannedDevice> = emptyList(),
    val isScanning: Boolean = false,
    val isConnecting: Boolean = false,
    val connectingAddress: String? = null,
    val permissionsGranted: Boolean = false,
    val error: String? = null,
)
