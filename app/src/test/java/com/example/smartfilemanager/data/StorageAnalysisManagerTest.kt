package com.example.smartfilemanager.data

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class StorageAnalysisManagerTest {

    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val storageManager = StorageManager(context, Dispatchers.IO)
    private val analysisManager = StorageAnalysisManager(storageManager, Dispatchers.IO)

    @Test
    fun `analyze finds duplicate files by identical content`() = runBlocking {
        val picturesDir = storageManager.getCommonDirectories().getValue("Pictures").apply { mkdirs() }
        File(picturesDir, "original.jpg").writeText("identical bytes")
        File(picturesDir, "copy.jpg").writeText("identical bytes")
        File(picturesDir, "unique.jpg").writeText("something else entirely")

        val result = analysisManager.analyze()

        assertTrue(result is OperationResult.Success)
        val data = (result as OperationResult.Success).data
        assertEquals(1, data.duplicateGroups.size)
        assertEquals(2, data.duplicateGroups.first().paths.size)
    }

    @Test
    fun `analyze detects empty folders`() = runBlocking {
        val picturesDir = storageManager.getCommonDirectories().getValue("Pictures").apply { mkdirs() }
        val emptySubfolder = File(picturesDir, "empty_album").apply { mkdirs() }

        val result = analysisManager.analyze()

        assertTrue(result is OperationResult.Success)
        val data = (result as OperationResult.Success).data
        assertTrue(data.emptyFolders.any { it.path == emptySubfolder.absolutePath })
    }

    @Test
    fun `analyze reports the largest files first`() = runBlocking {
        val documentsDir = storageManager.getCommonDirectories().getValue("Documents").apply { mkdirs() }
        File(documentsDir, "small.txt").writeBytes(ByteArray(10))
        File(documentsDir, "big.txt").writeBytes(ByteArray(1000))

        val result = analysisManager.analyze()

        assertTrue(result is OperationResult.Success)
        val largest = (result as OperationResult.Success).data.largestFiles
        assertTrue(largest.isNotEmpty())
        assertEquals("big.txt", largest.first().name)
    }

    @Test(timeout = 15_000)
    fun `analyze terminates even when a symlink cycle exists (regression test)`() = runBlocking {
        val documentsDir = storageManager.getCommonDirectories().getValue("Documents").apply { mkdirs() }
        val loopDir = File(documentsDir, "loop_test_dir").apply { mkdirs() }

        try {
            java.nio.file.Files.createSymbolicLink(
                File(loopDir, "back_to_self").toPath(),
                loopDir.toPath()
            )
        } catch (e: java.io.IOException) {
            org.junit.Assume.assumeNoException(e)
        }

        val result = analysisManager.analyze()

        assertTrue(result is OperationResult.Success)
    }
}
