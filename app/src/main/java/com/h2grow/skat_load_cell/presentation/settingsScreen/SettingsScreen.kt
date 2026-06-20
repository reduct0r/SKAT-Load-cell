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
                StatusLine("Статус", if (uiState.hx711Ok) "OK" else "нет связи")
                StatusLine("Коэффициент scale", "%.3f".format(uiState.scale))
                StatusLine("Масса", "${"%.1f".format(uiState.forceGrams)} г")
                StatusLine("Сырой ADC", uiState.hx711Raw.toString())
                Hint(
                    "Калибровка сохраняется в памяти ESP32 (scale и нулевая точка offset). " +
                        "Приложение Android данные не хранит.",
                )
                Hint(
                    "Калибровка: снять нагрузку → «Обнулить» → установить эталонную массу → " +
                        "ввести массу в граммах → «Калибровать по массе». Эталон не снимать до завершения.",
                )
                Hint(
                    "«Обнулить» — установить текущее состояние как нулевую точку (0 г). " +
                        "«Сбросить шкалу» — восстановить коэффициент по умолчанию (420) и выполнить обнуление.",
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
                StatusLine("Статус", if (uiState.ina226Ok) "OK" else "нет I2C")
                StatusLine("Напряжение шины", "${"%.2f".format(uiState.busVoltage)} В")
                StatusLine("Масштаб напряжения", "${uiState.busVScaleE4} (10000 = без коррекции)")
                StatusLine("ΔU шунта", "${"%.3f".format(uiState.shuntMv)} mV")
                StatusLine("Сопротивление шунта", "${formatOhm(uiState.shuntExtOhm)} Ω")
                Hint(
                    "Измерение: I = ΔU / R. Шунт устанавливается в разрыв плюсовой линии между IN+ и IN−. " +
                        "VIN+ подключается к BAT+ (той же точке, где измеряется эталонное напряжение).",
                )
                Hint(
                    "Калибровка напряжения: измерить напряжение шины мультиметром → ввести значение → " +
                        "«Калибровать напряжение». Повторная калибровка заменяет коэффициент, не суммируется.",
                )
                Hint(
                    "«Обнулить ток» — зафиксировать потребление в режиме покоя и вычитать смещение. " +
                        "Двигатель должен быть disarm, нагрузка отсутствует.",
                )
                Hint(
                    "«Перекалибровать INA226» — пересчёт внутренних регистров микросхемы. " +
                        "Применяется после изменения сопротивления шунта.",
                )
                OutlinedTextField(
                    value = refBusVText,
                    onValueChange = { refBusVText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Эталонное напряжение, В") },
                    supportingText = { Text("Значение с мультиметра на BAT+") },
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

            SensorCard(title = "Привод ESC (Skywalker 40A)") {
                StatusLine("PWM", "50 Гц, ${uiState.escMinUs}–${uiState.escMaxUs} µs")
                StatusLine("Импульс", "${uiState.escPulseUs} µs")
                StatusLine("Arm", if (uiState.motorsArmed) "да" else "нет")
                Hint(
                    "Управление: сигнал — GPIO25/26, общий GND. Диапазон импульса: 1000 µs (стоп) — 2000 µs (максимум).",
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
