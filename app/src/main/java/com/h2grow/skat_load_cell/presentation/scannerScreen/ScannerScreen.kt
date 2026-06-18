package com.h2grow.skat_load_cell.presentation.scannerScreen

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.h2grow.skat_load_cell.data.ble.SkatLoadCellSpec
import com.h2grow.skat_load_cell.domain.model.ScannedDevice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    onBack: () -> Unit,
    onConnected: () -> Unit = onBack,
    viewModel: ScannerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = requiredPermissions.all { permission ->
            result[permission] == true
        }
        viewModel.onPermissionsResult(granted)
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(requiredPermissions)
    }

    LaunchedEffect(Unit) {
        viewModel.connected.collect {
            onConnected()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Поиск устройств") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Назад")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.startScan() },
                        enabled = uiState.permissionsGranted && !uiState.isScanning && !uiState.isConnecting,
                    ) {
                        Text("Обновить")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ScannerStatusBar(uiState = uiState)

            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            when {
                !uiState.permissionsGranted -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Разрешите доступ к Bluetooth")
                    }
                }

                uiState.devices.isEmpty() && uiState.isScanning -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Text(
                                text = "Сканирование…",
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                    }
                }

                uiState.devices.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Устройства не найдены")
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        items(
                            items = uiState.devices,
                            key = { it.device.address },
                        ) { device ->
                            DeviceListItem(
                                device = device,
                                isConnecting = uiState.isConnecting &&
                                    uiState.connectingAddress == device.device.address,
                                onClick = { viewModel.connectToDevice(device) },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannerStatusBar(uiState: ScannerUiState) {
    val statusText = when {
        uiState.isConnecting -> "Подключение…"
        uiState.isScanning -> "Сканирование… (${uiState.devices.size})"
        uiState.devices.isNotEmpty() -> "Найдено: ${uiState.devices.size}"
        else -> "Сканирование остановлено"
    }

    Text(
        text = statusText,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun DeviceListItem(
    device: ScannedDevice,
    isConnecting: Boolean,
    onClick: () -> Unit,
) {
    val isSkatDevice = device.name.contains("SKAT", ignoreCase = true) ||
        device.name == SkatLoadCellSpec.DEVICE_NAME

    ListItem(
        modifier = Modifier.clickable(enabled = !isConnecting, onClick = onClick),
        headlineContent = {
            Text(
                text = device.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(text = device.device.address)
        },
        trailingContent = {
            if (isConnecting) {
                CircularProgressIndicator()
            } else {
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "${device.rssi} dBm")
                    if (isSkatDevice) {
                        Text(
                            text = "SKAT",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        },
    )
}
