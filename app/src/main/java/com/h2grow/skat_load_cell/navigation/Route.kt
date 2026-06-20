package com.h2grow.skat_load_cell.navigation

sealed class Route {
    data object Main : Route()
    data object Scanner : Route()
    data object ChartsDetail : Route()
    data class ChartFullscreen(val seriesName: String) : Route()
}
