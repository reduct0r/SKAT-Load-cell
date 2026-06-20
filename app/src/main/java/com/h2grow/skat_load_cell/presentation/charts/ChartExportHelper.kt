package com.h2grow.skat_load_cell.presentation.charts

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChartExportHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val timestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    fun exportCsv(samples: List<ChartSample>, fileLabel: String): Intent? {
        if (samples.isEmpty()) return null
        val file = createExportFile("skat_${fileLabel}_${timestampFormat.format(Date())}.csv")
        file.bufferedWriter().use { writer ->
            writer.appendLine("timestamp_ms,force_n,current_a,voltage_v")
            samples.forEach { sample ->
                writer.appendLine(
                    "${sample.timestampMs},${sample.forceNewtons},${sample.currentAmps},${sample.voltage}",
                )
            }
        }
        return shareFile(file, "text/csv")
    }

    fun exportPng(image: ImageBitmap, fileLabel: String): Intent? {
        val file = createExportFile("skat_${fileLabel}_${timestampFormat.format(Date())}.png")
        FileOutputStream(file).use { stream ->
            image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        return shareFile(file, "image/png")
    }

    private fun createExportFile(name: String): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        return File(dir, name)
    }

    private fun shareFile(file: File, mimeType: String): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
