package com.h2grow.skat_load_cell.data.ble

import java.util.UUID

/**
 * GATT UUID и имя устройства из прошивки ESP32 ([ble_module.cpp]).
 */
object SkatLoadCellSpec {
    const val DEVICE_NAME = "SKAT-Tenzo"

    /** Интервал notify телеметрии на ESP32 — 500 ms */
    const val TELEMETRY_INTERVAL_MS = 500

    val SERVICE: UUID = uuid("a1b2c3d4-e5f6-7890-abcd-ef1234567890")

    /** READ + NOTIFY, JSON телеметрии каждые 500 ms */
    val TELEMETRY: UUID = uuid("a1b2c3d4-e5f6-7890-abcd-ef1234567891")

    /** WRITE + WRITE_NR, JSON-команды */
    val COMMAND: UUID = uuid("a1b2c3d4-e5f6-7890-abcd-ef1234567892")

    /** READ + NOTIFY, JSON-ответ на команду */
    val RESPONSE: UUID = uuid("a1b2c3d4-e5f6-7890-abcd-ef1234567893")

    private fun uuid(value: String): UUID = UUID.fromString(value)
}
