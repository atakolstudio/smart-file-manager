package com.example.smartfilemanager.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dosya tarihlerini (son değiştirilme vb.) okunabilir metne çeviren yardımcı sınıf.
 */
object DateUtils {

    private val displayFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR"))

    fun formatTimestamp(timestampMillis: Long): String {
        return displayFormat.format(Date(timestampMillis))
    }
}
