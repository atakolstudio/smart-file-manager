package com.example.smartfilemanager.data

import androidx.test.core.app.ApplicationProvider
import com.example.smartfilemanager.model.FileCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class StorageManagerTest {

    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val storageManager = StorageManager(context, Dispatchers.IO)

    @Test
    fun `getCommonDirectories exposes all expected quick-access folders`() {
        val directories = storageManager.getCommonDirectories()
        assertEquals(
            setOf("DCIM", "Pictures", "Movies", "Music", "Documents", "Download"),
            directories.keys
        )
        directories.values.forEach { dir ->
            assertTrue("Expected a non-null path for $dir", dir.path.isNotBlank())
        }
    }

    @Test
    fun `getTotalAndFreeBytes never throws and returns non-negative values`() {
        val (total, free) = storageManager.getTotalAndFreeBytes()
        assertTrue(total >= 0L)
        assertTrue(free >= 0L)
    }

    @Test
    fun `getCategorySummaries counts files placed in common directories by category`() = runBlocking {
        val directories = storageManager.getCommonDirectories()
        val picturesDir = directories.getValue("Pictures").apply { mkdirs() }
        val musicDir = directories.getValue("Music").apply { mkdirs() }

        File(picturesDir, "photo1.jpg").writeBytes(ByteArray(10))
        File(picturesDir, "photo2.png").writeBytes(ByteArray(20))
        File(musicDir, "song.mp3").writeBytes(ByteArray(30))

        val result = storageManager.getCategorySummaries()

        assertTrue(result is OperationResult.Success)
        val summaries = (result as OperationResult.Success).data

        val imageSummary = summaries.first { it.category == FileCategory.IMAGE }
        assertEquals(2, imageSummary.fileCount)
        assertEquals(30L, imageSummary.totalSizeBytes)

        val audioSummary = summaries.first { it.category == FileCategory.AUDIO }
        assertEquals(1, audioSummary.fileCount)
        assertEquals(30L, audioSummary.totalSizeBytes)
    }

    @Test
    fun `getInstalledUserAppsCount succeeds without throwing`() = runBlocking {
        val result = storageManager.getInstalledUserAppsCount()
        assertTrue(result is OperationResult.Success)
        assertTrue((result as OperationResult.Success).data >= 0)
    }
}
