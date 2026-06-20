package com.h2grow.skat_load_cell.presentation.mainScreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
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
import com.h2grow.skat_load_cell.presentation.charts.ChartPanelCard
import com.h2grow.skat_load_cell.presentation.charts.ChartSample
import com.h2grow.skat_load_cell.presentation.charts.ChartSeries
import com.h2grow.skat_load_cell.presentation.charts.ChartVisibility
import com.h2grow.skat_load_cell.presentation.charts.ChartsViewModel
import com.h2grow.skat_load_cell.ui.theme.SKATLoadcellTheme

@Composable
fun MainScreen(
    onGoToScanner: () -> Unit,
    onOpenChartsDetail: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
    chartsViewModel: ChartsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val chartSamples by chartsViewModel.samples.collectAsStateWithLifecycle()
    val chartVisibility by chartsViewModel.visibility.collectAsStateWithLifecycle()
    MainScreenContent(
        uiState = uiState,
        chartSamples = chartSamples,
        chartVisibility = chartVisibility,
        onGoToScanner = onGoToScanner,
        onOpenChartsDetail = onOpenChartsDetail,
        onOpenSettings = onOpenSettings,
        onToggleChartSeries = chartsViewModel::toggleSeries,
        onResetCharts = chartsViewModel::reset,
        onArmToggle = { armed -> viewModel.setMotorsArmed(armed) },
        onMotorPwmChange = viewModel::setMotorPwm,
    )
}

@Composable
internal fun MainScreenContent(
    uiState: MainUiState,
    chartSamples: List<ChartSample> = emptyList(),
    chartVisibility: ChartVisibility = ChartVisibility(),
    onGoToScanner: () -> Unit,
    onOpenChartsDetail: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onToggleChartSeries: (ChartSeries, Boolean) -> Unit = { _, _ -> },
    onResetCharts: () -> Unit = {},
    onArmToggle: (Boolean) -> Unit = {},
    onMotorPwmChange: (Float) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var localPwm by remember { mutableFloatStateOf(0f) }
    var isDraggingPwm by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.motorPwmPercent, uiState.motorsArmed) {
        if (!isDraggingPwm) {
            localPwm = if (uiState.motorsArmed) uiState.motorPwmPercent else 0f
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
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
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.align(Alignment.CenterEnd),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Калибровка и настройки",
                    )
                }
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
                percent = localPwm,
                escPulseUs = uiState.escPulseUs,
                enabled = uiState.isConnected && uiState.motorsArmed,
            )

            MotorThrottleSlider(
                value = localPwm,
                onValueChange = { value ->
                    localPwm = value
                    if (uiState.isConnected && uiState.motorsArmed) {
                        onMotorPwmChange(value)
                    }
                },
                onDraggingChange = { isDraggingPwm = it },
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

        ChartPanelCard(
            samples = chartSamples,
            visibility = chartVisibility,
            onToggleSeries = onToggleChartSeries,
            onReset = onResetCharts,
            onOpenDetails = onOpenChartsDetail,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

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
    SKATLoadcellTheme {
        MainScreenContent(uiState = MainUiState(), onGoToScanner = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewMainScreenConnected() {
    SKATLoadcellTheme {
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
    SKATLoadcellTheme {
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
                escPulseUs = 1420,
            ),
            onGoToScanner = {},
        )
    }
}
