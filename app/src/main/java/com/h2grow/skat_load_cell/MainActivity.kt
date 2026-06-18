package com.h2grow.skat_load_cell

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.h2grow.skat_load_cell.navigation.AppNav
import com.h2grow.skat_load_cell.ui.theme.SKATLoadcellTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SKATLoadcellTheme {
                AppNav()
            }
        }
    }
}
