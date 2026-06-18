package com.h2grow.skat_load_cell.presentation.mainScreen

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun MainScreen(onGoToScanner: () -> Unit) {
    Button(onClick = onGoToScanner) {
        Text("Go to scan")
    }
}