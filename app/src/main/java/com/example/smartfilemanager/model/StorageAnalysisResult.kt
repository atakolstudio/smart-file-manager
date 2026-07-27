package com.example.smartfilemanager.model

/**
 * Depolama analizi ekranında gösterilen en büyük dosya girdisi.
 */
data class LargeFileEntry(
    val path: String,
    val name: String,
    val sizeBytes: Long
)

/**
 * Aynı boyut ve içerik özetine (hash) sahip, yinelenen olarak tespit edilen dosya grubu.
 * [paths] listesindeki ilk öğe "orijinal" kabul edilir, geri kalanlar temizlenebilir aday.
 */
data class DuplicateGroup(
    val sizeBytes: Long,
    val paths: List<String>
)

/**
 * İçinde hiç dosya/klasör bulunmayan boş klasör girdisi.
 */
data class EmptyFolderEntry(val path: String)

/**
 * Depolama analizi taramasının tüm sonuçlarını tek yerde toplayan model.
 */
data class StorageAnalysisResult(
    val categorySummaries: List<CategorySummary> = emptyList(),
    val largestFiles: List<LargeFileEntry> = emptyList(),
    val duplicateGroups: List<DuplicateGroup> = emptyList(),
    val emptyFolders: List<EmptyFolderEntry> = emptyList(),
    val scanTruncated: Boolean = false
)
