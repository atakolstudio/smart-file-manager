package com.example.smartfilemanager.data

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class CleanupManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val recycleBinManager = RecycleBinManager(context, Dispatchers.IO)
    private val cleanupManager = CleanupManager(context, recycleBinManager, Dispatchers.IO)

    @Test
    fun `deleteEmptyFolders removes only folders that are actually empty`() = runBlocking {
        val root = tempFolder.newFolder("root")
        val emptyDir = File(root, "empty").apply { mkdir() }
        val nonEmptyDir = File(root, "nonEmpty").apply { mkdir() }
        File(nonEmptyDir, "file.txt").createNewFile()

        val result = cleanupManager.deleteEmptyFolders(
            listOf(
                com.example.smartfilemanager.model.EmptyFolderEntry(emptyDir.absolutePath),
                com.example.smartfilemanager.model.EmptyFolderEntry(nonEmptyDir.absolutePath)
            )
        )

        assertTrue(result is OperationResult.Success)
        assertEquals(1, (result as OperationResult.Success).data)
        assertFalse(emptyDir.exists())
        assertTrue(nonEmptyDir.exists())
    }

    @Test
    fun `removeDuplicates keeps the first file of each group and trashes the rest`() = runBlocking {
        val root = tempFolder.newFolder("root")
        val original = File(root, "original.jpg").apply { writeText("same content") }
        val duplicate1 = File(root, "copy1.jpg").apply { writeText("same content") }
        val duplicate2 = File(root, "copy2.jpg").apply { writeText("same content") }

        val group = com.example.smartfilemanager.model.DuplicateGroup(
            sizeBytes = original.length(),
            paths = listOf(original.absolutePath, duplicate1.absolutePath, duplicate2.absolutePath)
        )

        val result = cleanupManager.removeDuplicates(listOf(group))

        assertTrue(result is OperationResult.Success)
        assertEquals(2, (result as OperationResult.Success).data)
        assertTrue(original.exists())
        assertFalse(duplicate1.exists())
        assertFalse(duplicate2.exists())
    }

    @Test
    fun `clearAppCache deletes files in cache directories and reports freed bytes`() = runBlocking {
        val cacheFile = File(context.cacheDir, "temp.cache").apply {
            parentFile?.mkdirs()
            writeBytes(ByteArray(100))
        }

        val result = cleanupManager.clearAppCache()

        assertTrue(result is OperationResult.Success)
        assertTrue((result as OperationResult.Success).data >= 100L)
        assertFalse(cacheFile.exists())
    }
}
