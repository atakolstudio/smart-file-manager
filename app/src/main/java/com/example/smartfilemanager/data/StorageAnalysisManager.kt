package com.example.smartfilemanager.data

import com.example.smartfilemanager.di.IoDispatcher
import com.example.smartfilemanager.model.CategorySummary
import com.example.smartfilemanager.model.DuplicateGroup
import com.example.smartfilemanager.model.EmptyFolderEntry
import com.example.smartfilemanager.model.LargeFileEntry
import com.example.smartfilemanager.model.StorageAnalysisResult
import com.example.smartfilemanager.util.HashAlgorithm
import com.example.smartfilemanager.util.HashCalculator
import com.example.smartfilemanager.util.MimeTypeHelper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Depolama analizini (en büyük dosyalar, yinelenen dosyalar, boş klasörler) yürütür.
 *
 * ÖNEMLİ: Tüm tarama, [StorageManager.getCommonDirectories] ile sınırlı klasörlerde
 * (DCIM/Pictures/Movies/Music/Documents/Download) yapılır ve toplam [SCAN_TIMEOUT_MS]
 * ile sınırlanır — cihazın tüm depolama alanını (Android/data, önbellek vb. dahil)
 * taramak, çok sayıda dosyası olan gerçek cihazlarda dakikalarca sürebilir ve arayüzün
 * sonsuza kadar "yükleniyor" durumunda kalmasına yol açar. Zaman aşımında o ana kadar
 * toplanan sonuçlar (kısmi de olsa) döndürülür; asla sonsuz beklemeye izin verilmez.
 */
@Singleton
class StorageAnalysisManager @Inject constructor(
    private val storageManager: StorageManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    companion object {
        private const val SCAN_TIMEOUT_MS = 20_000L
        private const val MAX_LARGEST_FILES = 30
        private const val MAX_HASH_CANDIDATES = 300
    }

    suspend fun analyze(): OperationResult<StorageAnalysisResult> =
        withContext(ioDispatcher) {
            safeFileOperation("Depolama analizi başarısız oldu") {
                val allFiles = mutableListOf<File>()
                val emptyFolders = mutableListOf<EmptyFolderEntry>()

                val completed = withTimeoutOrNull(SCAN_TIMEOUT_MS) {
                    storageManager.getCommonDirectories().values.distinctBy { it.absolutePath }.forEach { root ->
                        if (!root.exists() || !root.canRead()) return@forEach
                        root.walkTopDown()
                            .onEnter { dir -> !dir.name.startsWith(".") }
                            .forEach { entry ->
                                if (entry.isDirectory) {
                                    if (entry.absolutePath != root.absolutePath && entry.listFiles()?.isEmpty() == true) {
                                        emptyFolders += EmptyFolderEntry(entry.absolutePath)
                                    }
                                } else {
                                    allFiles += entry
                                }
                            }
                    }
                    true
                }

                val categorySummaries = buildCategorySummaries(allFiles)
                val largestFiles = allFiles
                    .sortedByDescending { it.length() }
                    .take(MAX_LARGEST_FILES)
                    .map { LargeFileEntry(it.absolutePath, it.name, it.length()) }

                val duplicateGroups = withTimeoutOrNull(SCAN_TIMEOUT_MS) {
                    findDuplicates(allFiles)
                } ?: emptyList()

                StorageAnalysisResult(
                    categorySummaries = categorySummaries,
                    largestFiles = largestFiles,
                    duplicateGroups = duplicateGroups,
                    emptyFolders = emptyFolders,
                    scanTruncated = completed == null
                )
            }
        }

    private fun buildCategorySummaries(files: List<File>): List<CategorySummary> {
        val counters = mutableMapOf<com.example.smartfilemanager.model.FileCategory, Pair<Int, Long>>()
        files.forEach { file ->
            val category = MimeTypeHelper.getCategory(file.name, isDirectory = false)
            val current = counters[category] ?: (0 to 0L)
            counters[category] = (current.first + 1) to (current.second + file.length())
        }
        return com.example.smartfilemanager.model.FileCategory.entries
            .filter { it != com.example.smartfilemanager.model.FileCategory.FOLDER }
            .map { category ->
                val (count, size) = counters[category] ?: (0 to 0L)
                CategorySummary(category, count, size)
            }
    }

    /**
     * Yinelenen dosyaları tespit eder: önce boyuta göre gruplar (ucuz), sadece birden fazla
     * dosya içeren boyut gruplarında (adayları [MAX_HASH_CANDIDATES] ile sınırlayarak) hash
     * hesaplar. Bu iki aşamalı yaklaşım, her dosyanın hash'ini hesaplamaktan çok daha ucuzdur.
     */
    private fun findDuplicates(files: List<File>): List<DuplicateGroup> {
        val bySize = files.filter { it.length() > 0 }.groupBy { it.length() }
        val candidates = bySize.values.filter { it.size > 1 }.flatten().take(MAX_HASH_CANDIDATES)

        return candidates
            .groupBy { file ->
                runCatching { HashCalculator.calculate(file, HashAlgorithm.MD5) }.getOrNull()
            }
            .filterKeys { it != null }
            .values
            .filter { it.size > 1 }
            .map { group ->
                DuplicateGroup(
                    sizeBytes = group.first().length(),
                    paths = group.map { it.absolutePath }
                )
            }
    }
}
