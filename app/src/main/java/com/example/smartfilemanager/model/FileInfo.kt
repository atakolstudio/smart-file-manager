package com.example.smartfilemanager.model

/**
 * "Dosya Bilgisi" ekranında/diyaloğunda gösterilen ayrıntılı bilgi seti.
 */
data class FileInfo(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long,
    val creationTime: Long?,
    val mimeType: String?,
    val canRead: Boolean,
    val canWrite: Boolean,
    val canExecute: Boolean
)
