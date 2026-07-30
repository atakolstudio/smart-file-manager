package com.example.smartfilemanager.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Environment
import com.example.smartfilemanager.di.IoDispatcher
import com.example.smartfilemanager.model.CategorySummary
import com.example.smartfilemanager.model.FileCategory
import com.example.smartfilemanager.util.MimeTypeHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cihazın depolama alanını tarayıp kategori bazlı özet (dosya sayısı + toplam boyut)
 * çıkaran sınıf. Ana sayfadaki "Hızlı Erişim" kartlarını besler.
 */
@Singleton
class StorageManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    companion object {
        /** Tek bir taramada ziyaret edilecek azami düğüm (dosya+klasör) sayısı — bir sembolik link
         * döngüsü gibi patolojik bir durumda bile tarama bu sayıda kesin olarak durur. */
        private const val MAX_ENTRIES_PER_SCAN = 20_000
        /** Ek güvenlik: klasör derinliğini de sınırla (sembolik link döngülerine karşı ikinci bir bariyer). */
        private const val MAX_SCAN_DEPTH = 20
    }

    /**
     * Yaygın kullanıcı klasörlerini (DCIM, Pictures, Movies, Music, Documents, Download)
     * tarayıp her kategori için dosya sayısı ve toplam boyutu hesaplar.
     * Çok büyük depolamalarda maliyeti sınırlamak için tarama tek seferlik ve arka planda yapılır.
     */
    suspend fun getCategorySummaries(): OperationResult<List<CategorySummary>> =
        withContext(ioDispatcher) {
            safeFileOperation("Depolama taranamadı") {
                val counters = mutableMapOf<FileCategory, Pair<Int, Long>>()
                var visitedCount = 0

                // withTimeoutOrNull, senkron/CPU-bağımlı sıkı döngüleri kesemez (cooperative
                // cancellation yalnızca suspend noktalarında çalışır). Bu yüzden burada gerçek
                // bir garanti için SAYIYA dayalı sabit bir üst sınır kullanıyoruz. ÖNEMLİ: sayaç
                // her ziyaret edilen düğümde (klasör dahil) artmalı — yalnızca dosyalarda artarsa,
                // bir sembolik link döngüsü sonsuz sayıda KLASÖR üretip sayacı hiç tetiklemeden
                // döngüyü sonsuza kadar sürdürebilir. maxDepth ikinci bir bağımsız güvenlik katmanı.
                outer@ for (directory in getCommonDirectories().values.distinctBy { it.absolutePath }) {
                    if (!directory.exists() || !directory.canRead()) continue

                    val iterator = directory.walkTopDown()
                        .onEnter { dir -> !dir.name.startsWith(".") }
                        .maxDepth(MAX_SCAN_DEPTH)
                        .iterator()

                    while (iterator.hasNext()) {
                        val entry = iterator.next()

                        visitedCount++
                        if (visitedCount >= MAX_ENTRIES_PER_SCAN) break@outer

                        if (!entry.isFile) continue

                        val category = MimeTypeHelper.getCategory(entry.name, isDirectory = false)
                        val current = counters[category] ?: (0 to 0L)
                        counters[category] = (current.first + 1) to (current.second + entry.length())
                    }
                }

                FileCategory.entries
                    .filter { it != FileCategory.FOLDER }
                    .map { category ->
                        val (count, size) = counters[category] ?: (0 to 0L)
                        CategorySummary(category, count, size)
                    }
            }
        }

    /**
     * Kullanıcı tarafından yüklenmiş (sistem uygulaması olmayan) uygulama sayısını döner.
     */
    suspend fun getInstalledUserAppsCount(): OperationResult<Int> =
        withContext(ioDispatcher) {
            safeFileOperation("Yüklü uygulamalar okunamadı") {
                context.packageManager.getInstalledApplications(0)
                    .count { appInfo -> (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            }
        }

    fun getTotalAndFreeBytes(): Pair<Long, Long> {
        val statFs = android.os.StatFs(Environment.getExternalStorageDirectory().path)
        return statFs.totalBytes to statFs.availableBytes
    }

    fun getCommonDirectories(): Map<String, File> {
        return mapOf(
            "DCIM" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            "Pictures" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "Movies" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "Music" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            "Documents" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "Download" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        )
    }
}
