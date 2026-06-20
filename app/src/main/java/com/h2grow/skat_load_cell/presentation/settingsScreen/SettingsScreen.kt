package com.h2grow.skat_load_cell.presentation.settingsScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
    var shuntExtText by remember { mutableStateOf("0.005") }
    var shuntBrdText by remember { mutableStateOf("0.100") }
    var includeBoardShunt by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Калибровка и настройки") },
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
                    text = "Подключите SKAT-Tenzo для калибровки",
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
                StatusLine("Scale", "%.3f".format(uiState.scale))
                StatusLine("Raw ADC", uiState.hx711Raw.toString())
                if (uiState.hx711Ok && kotlin.math.abs(uiState.hx711Raw) < 500) {
                    Text(
                        text = "Raw ≈ 0: проверьте DT→GPIO19, SCK→GPIO18, VCC/GND. После прошивки нажмите «Обнулить» без груза.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Text(
                    text = "1. Снимите груз. 2. Обнулите. 3. Положите эталонный груз. 4. Введите массу и калибруйте.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ActionButton("Обнулить (Tare)", uiState) { viewModel.tare() }
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

            SensorCard(title = "Ток INA226") {
                StatusLine("Статус", if (uiState.ina226Ok) "OK" else "нет I2C")
                StatusLine("Калибровка шунта", if (uiState.ina226CalOk) "OK" else "нет")
                StatusLine("Напряжение шины", "${"%.2f".format(uiState.busVoltage)} В")
                StatusLine("Шунт ΔU", "${"%.3f".format(uiState.shuntMv)} mV")
                StatusLine("Eff. R", "${"%.5f".format(uiState.shuntOhm)} Ω")
                if (uiState.ina226Addr != 0) {
                    StatusLine("Адрес I2C", "0x${uiState.ina226Addr.toString(16).uppercase()}")
                }
                if (uiState.busVoltage < 1f && uiState.ina226Ok) {
                    Text(
                        text = "Bus = 0 В: подключите VIN+ модуля INA226 к BAT+ аккумулятора (14.8 В). " +
                            "GND модуля — общая земля с PDB.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Text(
                    text = "High-side: BAT+ → IN+ → [внешний 5 mΩ] → IN− → PDB+. " +
                        "R100 на модуле (0.1 Ω) учитывается только если ток идёт и через него (переключатель ниже). " +
                        "Если shunt ΔU = 0 при нагрузке — IN+/IN− не на вашем шунте.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = shuntExtText,
                    onValueChange = { shuntExtText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Внешний шунт, Ω") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    enabled = uiState.isConnected && !uiState.isBusy,
                    singleLine = true,
                )
                OutlinedTextField(
                    value = shuntBrdText,
                    onValueChange = { shuntBrdText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("R100 на модуле, Ω") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    enabled = uiState.isConnected && !uiState.isBusy,
                    singleLine = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Добавить R100 модуля (серия)")
                    Switch(
                        checked = includeBoardShunt,
                        onCheckedChange = { includeBoardShunt = it },
                        enabled = uiState.isConnected && !uiState.isBusy,
                    )
                }
                ActionButton("Применить шунт", uiState) {
                    viewModel.applyShunt(
                        extOhm = shuntExtText.replace(',', '.').toFloatOrNull() ?: 0.005f,
                        brdOhm = shuntBrdText.replace(',', '.').toFloatOrNull() ?: 0.1f,
                        includeBoard = includeBoardShunt,
                    )
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
                ActionButton("Обнулить ток (без нагрузки)", uiState) { viewModel.zeroCurrent() }
                ActionButton("Перекалибровать INA226", uiState) { viewModel.recalibrateIna226() }
            }

            SensorCard(title = "ESC / двигатели T-Motor") {
                StatusLine("PWM", "50 Гц, ${uiState.escMinUs}–${uiState.escMaxUs} µs")
                StatusLine("Текущий импульс", "${uiState.escPulseUs} µs")
                StatusLine("Arm", if (uiState.motorsArmed) "да" else "нет")
                Text(
                    text = "Skywalker 40A: белый → GPIO25/26, чёрный GND, 1000 µs = стоп, 2000 µs = макс. " +
                        "При включении ESC получает мин. импульс 2 с. Снимите пропellers при настройке!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
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
