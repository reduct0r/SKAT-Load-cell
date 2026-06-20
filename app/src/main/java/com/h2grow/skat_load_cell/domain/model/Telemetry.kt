package com.h2grow.skat_load_cell.domain.model

data class Telemetry(
    /** Масса/усилие с HX711, г (эталонная единица датчика) */
    val forceGrams: Float = 0f,
    /** Сила тяги, Н (с ESP или force_g × g) */
    val forceNewtons: Float = 0f,
    val currentAmps: Float = 0f,
    val busVoltage: Float = 0f,
    val hx711Ok: Boolean = false,
    val ina226Ok: Boolean = false,
    val ina226CalOk: Boolean = false,
    val ina226Addr: Int = 0,
    val i2cScan: String = "",
    val scale: Float = 0f,
    val motorsArmed: Boolean = false,
    val motorPwmPercent: Float = 0f,
    /** Сырой PWM с ESP (0…1023) */
    val motorPwmRaw: Int = 0,
) {
    val massGrams: Float get() = forceGrams
    val powerWatts: Float get() = busVoltage * currentAmps
}
