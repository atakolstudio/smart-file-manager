package com.example.smartfilemanager.util

import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

/**
 * Byte cinsinden verilen değeri "12,3 MB" gibi okunabilir bir metne çevirir.
 */
object SizeFormatter {

    private val units = arrayOf("B", "KB", "MB", "GB", "TB")

    fun format(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val digitGroups = (ln(bytes.toDouble()) / ln(1024.0)).toInt()
            .coerceIn(0, units.size - 1)
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        return String.format(Locale.getDefault(), "%.1f %s", value, units[digitGroups])
    }
}
