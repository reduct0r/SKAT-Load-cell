package com.h2grow.skat_load_cell.data.ble

import java.util.UUID

/**
 * GATT UUID и имя устройства из прошивки ESP32 ([ble_module.cpp]).
 * [LEGACY_*] — старые placeholder-UUID на части устройств.
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

    val LEGACY_SERVICE: UUID = uuid("12345678-1234-1234-1234-123456789abc")
    val LEGACY_TELEMETRY: UUID = uuid("12345678-1234-1234-1234-123456789abd")
    val LEGACY_COMMAND: UUID = uuid("12345678-1234-1234-1234-123456789abe")
    val LEGACY_RESPONSE: UUID = uuid("12345678-1234-1234-1234-123456789abf")

    /** Стандартные GATT-сервисы Android/BLE — не SKAT */
    val STANDARD_BLE_SERVICES: Set<UUID> = setOf(
        uuid("00001800-0000-1000-8000-00805f9b34fb"), // Generic Access
        uuid("00001801-0000-1000-8000-00805f9b34fb"), // Generic Attribute
        uuid("0000180a-0000-1000-8000-00805f9b34fb"), // Device Information
        uuid("0000180f-0000-1000-8000-00805f9b34fb"), // Battery
    )

    private fun uuid(value: String): UUID = UUID.fromString(value)
}
