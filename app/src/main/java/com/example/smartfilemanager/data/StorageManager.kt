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
        /** Her klasör için azami tarama süresi — bir klasör (ör. bulut senkronu olan sanal bir
         * galeri klasörü) tek bir I/O çağrısında donarsa bile, o klasörden vazgeçilip devam edilir. */
        private const val PER_DIRECTORY_TIMEOUT_MS = 5_000L
    }

    /**
     * Yaygın kullanıcı klasörlerini (DCIM, Pictures, Movies, Music, Documents, Download)
     * tarayıp her kategori için dosya sayısı ve toplam boyutu hesaplar.
     * Çok büyük depolamalarda maliyeti sınırlamak için tarama tek seferlik ve arka planda yapılır.
     */
    suspend fun getCategorySummaries(onProgress: (String) -> Unit = {}): OperationResult<List<CategorySummary>> =
        withContext(ioDispatcher) {
            safeFileOperation("Depolama taranamadı") {
                val counters = mutableMapOf<FileCategory, Pair<Int, Long>>()
                var visitedCount = 0

                val seenPaths = mutableSetOf<String>()
                for ((label, directory) in getCommonDirectories()) {
                    if (!seenPaths.add(directory.absolutePath)) continue
                    if (visitedCount >= MAX_ENTRIES_PER_SCAN) break
                    if (!directory.exists() || !directory.canRead()) continue

                    onProgress(label)

                    // ÖNEMLİ: Sadece sayıya dayalı bir sınır yeterli değil — eğer bir klasörün
                    // (ör. bulut senkronlu sanal bir galeri klasörünün) İÇİNDEKİ TEK BİR I/O
                    // çağrısı senkron olarak sonsuza kadar bloke olursa, döngü sayaca hiç
                    // ulaşamadan durur. Bu yüzden her klasörü kendi zaman aşımı içinde,
                    // runInterruptible ile çalıştırıyoruz: zaman aşımında arka plan iş parçacığına
                    // gerçek bir kesme (Thread.interrupt) sinyali gönderilir ve bu klasörden
                    // vazgeçilip bir sonrakine geçilir.
                    kotlinx.coroutines.withTimeoutOrNull(PER_DIRECTORY_TIMEOUT_MS) {
                        kotlinx.coroutines.runInterruptible {
                            val iterator = directory.walkTopDown()
                                .onEnter { dir -> !dir.name.startsWith(".") }
                                .maxDepth(MAX_SCAN_DEPTH)
                                .iterator()

                            while (iterator.hasNext() && visitedCount < MAX_ENTRIES_PER_SCAN) {
                                val entry = iterator.next()
                                visitedCount++
                                if (!entry.isFile) continue

                                val category = MimeTypeHelper.getCategory(entry.name, isDirectory = false)
                                val current = counters[category] ?: (0 to 0L)
                                counters[category] = (current.first + 1) to (current.second + entry.length())
                            }
                        }
                    }
                    // withTimeoutOrNull null dönse (zaman aşımı) bile buraya devam ediyoruz —
                    // bu klasör atlanıp bir sonrakine geçilir, tüm tarama asla kilitlenmez.
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
