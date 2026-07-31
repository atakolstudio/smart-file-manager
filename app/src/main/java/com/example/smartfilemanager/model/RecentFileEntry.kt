package com.example.smartfilemanager.model

/**
 * Ana sayfadaki "Son Kullanılanlar" bölümünde gösterilen, en son değiştirilmiş dosya girdisi.
 */
data class RecentFileEntry(
    val path: String,
    val name: String,
    val lastModified: Long,
    val sizeBytes: Long
)
