package com.h2grow.skat_load_cell.domain.model

data class Telemetry(
    val forceGrams: Float = 0f,
    val currentAmps: Float = 0f,
    val busVoltage: Float = 0f,
    val hx711Ok: Boolean = false,
    val ina226Ok: Boolean = false,
    val ina226CalOk: Boolean = false,
    val ina226Addr: Int = 0,
    val i2cScan: String = "",
    val scale: Float = 0f,
) {
    val weightGrams: Float get() = forceGrams
}