package com.h2grow.skat_load_cell.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.h2grow.skat_load_cell.presentation.charts.ChartFullscreenScreen
import com.h2grow.skat_load_cell.presentation.charts.ChartSeries
import com.h2grow.skat_load_cell.presentation.charts.ChartsDetailScreen
import com.h2grow.skat_load_cell.presentation.mainScreen.MainScreen
import com.h2grow.skat_load_cell.presentation.scannerScreen.ScannerScreen
import com.h2grow.skat_load_cell.presentation.settingsScreen.SettingsScreen

@Composable
fun AppNav() {
    val router = remember { Router() }
    NavDisplay(
        backStack = router.backStack,
        onBack = { router.pop() },
        entryProvider = { key ->
            when (key) {
                is Route.Main -> NavEntry(key) {
                    MainScreen(
                        onGoToScanner = { router.push(Route.Scanner) },
                        onOpenChartsDetail = { router.push(Route.ChartsDetail) },
                        onOpenSettings = { router.push(Route.Settings) },
                    )
                }

                is Route.Settings -> NavEntry(key) {
                    SettingsScreen(onBack = { router.pop() })
                }

                is Route.Scanner -> NavEntry(key) {
                    ScannerScreen(
                        onBack = { router.pop() },
                        onConnected = { router.pop() },
                    )
                }

                is Route.ChartsDetail -> NavEntry(key) {
                    ChartsDetailScreen(
                        onBack = { router.pop() },
                        onOpenFullscreen = { series ->
                            router.push(Route.ChartFullscreen(series.name))
                        },
                    )
                }

                is Route.ChartFullscreen -> NavEntry(key) {
                    val series = runCatching { ChartSeries.valueOf(key.seriesName) }
                        .getOrDefault(ChartSeries.FORCE)
                    ChartFullscreenScreen(
                        series = series,
                        onBack = { router.pop() },
                    )
                }
            }
        },
    )
}
