package com.h2grow.skat_load_cell.presentation.charts

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import com.h2grow.skat_load_cell.data.telemetry.TelemetryRecorder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope

@HiltViewModel
class ChartsViewModel @Inject constructor(
    private val recorder: TelemetryRecorder,
    private val exportHelper: ChartExportHelper,
) : ViewModel() {

    val samples: StateFlow<List<ChartSample>> = recorder.samples
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val visibility: StateFlow<ChartVisibility> = recorder.visibility
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChartVisibility())

    fun toggleSeries(series: ChartSeries, visible: Boolean) {
        recorder.toggleSeries(series, visible)
    }

    fun reset() {
        recorder.reset()
    }

    fun exportCsv(samples: List<ChartSample>, label: String) =
        exportHelper.exportCsv(samples, label)

    fun exportPng(image: ImageBitmap, label: String) =
        exportHelper.exportPng(image, label)
}
