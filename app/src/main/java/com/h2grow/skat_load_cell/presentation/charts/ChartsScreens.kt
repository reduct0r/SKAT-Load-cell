package com.h2grow.skat_load_cell.presentation.charts

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsDetailScreen(
    onBack: () -> Unit,
    onOpenFullscreen: (ChartSeries) -> Unit,
    viewModel: ChartsViewModel = hiltViewModel(),
) {
    val samples by viewModel.samples.collectAsStateWithLifecycle()
    val visibility by viewModel.visibility.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
        topBar = {
            TopAppBar(
                title = { Text("Графики — подробно") },
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
            ChartSeries.entries.forEach { series ->
                DetailChartItem(
                    series = series,
                    samples = samples,
                    visibility = visibility,
                    onOpenFullscreen = { onOpenFullscreen(series) },
                    onExportPng = { bitmap ->
                        viewModel.exportPng(bitmap, series.name.lowercase())
                            ?.let { context.startActivity(it) }
                    },
                )
            }

            ExportSection(
                onExportAllCsv = {
                    viewModel.exportCsv(samples, "all")?.let { context.startActivity(it) }
                },
                onExportSeriesCsv = { series ->
                    viewModel.exportCsv(samples, series.name.lowercase())
                        ?.let { context.startActivity(it) }
                },
            )
        }
    }
}

@Composable
private fun DetailChartItem(
    series: ChartSeries,
    samples: List<ChartSample>,
    visibility: ChartVisibility,
    onOpenFullscreen: () -> Unit,
    onExportPng: (androidx.compose.ui.graphics.ImageBitmap) -> Unit,
) {
    val chartLayer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenFullscreen),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            RowWithExport(
                title = series.label,
                titleColor = series.color,
                onExportPng = {
                    scope.launch {
                        onExportPng(chartLayer.toImageBitmap())
                    }
                },
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .drawWithContent {
                        chartLayer.record {
                            this@drawWithContent.drawContent()
                        }
                        drawLayer(chartLayer)
                    },
            ) {
                TelemetryMultiChart(
                    samples = samples,
                    visibility = visibility,
                    singleSeries = series,
                    showAxes = true,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Text(
                text = "Нажмите для полноэкранного режима (альбомная ориентация)",
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun RowWithExport(
    title: String,
    titleColor: androidx.compose.ui.graphics.Color,
    onExportPng: () -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
            color = titleColor,
        )
        Button(onClick = onExportPng) {
            Text("PNG")
        }
    }
}

@Composable
private fun ExportSection(
    onExportAllCsv: () -> Unit,
    onExportSeriesCsv: (ChartSeries) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Экспорт CSV",
            style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
        )
        Button(onClick = onExportAllCsv, modifier = Modifier.fillMaxWidth()) {
            Text("CSV — все параметры")
        }
        ChartSeries.entries.forEach { series ->
            Button(
                onClick = { onExportSeriesCsv(series) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("CSV — ${series.label}")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartFullscreenScreen(
    series: ChartSeries,
    onBack: () -> Unit,
    viewModel: ChartsViewModel = hiltViewModel(),
) {
    val samples by viewModel.samples.collectAsStateWithLifecycle()
    val visibility by viewModel.visibility.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity
    val chartLayer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()

    DisposableEffect(activity) {
        val previous = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
        topBar = {
            TopAppBar(
                title = { Text(series.label) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .drawWithContent {
                        chartLayer.record {
                            this@drawWithContent.drawContent()
                        }
                        drawLayer(chartLayer)
                    },
            ) {
                TelemetryMultiChart(
                    samples = samples,
                    visibility = visibility,
                    singleSeries = series,
                    showAxes = true,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        viewModel.exportCsv(samples, series.name.lowercase())
                            ?.let { context.startActivity(it) }
                    },
                ) { Text("CSV") }
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.exportPng(chartLayer.toImageBitmap(), series.name.lowercase())
                                ?.let { context.startActivity(it) }
                        }
                    },
                ) { Text("PNG") }
            }
        }
    }
}
