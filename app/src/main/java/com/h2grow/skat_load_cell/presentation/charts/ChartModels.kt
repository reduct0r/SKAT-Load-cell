package com.h2grow.skat_load_cell.presentation.charts

import androidx.compose.ui.graphics.Color

data class ChartSample(
    val timestampMs: Long,
    val forceNewtons: Float,
    val currentAmps: Float,
    val voltage: Float,
)

enum class ChartSeries(
    val label: String,
    val unit: String,
    val color: Color,
) {
    FORCE("Сила тяги", "Н", Color(0xFF3B82F6)),
    CURRENT("Ток", "А", Color(0xFFF59E0B)),
    VOLTAGE("Напряжение", "В", Color(0xFF22C55E)),
    ;

    fun valueOf(sample: ChartSample): Float = when (this) {
        FORCE -> sample.forceNewtons
        CURRENT -> sample.currentAmps
        VOLTAGE -> sample.voltage
    }

    fun format(value: Float): String = when (this) {
        FORCE -> "%.2f %s".format(value, unit)
        CURRENT -> "%.3f %s".format(value, unit)
        VOLTAGE -> "%.2f %s".format(value, unit)
    }
}

data class ChartVisibility(
    val force: Boolean = true,
    val current: Boolean = true,
    val voltage: Boolean = true,
) {
    fun isVisible(series: ChartSeries): Boolean = when (series) {
        ChartSeries.FORCE -> force
        ChartSeries.CURRENT -> current
        ChartSeries.VOLTAGE -> voltage
    }

    fun withToggle(series: ChartSeries, visible: Boolean): ChartVisibility = when (series) {
        ChartSeries.FORCE -> copy(force = visible)
        ChartSeries.CURRENT -> copy(current = visible)
        ChartSeries.VOLTAGE -> copy(voltage = visible)
    }

    fun visibleSeries(): List<ChartSeries> = ChartSeries.entries.filter(::isVisible)
}

data class CrosshairValues(
    val timestampMs: Long,
    val values: Map<ChartSeries, Float>,
)
