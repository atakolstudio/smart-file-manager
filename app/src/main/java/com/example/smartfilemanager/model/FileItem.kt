package com.example.smartfilemanager.model

/**
 * Dosya sisteminde tek bir dosya ya da klasörü temsil eden değişmez model.
 */
data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long,
    val mimeType: String?,
    val isHidden: Boolean = false
) {
    val extension: String
        get() = if (isDirectory) "" else name.substringAfterLast('.', "")

    companion object {
        fun fromFile(file: java.io.File): FileItem {
            return FileItem(
                name = file.name,
                path = file.absolutePath,
                isDirectory = file.isDirectory,
                sizeBytes = if (file.isDirectory) 0L else file.length(),
                lastModified = file.lastModified(),
                mimeType = if (file.isDirectory) null else
                    com.example.smartfilemanager.util.MimeTypeHelper.getMimeType(file.name),
                isHidden = file.isHidden
            )
        }
    }
}

/**
 * Dosyaların ana kategorileri (ana sayfadaki hızlı erişim kutuları ve filtreleme için).
 */
enum class FileCategory {
    IMAGE, VIDEO, AUDIO, DOCUMENT, ARCHIVE, APK, OTHER, FOLDER
}

/**
 * Bir kategoriye ait toplam dosya sayısı ve boyutu.
 */
data class CategorySummary(
    val category: FileCategory,
    val fileCount: Int,
    val totalSizeBytes: Long
)
