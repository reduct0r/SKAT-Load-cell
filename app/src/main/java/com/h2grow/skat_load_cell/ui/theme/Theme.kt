package com.h2grow.skat_load_cell.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppLightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color.White,
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F5F9),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF64748B),
)

@Composable
fun SKATLoadcellTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = AppLightColorScheme,
        typography = Typography,
        content = content,
    )
}
