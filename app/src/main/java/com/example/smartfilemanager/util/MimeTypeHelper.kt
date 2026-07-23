package com.example.smartfilemanager.util

import android.webkit.MimeTypeMap
import com.example.smartfilemanager.model.FileCategory
import java.util.Locale

/**
 * Dosya uzantısından MIME type ve kategori çıkaran yardımcı sınıf.
 */
object MimeTypeHelper {

    private val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "heic", "heif")
    private val videoExtensions = setOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "3gp", "m4v")
    private val audioExtensions = setOf("mp3", "wav", "ogg", "flac", "aac", "m4a", "wma", "opus")
    private val documentExtensions = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md", "csv", "json", "xml", "html")
    private val archiveExtensions = setOf("zip", "rar", "7z", "tar", "gz", "bz2")
    private val apkExtensions = setOf("apk", "apks", "xapk")

    fun getMimeType(fileName: String): String? {
        val extension = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        if (extension.isEmpty()) return null
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    fun getCategory(fileName: String, isDirectory: Boolean): FileCategory {
        if (isDirectory) return FileCategory.FOLDER
        val extension = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return when (extension) {
            in imageExtensions -> FileCategory.IMAGE
            in videoExtensions -> FileCategory.VIDEO
            in audioExtensions -> FileCategory.AUDIO
            in documentExtensions -> FileCategory.DOCUMENT
            in archiveExtensions -> FileCategory.ARCHIVE
            in apkExtensions -> FileCategory.APK
            else -> FileCategory.OTHER
        }
    }
}
