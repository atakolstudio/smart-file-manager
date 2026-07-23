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

    /**
     * Yaygın kullanıcı klasörlerini (DCIM, Pictures, Movies, Music, Documents, Download)
     * tarayıp her kategori için dosya sayısı ve toplam boyutu hesaplar.
     * Çok büyük depolamalarda maliyeti sınırlamak için tarama tek seferlik ve arka planda yapılır.
     */
    suspend fun getCategorySummaries(): OperationResult<List<CategorySummary>> =
        withContext(ioDispatcher) {
            safeFileOperation("Depolama taranamadı") {
                val root = Environment.getExternalStorageDirectory()
                val counters = mutableMapOf<FileCategory, Pair<Int, Long>>()

                if (root.exists() && root.canRead()) {
                    root.walkTopDown()
                        .onEnter { dir -> !dir.name.startsWith(".") }
                        .filter { it.isFile }
                        .forEach { file ->
                            val category = MimeTypeHelper.getCategory(file.name, isDirectory = false)
                            val current = counters[category] ?: (0 to 0L)
                            counters[category] = (current.first + 1) to (current.second + file.length())
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
