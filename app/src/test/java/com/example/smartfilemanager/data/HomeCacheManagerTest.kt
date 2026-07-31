package com.example.smartfilemanager.data

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import com.example.smartfilemanager.model.CategorySummary
import com.example.smartfilemanager.model.FileCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeCacheManagerTest {

    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val cacheManager = HomeCacheManager(context)

    @Test
    fun `loadCache returns null when nothing has been saved yet`() = runBlocking {
        assertNull(cacheManager.loadCache())
    }

    @Test
    fun `saveCache then loadCache round-trips the data`() = runBlocking {
        val summaries = listOf(
            CategorySummary(FileCategory.IMAGE, fileCount = 10, totalSizeBytes = 1000L),
            CategorySummary(FileCategory.DOCUMENT, fileCount = 5, totalSizeBytes = 500L)
        )

        cacheManager.saveCache(totalBytes = 100_000L, usedBytes = 60_000L, freeBytes = 40_000L, categorySummaries = summaries)
        val cached = cacheManager.loadCache()

        assertEquals(100_000L, cached?.totalBytes)
        assertEquals(60_000L, cached?.usedBytes)
        assertEquals(40_000L, cached?.freeBytes)
        assertEquals(10, cached?.categorySummaries?.first { it.category == FileCategory.IMAGE }?.fileCount)
    }

    @Test
    fun `saveCache with zero total bytes is rejected (corrupted-scan guard)`() = runBlocking {
        // Regresyon testi: gercek bir cihazin toplam depolamasi asla 0 olamaz. Bozuk/eksik
        // bir tarama sonucu (0 bayt) hicbir zaman onbellege yazilmamali ve okundugunda da
        // gecersiz sayilmalidir — aksi halde kullaniciya yanlislikla "0 B" gosterilir.
        cacheManager.saveCache(totalBytes = 0L, usedBytes = 0L, freeBytes = 0L, categorySummaries = emptyList())

        assertNull(cacheManager.loadCache())
    }

    @Test
    fun `a valid cache followed by a corrupted save does not silently overwrite with zeros`() = runBlocking {
        val summaries = listOf(CategorySummary(FileCategory.IMAGE, fileCount = 3, totalSizeBytes = 300L))
        cacheManager.saveCache(totalBytes = 50_000L, usedBytes = 20_000L, freeBytes = 30_000L, categorySummaries = summaries)

        // Bozuk bir sonraki tarama sonucu (0 bayt) gelirse onbellek bozulmamali.
        cacheManager.saveCache(totalBytes = 0L, usedBytes = 0L, freeBytes = 0L, categorySummaries = emptyList())

        val cached = cacheManager.loadCache()
        assertEquals(50_000L, cached?.totalBytes)
    }
}
