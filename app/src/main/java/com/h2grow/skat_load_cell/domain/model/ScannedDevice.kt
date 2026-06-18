package com.h2grow.skat_load_cell.domain.model

import android.bluetooth.BluetoothDevice

data class ScannedDevice(
    val device: BluetoothDevice,
    val name: String,
    val rssi: Int,
    val lastSeenMs: Long,
)