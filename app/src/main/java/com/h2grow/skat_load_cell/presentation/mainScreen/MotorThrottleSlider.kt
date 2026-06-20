package com.h2grow.skat_load_cell.presentation.mainScreen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MotorThrottleSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val trackHeight = 14.dp
    val thumbRadius = 22.dp
    val activeColor = Color(0xFF22C55E)
    val idleColor = Color(0xFF334155)
    val fillBrush = Brush.horizontalGradient(
        colors = listOf(Color(0xFF166534), activeColor, Color(0xFF86EFAC)),
    )
    val disabledFill = Brush.horizontalGradient(
        colors = listOf(Color(0xFF1E293B), Color(0xFF475569)),
    )

    var dragValue by remember { mutableFloatStateOf(value) }
    val displayValue = if (enabled) dragValue.coerceIn(0f, 100f) else 0f

    androidx.compose.runtime.LaunchedEffect(value, enabled) {
        if (!enabled) {
            dragValue = 0f
        } else {
            dragValue = value.coerceIn(0f, 100f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f)
            .padding(horizontal = 8.dp, vertical = 12.dp),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(thumbRadius * 2)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            dragValue = fraction * 100f
                            onValueChange(dragValue)
                        },
                        onDragEnd = {},
                        onDragCancel = {},
                        onHorizontalDrag = { _, dragAmount ->
                            val fractionDelta = dragAmount / size.width * 100f
                            dragValue = (dragValue + fractionDelta).coerceIn(0f, 100f)
                            onValueChange(dragValue)
                        },
                    )
                },
        ) {
            val trackY = center.y
            val trackTop = trackY - trackHeight.toPx() / 2f
            val radius = trackHeight.toPx() / 2f
            val thumbR = thumbRadius.toPx()
            val fraction = displayValue / 100f
            val fillWidth = (size.width - thumbR * 2f) * fraction + thumbR

            drawRoundRect(
                color = idleColor,
                topLeft = Offset(thumbR, trackTop),
                size = Size(size.width - thumbR * 2f, trackHeight.toPx()),
                cornerRadius = CornerRadius(radius, radius),
            )

            if (fillWidth > thumbR) {
                drawRoundRect(
                    brush = if (enabled) fillBrush else disabledFill,
                    topLeft = Offset(thumbR, trackTop),
                    size = Size(fillWidth - thumbR, trackHeight.toPx()),
                    cornerRadius = CornerRadius(radius, radius),
                )
            }

            val thumbCenter = Offset(
                x = thumbR + (size.width - thumbR * 2f) * fraction,
                y = trackY,
            )

            drawCircle(
                color = Color.Black.copy(alpha = 0.25f),
                radius = thumbR + 4f,
                center = thumbCenter + Offset(0f, 3f),
            )
            drawCircle(
                color = if (enabled) Color.White else Color(0xFF94A3B8),
                radius = thumbR,
                center = thumbCenter,
            )
            drawCircle(
                color = if (enabled) activeColor else Color(0xFF64748B),
                radius = thumbR * 0.55f,
                center = thumbCenter,
            )
        }

        Text(
            text = if (enabled) "THROTTLE" else "ЗАБЛОКИРОВАНО",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (enabled) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.TopCenter),
        )


    }
}

@Composable
fun MotorPwmReadout(
    percent: Float,
    pwmRaw: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val activeColor = Color(0xFF22C55E)
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val displayPercent = if (enabled) percent else 0f
    val displayPwm = if (enabled) pwmRaw else 0

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Мощность",
                style = MaterialTheme.typography.labelMedium,
                color = mutedColor,
            )
            Text(
                text = "%.1f %%".format(displayPercent),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = if (enabled) activeColor else mutedColor,
            )
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(48.dp)
                .background(Color(0xFF334155)),
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "PWM (ESP)",
                style = MaterialTheme.typography.labelMedium,
                color = mutedColor,
            )
            Text(
                text = displayPwm.toString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else mutedColor,
            )
            Text(
                text = "response",
                style = MaterialTheme.typography.labelSmall,
                color = mutedColor,
            )
        }
    }
}
