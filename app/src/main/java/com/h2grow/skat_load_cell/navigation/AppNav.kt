package com.h2grow.skat_load_cell.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.h2grow.skat_load_cell.presentation.mainScreen.MainScreen
import com.h2grow.skat_load_cell.presentation.scannerScreen.ScannerScreen

@Composable
fun AppNav() {
    val router = remember { Router() }
    NavDisplay(
        backStack = router.backStack,
        onBack = { router.pop() },
        entryProvider = { key ->
            when (key) {
                is Route.Main-> NavEntry(key) {
                    MainScreen(
                        onGoToScanner = {
                            router.push(Route.Scanner)
                        }
                    )
                }

                is Route.Scanner -> NavEntry(key) {
                    ScannerScreen(
                        onBack = { router.pop()  }
                    )
                }

            }
        }
    )
}
