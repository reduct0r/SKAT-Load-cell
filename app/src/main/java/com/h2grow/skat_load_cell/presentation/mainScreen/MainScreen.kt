package com.h2grow.skat_load_cell.presentation.mainScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.h2grow.skat_load_cell.ui.theme.SKATLoadcellTheme
import kotlinx.coroutines.delay

@Composable
fun MainScreen(
    onGoToScanner: () -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MainScreenContent(
        uiState = uiState,
        onGoToScanner = onGoToScanner,
        onArmToggle = { armed -> viewModel.setMotorsArmed(armed) },
        onMotorPwmChange = viewModel::setMotorPwm,
    )
}

@Composable
internal fun MainScreenContent(
    uiState: MainUiState,
    onGoToScanner: () -> Unit,
    onArmToggle: (Boolean) -> Unit = {},
    onMotorPwmChange: (Float) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var localPwm by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(uiState.motorPwmPercent, uiState.motorsArmed) {
        localPwm = if (uiState.motorsArmed) uiState.motorPwmPercent else 0f
    }

    LaunchedEffect(localPwm, uiState.motorsArmed, uiState.isConnected) {
        if (!uiState.isConnected || !uiState.motorsArmed) return@LaunchedEffect
        delay(120)
        onMotorPwmChange(localPwm)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (uiState.isConnected) "Подключено" else "Не подключено",
                style = MaterialTheme.typography.titleMedium,
                color = if (uiState.isConnected) Color(0xFF22C55E) else MaterialTheme.colorScheme.error,
            )
            uiState.deviceName?.let { name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MetricRow(
                        label = "Сила тяги",
                        value = "${"%.2f".format(uiState.forceNewtons)} Н",
                        primary = true,
                    )
                    MetricRow(label = "Ток", value = "${"%.3f".format(uiState.current)} А")
                    MetricRow(label = "Напряжение", value = "${"%.2f".format(uiState.voltage)} В")
                }
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MetricRow(
                        label = "Экв. масса",
                        value = "${"%.1f".format(uiState.massGrams)} г",
                        primary = false,
                    )
                    MetricRow(
                        label = "Мощность",
                        value = "${"%.1f".format(uiState.voltage * uiState.current)} Вт",
                        primary = false,
                    )
                }
            }
        }

        if (uiState.isConnected) {
            Text(
                text = buildString {
                    append("HX711: ")
                    append(if (uiState.hx711Ok) "OK" else "нет")
                    append("  |  INA226: ")
                    append(if (uiState.ina226Ok) "OK" else "нет")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        HorizontalDivider(thickness = 2.dp, color = Color(0xFF334155))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MotorPwmReadout(
                percent = uiState.motorPwmPercent,
                pwmRaw = uiState.motorPwmRaw,
                enabled = uiState.isConnected && uiState.motorsArmed,
            )

            MotorThrottleSlider(
                value = localPwm,
                onValueChange = { localPwm = it },
                enabled = uiState.isConnected && uiState.motorsArmed,
            )

            Button(
                modifier = Modifier.width(220.dp),
                onClick = {
                    if (uiState.isConnected) {
                        onArmToggle(!uiState.motorsArmed)
                    }
                },
                enabled = uiState.isConnected,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.motorsArmed) Color(0xFFDC2626) else Color(0xFFDC2626),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF475569),
                ),
            ) {
                Text(if (uiState.motorsArmed) "Disarm" else "Arm")
            }

            if (uiState.isConnected && !uiState.motorsArmed) {
                Text(
                    text = "Выполните Arm для изменения PWM",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        HorizontalDivider(thickness = 2.dp, color = Color(0xFF334155))

        Button(onClick = onGoToScanner) {
            Text(if (uiState.isConnected) "Сменить устройство" else "Найти устройство")
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    primary: Boolean = true,
) {
    Column(modifier = Modifier.width(130.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = if (primary) {
                MaterialTheme.typography.headlineSmall
            } else {
                MaterialTheme.typography.titleLarge
            },
            fontWeight = FontWeight.SemiBold,
            color = if (primary) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewMainScreenDisconnected() {
    SKATLoadcellTheme(dynamicColor = false) {
        MainScreenContent(uiState = MainUiState(), onGoToScanner = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewMainScreenConnected() {
    SKATLoadcellTheme(dynamicColor = false) {
        MainScreenContent(
            uiState = MainUiState(
                isConnected = true,
                deviceName = "SKAT-Tenzo",
                forceNewtons = 11.78f,
                massGrams = 1201.5f,
                current = 0.042f,
                voltage = 5.12f,
                hx711Ok = true,
                ina226Ok = true,
            ),
            onGoToScanner = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewMainScreenArmed() {
    SKATLoadcellTheme(dynamicColor = false) {
        MainScreenContent(
            uiState = MainUiState(
                isConnected = true,
                deviceName = "SKAT-Tenzo",
                forceNewtons = 24.5f,
                massGrams = 2498f,
                current = 1.2f,
                voltage = 11.8f,
                hx711Ok = true,
                ina226Ok = true,
                motorsArmed = true,
                motorPwmPercent = 42f,
                motorPwmRaw = 419,
            ),
            onGoToScanner = {},
        )
    }
}
