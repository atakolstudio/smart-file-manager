package com.example.smartfilemanager.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dosya tarihlerini (son değiştirilme vb.) okunabilir metne çeviren yardımcı sınıf.
 */
object DateUtils {

    private val turkishLocale: Locale = Locale.Builder().setLanguage("tr").setRegion("TR").build()
    private val displayFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", turkishLocale)

    fun formatTimestamp(timestampMillis: Long): String {
        return displayFormat.format(Date(timestampMillis))
    }
}
