package com.example.smartfilemanager.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.smartfilemanager.model.CategorySummary
import com.example.smartfilemanager.model.FileCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.homeCacheDataStore by preferencesDataStore(name = "home_cache")

data class CachedHomeSummary(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val categorySummaries: List<CategorySummary>,
    val scannedAtMillis: Long
)

/**
 * Ana sayfadaki depolama taramasının sonucunu kalıcı olarak saklar. Tarama, özellikle
 * çok sayıda dosyası olan cihazlarda dakikalar sürebiliyor (Android'in depolama
 * emülasyon katmanı yüzünden) — bu yüzden her uygulama açılışında yeniden taramak
 * yerine, son sonucu gösterip yalnızca kullanıcı isterse (elle "Yenile") yeniden tarıyoruz.
 */
@Singleton
class HomeCacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val totalBytesKey = longPreferencesKey("total_bytes")
    private val usedBytesKey = longPreferencesKey("used_bytes")
    private val freeBytesKey = longPreferencesKey("free_bytes")
    private val scannedAtKey = longPreferencesKey("scanned_at")
    private fun countKey(category: FileCategory) = longPreferencesKey("count_${category.name}")
    private fun sizeKey(category: FileCategory) = longPreferencesKey("size_${category.name}")

    suspend fun loadCache(): CachedHomeSummary? {
        val prefs = context.homeCacheDataStore.data.first()
        val scannedAt = prefs[scannedAtKey] ?: return null
        val totalBytes = prefs[totalBytesKey] ?: 0L

        // Gerçek bir cihazın toplam depolaması asla 0 olamaz. 0 görüyorsak bu önbellek
        // bozuk/eksik bir taramadan kalmış demektir — geçerli kabul etmeyip yeniden
        // taramayı tetiklememiz gerekir, aksi halde kullanıcıya yanlış "0 B" verisi gösteririz.
        if (totalBytes <= 0L) return null

        val categorySummaries = FileCategory.entries
            .filter { it != FileCategory.FOLDER }
            .map { category ->
                CategorySummary(
                    category = category,
                    fileCount = (prefs[countKey(category)] ?: 0L).toInt(),
                    totalSizeBytes = prefs[sizeKey(category)] ?: 0L
                )
            }

        return CachedHomeSummary(
            totalBytes = totalBytes,
            usedBytes = prefs[usedBytesKey] ?: 0L,
            freeBytes = prefs[freeBytesKey] ?: 0L,
            categorySummaries = categorySummaries,
            scannedAtMillis = scannedAt
        )
    }

    suspend fun saveCache(
        totalBytes: Long,
        usedBytes: Long,
        freeBytes: Long,
        categorySummaries: List<CategorySummary>
    ) {
        if (totalBytes <= 0L) return // Bozuk/eksik bir sonucu asla onbellege yazma
        context.homeCacheDataStore.edit { prefs ->
            prefs[totalBytesKey] = totalBytes
            prefs[usedBytesKey] = usedBytes
            prefs[freeBytesKey] = freeBytes
            prefs[scannedAtKey] = System.currentTimeMillis()
            categorySummaries.forEach { summary ->
                prefs[countKey(summary.category)] = summary.fileCount.toLong()
                prefs[sizeKey(summary.category)] = summary.totalSizeBytes
            }
        }
    }
}
