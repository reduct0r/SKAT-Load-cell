package com.h2grow.skat_load_cell.presentation.charts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ChartPanelCard(
    samples: List<ChartSample>,
    visibility: ChartVisibility,
    onToggleSeries: (ChartSeries, Boolean) -> Unit,
    onReset: () -> Unit,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier,
    chartHeight: androidx.compose.ui.unit.Dp = 200.dp,
    showDetailsButton: Boolean = true,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Графики",
                    style = MaterialTheme.typography.titleMedium,
                )
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Настройки графика",
                        )
                    }
                    ChartSettingsMenu(
                        expanded = menuExpanded,
                        onDismiss = { menuExpanded = false },
                        visibility = visibility,
                        onToggleSeries = onToggleSeries,
                        onReset = onReset,
                        onOpenDetails = onOpenDetails,
                    )
                }
            }

            LegendRow(visibility = visibility)

            TelemetryMultiChart(
                samples = samples,
                visibility = visibility,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight),
            )

            if (showDetailsButton) {
                TextButton(
                    onClick = onOpenDetails,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text("Подробнее")
                }
            }
        }
    }
}

@Composable
fun ChartSettingsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    visibility: ChartVisibility,
    onToggleSeries: (ChartSeries, Boolean) -> Unit,
    onReset: () -> Unit,
    onOpenDetails: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        Text(
            text = "Отображение",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ChartSeries.entries.forEach { series ->
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = visibility.isVisible(series),
                            onCheckedChange = { checked ->
                                onToggleSeries(series, checked)
                            },
                            modifier = Modifier.size(32.dp),
                        )
                        Text(
                            text = series.label,
                            color = series.color,
                        )
                    }
                },
                onClick = {
                    onToggleSeries(series, !visibility.isVisible(series))
                },
            )
        }
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Сбросить") },
            onClick = {
                onReset()
                onDismiss()
            },
        )
        DropdownMenuItem(
            text = { Text("Подробнее") },
            onClick = {
                onOpenDetails()
                onDismiss()
            },
        )
    }
}

@Composable
private fun LegendRow(visibility: ChartVisibility) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ChartSeries.entries.filter { visibility.isVisible(it) }.forEach { series ->
            Text(
                text = "● ${series.label}",
                style = MaterialTheme.typography.labelSmall,
                color = series.color,
            )
        }
    }
}

@Composable
fun SingleChartCard(
    series: ChartSeries,
    samples: List<ChartSample>,
    visibility: ChartVisibility,
    onOpenFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenFullscreen),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = series.label,
                style = MaterialTheme.typography.titleSmall,
                color = series.color,
            )
            TelemetryMultiChart(
                samples = samples,
                visibility = visibility,
                singleSeries = series,
                showAxes = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            )
            Text(
                text = "Нажмите для полноэкранного режима",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
fun ExportButtonsRow(
    onExportAllCsv: () -> Unit,
    onExportAllPng: () -> Unit,
    onExportSeriesCsv: (ChartSeries) -> Unit,
    onExportSeriesPng: (ChartSeries) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Экспорт",
            style = MaterialTheme.typography.titleSmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onExportAllCsv) { Text("CSV все") }
            Button(onClick = onExportAllPng) { Text("PNG все") }
        }
        ChartSeries.entries.forEach { series ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onExportSeriesCsv(series) }) {
                    Text("CSV ${series.label}")
                }
                Button(onClick = { onExportSeriesPng(series) }) {
                    Text("PNG ${series.label}")
                }
            }
        }
    }
}
