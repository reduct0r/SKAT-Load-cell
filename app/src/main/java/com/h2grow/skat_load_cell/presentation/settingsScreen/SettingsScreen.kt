package com.h2grow.skat_load_cell.presentation.settingsScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var calGramsText by remember { mutableStateOf("") }
    var shuntOhmText by remember { mutableStateOf("0.005") }
    var refBusVText by remember { mutableStateOf("") }

    LaunchedEffect(uiState.shuntExtOhm, uiState.isConnected) {
        if (uiState.isConnected && uiState.shuntExtOhm > 0f) {
            shuntOhmText = formatOhm(uiState.shuntExtOhm)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Калибровка") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!uiState.isConnected) {
                Text(
                    text = "Требуется подключение к SKAT-Tenzo",
                    color = MaterialTheme.colorScheme.error,
                )
            }

            uiState.statusMessage?.let { msg ->
                Text(
                    text = msg,
                    color = if (uiState.isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }

            SensorCard(title = "Тензодатчик HX711") {
                StatusLine("Статус", if (uiState.hx711Ok) "исправен" else "нет связи")
                StatusLine("Текущая масса", "${"%.1f".format(uiState.forceGrams)} г")
                Hint(
                    "Порядок калибровки: снять нагрузку → «Обнулить» → установить эталонную массу → " +
                        "указать массу в граммах → «Калибровать по массе». Эталон не снимать до завершения.",
                )
                Hint(
                    "«Обнулить» — зафиксировать нулевую точку без нагрузки. " +
                        "«Сбросить шкалу» — вернуть заводские параметры и обнулить датчик.",
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Инвертировать знак силы")
                    Switch(
                        checked = uiState.forceSign < 0,
                        onCheckedChange = { viewModel.setForceInverted(it) },
                        enabled = uiState.isConnected && !uiState.isBusy,
                    )
                }
                ActionButton("Обнулить", uiState) { viewModel.tare() }
                OutlinedTextField(
                    value = calGramsText,
                    onValueChange = { calGramsText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Эталонная масса, г") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    enabled = uiState.isConnected && !uiState.isBusy,
                    singleLine = true,
                )
                ActionButton("Калибровать по массе", uiState) {
                    viewModel.calibrate(calGramsText.replace(',', '.').toFloatOrNull() ?: 0f)
                }
                ActionButton("Сбросить шкалу", uiState) { viewModel.resetScale() }
            }

            SensorCard(title = "Датчик тока INA226") {
                StatusLine("Статус", if (uiState.ina226Ok) "исправен" else "нет связи")
                StatusLine("Напряжение", "${"%.2f".format(uiState.busVoltage)} В")
                Hint(
                    "Шунт: указать сопротивление в омах → «Применить шунт». " +
                        "После смены шунта нажать «Перекалибровать INA226».",
                )
                Hint(
                    "Напряжение: измерить мультиметром на клеммах питания → ввести значение → " +
                        "«Калибровать напряжение».",
                )
                Hint(
                    "Ток: при выключенном приводе и отсутствии нагрузки нажать «Обнулить ток».",
                )
                OutlinedTextField(
                    value = refBusVText,
                    onValueChange = { refBusVText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Эталонное напряжение, В") },
                    supportingText = { Text("Показание мультиметра") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    enabled = uiState.isConnected && !uiState.isBusy,
                    singleLine = true,
                )
                ActionButton("Калибровать напряжение", uiState) {
                    viewModel.calibrateBusVoltage(
                        refBusVText.replace(',', '.').toFloatOrNull() ?: 0f,
                    )
                }
                OutlinedTextField(
                    value = shuntOhmText,
                    onValueChange = { shuntOhmText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Сопротивление шунта, Ω") },
                    supportingText = { Text("3,6 mΩ = 0,0036") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    enabled = uiState.isConnected && !uiState.isBusy,
                    singleLine = true,
                )
                ActionButton("Применить шунт", uiState) {
                    viewModel.applyShunt(shuntOhmText.replace(',', '.').toFloatOrNull() ?: 0.005f)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Инвертировать знак тока")
                    Switch(
                        checked = uiState.currentSign < 0,
                        onCheckedChange = { viewModel.setCurrentInverted(it) },
                        enabled = uiState.isConnected && !uiState.isBusy,
                    )
                }
                ActionButton("Обнулить ток", uiState) { viewModel.zeroCurrent() }
                ActionButton("Перекалибровать INA226", uiState) { viewModel.recalibrateIna226() }
            }

            SensorCard(title = "Привод ESC") {
                Hint(
                    "Диапазон импульса: ${uiState.escMinUs}–${uiState.escMaxUs} µs " +
                        "(минимум — останов, максимум — полная мощность).",
                )
            }
        }
    }
}

private fun formatOhm(value: Float): String =
    when {
        value >= 0.01f -> "%.3f".format(value)
        else -> "%.5f".format(value)
    }

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SensorCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun StatusLine(label: String, value: String) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun ActionButton(
    text: String,
    uiState: SettingsUiState,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = uiState.isConnected && !uiState.isBusy,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (uiState.isBusy) {
            CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
        }
        Text(text)
    }
}
