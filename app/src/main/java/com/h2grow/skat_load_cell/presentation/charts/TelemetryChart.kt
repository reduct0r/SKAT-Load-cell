package com.h2grow.skat_load_cell.presentation.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CHART_LEFT_PADDING = 44.dp
private val CHART_RIGHT_PADDING = 72.dp
private val CHART_TOP_PADDING = 8.dp
private val CHART_BOTTOM_PADDING = 28.dp

@Composable
fun TelemetryMultiChart(
    samples: List<ChartSample>,
    visibility: ChartVisibility,
    modifier: Modifier = Modifier,
    showAxes: Boolean = false,
    singleSeries: ChartSeries? = null,
) {
    val activeSeries = singleSeries?.let { listOf(it) } ?: visibility.visibleSeries()
    var crosshairTimestampMs by remember { mutableLongStateOf(-1L) }
    var hasCrosshair by remember { mutableStateOf(false) }
    val latestSamples by rememberUpdatedState(samples)
    val textMeasurer = rememberTextMeasurer()
    val axisTextColor = MaterialTheme.colorScheme.onSurface
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 10.sp,
        color = axisTextColor,
    )
    val axisStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 9.sp,
        color = axisTextColor,
    )
    val crosshairLineColor = MaterialTheme.colorScheme.outline
    val density = LocalDensity.current

    val crosshairFraction = when {
        samples.isEmpty() -> 1f
        !hasCrosshair -> 1f
        else -> fractionForTimestamp(samples, crosshairTimestampMs)
    }
    val crosshair = interpolateSample(samples, crosshairFraction)

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(density) {
                    fun updateCrosshairAt(x: Float) {
                        val current = latestSamples
                        if (current.isEmpty()) return
                        val chartLeft = with(density) { CHART_LEFT_PADDING.toPx() }
                        val chartRight = size.width - with(density) { CHART_RIGHT_PADDING.toPx() }
                        val width = chartRight - chartLeft
                        if (width <= 0f) return
                        val fraction = ((x - chartLeft) / width).coerceIn(0f, 1f)
                        interpolateSample(current, fraction)?.let { values ->
                            crosshairTimestampMs = values.timestampMs
                            hasCrosshair = true
                        }
                    }

                    detectDragGestures(
                        onDragStart = { offset -> updateCrosshairAt(offset.x) },
                        onDragEnd = {},
                        onDragCancel = {},
                    ) { change, _ ->
                        updateCrosshairAt(change.position.x)
                        change.consume()
                    }
                },
        ) {
            if (samples.isEmpty()) return@Canvas

            val chartLeft = CHART_LEFT_PADDING.toPx()
            val chartRight = size.width - CHART_RIGHT_PADDING.toPx()
            val chartTop = CHART_TOP_PADDING.toPx()
            val chartBottom = size.height - CHART_BOTTOM_PADDING.toPx()
            val chartWidth = chartRight - chartLeft
            val chartHeight = chartBottom - chartTop

            if (chartWidth <= 0f || chartHeight <= 0f) return@Canvas

            val gridColor = Color(0xFF334155)
            val axisColor = Color(0xFF64748B)

            for (i in 0..4) {
                val y = chartTop + chartHeight * i / 4f
                drawLine(
                    color = gridColor.copy(alpha = 0.35f),
                    start = Offset(chartLeft, y),
                    end = Offset(chartRight, y),
                    strokeWidth = 1f,
                )
            }

            drawLine(
                color = axisColor,
                start = Offset(chartLeft, chartBottom),
                end = Offset(chartRight, chartBottom),
                strokeWidth = 2f,
            )
            drawLine(
                color = axisColor,
                start = Offset(chartLeft, chartTop),
                end = Offset(chartLeft, chartBottom),
                strokeWidth = 2f,
            )

            activeSeries.forEach { series ->
                val range = rangeForSeries(samples, series)
                val path = Path()
                samples.forEachIndexed { index, sample ->
                    val x = chartLeft + chartWidth * index / (samples.size - 1).coerceAtLeast(1)
                    val normalized = range.normalize(series.valueOf(sample))
                    val y = chartBottom - normalized * chartHeight
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = series.color,
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )

                if (showAxes && singleSeries == series) {
                    for (i in 0..4) {
                        val value = range.max - range.span * i / 4f
                        val y = chartTop + chartHeight * i / 4f
                        val text = series.format(value)
                        val layout = textMeasurer.measure(text, axisStyle)
                        drawText(
                            textLayoutResult = layout,
                            topLeft = Offset(4f, y - layout.size.height / 2f),
                        )
                    }
                }
            }

            if (showAxes) {
                val startMs = samples.first().timestampMs
                val endMs = samples.last().timestampMs
                listOf(0f, 0.5f, 1f).forEach { fraction ->
                    val x = chartLeft + chartWidth * fraction
                    val timeMs = startMs + ((endMs - startMs) * fraction).toLong()
                    val text = formatTimeAxis(timeMs, startMs)
                    val layout = textMeasurer.measure(text, axisStyle)
                    drawText(
                        textLayoutResult = layout,
                        topLeft = Offset(x - layout.size.width / 2f, chartBottom + 6f),
                    )
                }
            }

            val crosshairX = chartLeft + chartWidth * crosshairFraction
            drawLine(
                color = crosshairLineColor,
                start = Offset(crosshairX, chartTop),
                end = Offset(crosshairX, chartBottom),
                strokeWidth = 1.5f,
            )

            crosshair?.let { values ->
                activeSeries.forEach { series ->
                    val value = values.values[series] ?: return@forEach
                    val dotY = chartBottom -
                        rangeForSeries(samples, series).normalize(value) * chartHeight
                    drawCircle(color = series.color, radius = 5f, center = Offset(crosshairX, dotY))
                }
            }
        }

        if (crosshair != null && activeSeries.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp),
            ) {
                Text(
                    text = formatTimeAxis(crosshair.timestampMs, samples.first().timestampMs),
                    style = labelStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                activeSeries.forEach { series ->
                    val value = crosshair.values[series] ?: return@forEach
                    Text(
                        text = series.format(value),
                        style = labelStyle,
                        color = series.color,
                    )
                }
            }
        }

        if (samples.isEmpty()) {
            Text(
                text = "Нет данных",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
