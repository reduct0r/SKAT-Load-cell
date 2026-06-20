package com.h2grow.skat_load_cell.data.telemetry

import com.h2grow.skat_load_cell.data.ble.SkatLoadCellManager
import com.h2grow.skat_load_cell.presentation.charts.ChartSample
import com.h2grow.skat_load_cell.presentation.charts.ChartVisibility
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Singleton
class TelemetryRecorder @Inject constructor(
    loadCellManager: SkatLoadCellManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _samples = MutableStateFlow<List<ChartSample>>(emptyList())
    val samples: StateFlow<List<ChartSample>> = _samples.asStateFlow()

    private val _visibility = MutableStateFlow(ChartVisibility())
    val visibility: StateFlow<ChartVisibility> = _visibility.asStateFlow()

    init {
        combine(loadCellManager.telemetry, loadCellManager.isConnected) { telemetry, connected ->
            connected to telemetry
        }.onEach { (connected, telemetry) ->
            if (!connected) return@onEach
            append(
                ChartSample(
                    timestampMs = System.currentTimeMillis(),
                    forceNewtons = telemetry.forceNewtons,
                    currentAmps = telemetry.currentAmps,
                    voltage = telemetry.busVoltage,
                ),
            )
        }.launchIn(scope)
    }

    fun setVisibility(visibility: ChartVisibility) {
        _visibility.value = visibility
    }

    fun toggleSeries(series: com.h2grow.skat_load_cell.presentation.charts.ChartSeries, visible: Boolean) {
        _visibility.value = _visibility.value.withToggle(series, visible)
    }

    fun reset() {
        _samples.value = emptyList()
    }

    private fun append(sample: ChartSample) {
        val updated = (_samples.value + sample).let { list ->
            if (list.size <= MAX_SAMPLES) list else list.takeLast(MAX_SAMPLES)
        }
        _samples.value = updated
    }

    companion object {
        const val MAX_SAMPLES = 1200
    }
}
