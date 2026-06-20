package com.h2grow.skat_load_cell.presentation.charts

import kotlin.math.max
import kotlin.math.min

internal data class SeriesRange(val min: Float, val max: Float) {
    val span: Float get() = max(max - min, 1e-6f)

    fun normalize(value: Float): Float = ((value - min) / span).coerceIn(0f, 1f)
}

internal fun rangeForSeries(samples: List<ChartSample>, series: ChartSeries): SeriesRange {
    if (samples.isEmpty()) return SeriesRange(0f, 1f)
    var minV = Float.MAX_VALUE
    var maxV = -Float.MAX_VALUE
    samples.forEach { sample ->
        val v = series.valueOf(sample)
        minV = min(minV, v)
        maxV = max(maxV, v)
    }
    if (minV == maxV) {
        minV -= 1f
        maxV += 1f
    }
    val padding = (maxV - minV) * 0.08f
    return SeriesRange(minV - padding, maxV + padding)
}

internal fun interpolateSample(samples: List<ChartSample>, fraction: Float): CrosshairValues? {
    if (samples.isEmpty()) return null
    if (samples.size == 1) {
        val s = samples.first()
        return CrosshairValues(
            timestampMs = s.timestampMs,
            values = ChartSeries.entries.associateWith { it.valueOf(s) },
        )
    }
    val clamped = fraction.coerceIn(0f, 1f)
    val position = clamped * (samples.size - 1)
    val index = position.toInt().coerceIn(0, samples.lastIndex)
    val nextIndex = min(index + 1, samples.lastIndex)
    val t = position - index
    val a = samples[index]
    val b = samples[nextIndex]
    val values = ChartSeries.entries.associateWith { series ->
        val va = series.valueOf(a)
        val vb = series.valueOf(b)
        va + (vb - va) * t
    }
    val time = a.timestampMs + ((b.timestampMs - a.timestampMs) * t).toLong()
    return CrosshairValues(timestampMs = time, values = values)
}

internal fun fractionForTimestamp(samples: List<ChartSample>, timestampMs: Long): Float {
    if (samples.isEmpty()) return 1f
    if (samples.size == 1) return 0f
    val first = samples.first().timestampMs
    val last = samples.last().timestampMs
    if (last <= first) return 1f
    return ((timestampMs - first).toFloat() / (last - first)).coerceIn(0f, 1f)
}

internal fun formatTimeAxis(timestampMs: Long, startMs: Long): String {
    val seconds = ((timestampMs - startMs) / 1000f).coerceAtLeast(0f)
    return if (seconds >= 60f) {
        "%d:%02d".format((seconds / 60).toInt(), (seconds % 60).toInt())
    } else {
        "%.0f с".format(seconds)
    }
}
