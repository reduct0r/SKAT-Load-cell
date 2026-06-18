package com.h2grow.skat_load_cell.presentation.mainScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MainScreen(
    onGoToScanner: () -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (uiState.isConnected) "Подключено" else "Не подключено",
            style = MaterialTheme.typography.titleMedium,
            color = if (uiState.isConnected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )

        uiState.deviceName?.let { name ->
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MetricRow(label = "Вес", value = "${"%.1f".format(uiState.weight)} г")
                MetricRow(label = "Ток", value = "${"%.3f".format(uiState.current)} А")
                MetricRow(label = "Напряжение", value = "${"%.2f".format(uiState.voltage)} В")

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
            }
        }

        Button(onClick = onGoToScanner) {
            Text(if (uiState.isConnected) "Сменить устройство" else "Найти устройство")
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Column {
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
