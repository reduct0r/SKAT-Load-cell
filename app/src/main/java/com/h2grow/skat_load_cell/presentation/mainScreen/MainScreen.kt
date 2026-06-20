package com.h2grow.skat_load_cell.presentation.mainScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.h2grow.skat_load_cell.ui.theme.SKATLoadcellTheme

@Composable
fun MainScreen(
    onGoToScanner: () -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MainScreenContent(uiState = uiState, onGoToScanner = onGoToScanner)
}

@Composable
internal fun MainScreenContent(
    uiState: MainUiState,
    onGoToScanner: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = modifier
            .padding(0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (uiState.isConnected) "Подключено" else "Не подключено",
                style = MaterialTheme.typography.titleMedium,
                color = if (uiState.isConnected) {
                    Color.Green
                } else {
                    MaterialTheme.colorScheme.error
                },
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
                .padding(start = 16.dp, end = 16.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MetricRow(label = "Сила тяги", value = "${"%.2f".format(uiState.tractionForce)} Н")
                    MetricRow(label = "Ток", value = "${"%.3f".format(uiState.current)} А")
                    MetricRow(label = "Напряжение", value = "${"%.2f".format(uiState.voltage)} В")
                }

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MetricRow(label = "Экв. масса", value = "${"%.2f".format(uiState.tractionForce * 9.80665)} г")
                    MetricRow(label = "Мощность", value = "${"%.1f".format(uiState.voltage * uiState.current)} Вт")
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

        HorizontalDivider(
            thickness = 5.dp,
            color = Color.Gray
        )

        Button(onClick = onGoToScanner) {
            Text(if (uiState.isConnected) "Сменить устройство" else "Найти устройство")
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Column (
        modifier = Modifier
            .width(120.dp),
    ){
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewMainScreenDisconnected() {
    SKATLoadcellTheme(dynamicColor = false) {
        MainScreenContent(
            uiState = MainUiState(),
            onGoToScanner = {},
        )
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
                tractionForce = 1201.5f,
                current = 0.042f,
                voltage = 5.12f,
                hx711Ok = true,
                ina226Ok = true,
            ),
            onGoToScanner = {},
        )
    }
}
