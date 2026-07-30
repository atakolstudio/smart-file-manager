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
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Depolama analizini (en büyük dosyalar, yinelenen dosyalar, boş klasörler) yürütür.
 *
 * ÖNEMLİ: Tüm tarama, [StorageManager.getCommonDirectories] ile sınırlı klasörlerde
 * (DCIM/Pictures/Movies/Music/Documents/Download) yapılır. Üç bağımsız güvenlik katmanı var:
 * 1) SAYIYA dayalı üst sınır ([MAX_ENTRIES_TO_SCAN]) — dosya VE klasör dahil her ziyaret
 *    edilen düğümde artar (bir sembolik link döngüsü sonsuz KLASÖR üretebileceği için).
 * 2) [MAX_SCAN_DEPTH] ile klasör derinliği sınırı.
 * 3) Her klasör kendi [PER_DIRECTORY_TIMEOUT_MS] süresiyle ve `runInterruptible` ile taranır —
 *    bir klasör (ör. bulut senkronlu sanal bir galeri klasörü) TEK BİR I/O çağrısında senkron
 *    olarak bloke olsa bile (ki bu durumda 1 ve 2 numaralı sayaçlar hiç devreye giremez),
 *    zaman aşımında arka plan iş parçacığına gerçek bir kesme (Thread.interrupt) sinyali
 *    gönderilir ve o klasörden vazgeçilip diğerlerine geçilir.
 */
@Singleton
class StorageAnalysisManager @Inject constructor(
    private val storageManager: StorageManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    companion object {
        private const val MAX_ENTRIES_TO_SCAN = 20_000
        private const val MAX_SCAN_DEPTH = 20
        private const val MAX_LARGEST_FILES = 30
        private const val MAX_HASH_CANDIDATES = 300
        private const val PER_DIRECTORY_TIMEOUT_MS = 5_000L
    }

    suspend fun analyze(): OperationResult<StorageAnalysisResult> =
        withContext(ioDispatcher) {
            safeFileOperation("Depolama analizi başarısız oldu") {
                val allFiles = mutableListOf<File>()
                val emptyFolders = mutableListOf<EmptyFolderEntry>()
                var visitedCount = 0
                var truncated = false

                for (root in storageManager.getCommonDirectories().values.distinctBy { it.absolutePath }) {
                    if (visitedCount >= MAX_ENTRIES_TO_SCAN) {
                        truncated = true
                        break
                    }
                    if (!root.exists() || !root.canRead()) continue

                    // Her klasör kendi zaman aşımı ve gerçek iş parçacığı kesmesiyle (runInterruptible)
                    // taranır — bir klasör (ör. bulut senkronlu sanal galeri) tek bir I/O çağrısında
                    // sonsuza kadar bloke olsa bile, o klasörden vazgeçilip devam edilir.
                    val completed = kotlinx.coroutines.withTimeoutOrNull(PER_DIRECTORY_TIMEOUT_MS) {
                        kotlinx.coroutines.runInterruptible {
                            val iterator = root.walkTopDown()
                                .onEnter { dir -> !dir.name.startsWith(".") }
                                .maxDepth(MAX_SCAN_DEPTH)
                                .iterator()

                            while (iterator.hasNext() && visitedCount < MAX_ENTRIES_TO_SCAN) {
                                val entry = iterator.next()
                                visitedCount++

                                if (entry.isDirectory) {
                                    if (entry.absolutePath != root.absolutePath && entry.listFiles()?.isEmpty() == true) {
                                        emptyFolders += EmptyFolderEntry(entry.absolutePath)
                                    }
                                } else {
                                    allFiles += entry
                                }
                            }
                        }
                    }
                    if (completed == null) truncated = true
                }

                val categorySummaries = buildCategorySummaries(allFiles)
                val largestFiles = allFiles
                    .sortedByDescending { it.length() }
                    .take(MAX_LARGEST_FILES)
                    .map { LargeFileEntry(it.absolutePath, it.name, it.length()) }

                val duplicateGroups = findDuplicates(allFiles)

                StorageAnalysisResult(
                    categorySummaries = categorySummaries,
                    largestFiles = largestFiles,
                    duplicateGroups = duplicateGroups,
                    emptyFolders = emptyFolders,
                    scanTruncated = truncated
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
