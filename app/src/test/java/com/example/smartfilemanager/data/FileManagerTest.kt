package com.example.smartfilemanager.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val fileManager = FileManager(Dispatchers.IO)

    @Test
    fun `listFiles returns directories before files, both alphabetically`() = runBlocking {
        val root = tempFolder.newFolder("root")
        File(root, "banana.txt").createNewFile()
        File(root, "apple.txt").createNewFile()
        File(root, "zeta_folder").mkdir()
        File(root, "alpha_folder").mkdir()

        val result = fileManager.listFiles(root.absolutePath)

        assertTrue(result is OperationResult.Success)
        val names = (result as OperationResult.Success).data.map { it.name }
        assertEquals(listOf("alpha_folder", "zeta_folder", "apple.txt", "banana.txt"), names)
    }

    @Test
    fun `listFiles on a nonexistent directory returns empty list, not an error`() = runBlocking {
        val missing = File(tempFolder.root, "does_not_exist")
        val result = fileManager.listFiles(missing.absolutePath)

        assertTrue(result is OperationResult.Success)
        assertTrue((result as OperationResult.Success).data.isEmpty())
    }

    @Test
    fun `createFolder creates a new directory`() = runBlocking {
        val root = tempFolder.newFolder("root")
        val result = fileManager.createFolder(root.absolutePath, "NewFolder")

        assertTrue(result is OperationResult.Success)
        assertTrue(File(root, "NewFolder").isDirectory)
    }

    @Test
    fun `createFolder fails when a folder with the same name already exists`() = runBlocking {
        val root = tempFolder.newFolder("root")
        File(root, "Existing").mkdir()

        val result = fileManager.createFolder(root.absolutePath, "Existing")

        assertTrue(result is OperationResult.Error)
    }

    @Test
    fun `createFile creates an empty file`() = runBlocking {
        val root = tempFolder.newFolder("root")
        val result = fileManager.createFile(root.absolutePath, "note.txt")

        assertTrue(result is OperationResult.Success)
        assertTrue(File(root, "note.txt").isFile)
    }

    @Test
    fun `rename changes the file name and preserves content`() = runBlocking {
        val root = tempFolder.newFolder("root")
        val original = File(root, "old.txt").apply { writeText("content") }

        val result = fileManager.rename(original.absolutePath, "new.txt")

        assertTrue(result is OperationResult.Success)
        assertFalse(original.exists())
        val renamed = File(root, "new.txt")
        assertTrue(renamed.exists())
        assertEquals("content", renamed.readText())
    }

    @Test
    fun `rename fails when target name already exists`() = runBlocking {
        val root = tempFolder.newFolder("root")
        val source = File(root, "source.txt").apply { createNewFile() }
        File(root, "target.txt").createNewFile()

        val result = fileManager.rename(source.absolutePath, "target.txt")

        assertTrue(result is OperationResult.Error)
    }

    @Test
    fun `delete removes a file`() = runBlocking {
        val root = tempFolder.newFolder("root")
        val file = File(root, "toDelete.txt").apply { createNewFile() }

        val result = fileManager.delete(file.absolutePath)

        assertTrue(result is OperationResult.Success)
        assertFalse(file.exists())
    }

    @Test
    fun `delete removes a directory recursively`() = runBlocking {
        val root = tempFolder.newFolder("root")
        val dir = File(root, "toDelete").apply { mkdir() }
        File(dir, "inner.txt").createNewFile()

        val result = fileManager.delete(dir.absolutePath)

        assertTrue(result is OperationResult.Success)
        assertFalse(dir.exists())
    }

    @Test
    fun `copy duplicates a file into the destination directory`() = runBlocking {
        val root = tempFolder.newFolder("root")
        val destination = tempFolder.newFolder("destination")
        val source = File(root, "source.txt").apply { writeText("hello") }

        val result = fileManager.copy(source.absolutePath, destination.absolutePath)

        assertTrue(result is OperationResult.Success)
        assertTrue(source.exists())
        val copied = File(destination, "source.txt")
        assertTrue(copied.exists())
        assertEquals("hello", copied.readText())
    }

    @Test
    fun `move relocates a file and removes the source`() = runBlocking {
        val root = tempFolder.newFolder("root")
        val destination = tempFolder.newFolder("destination")
        val source = File(root, "source.txt").apply { writeText("hello") }

        val result = fileManager.move(source.absolutePath, destination.absolutePath)

        assertTrue(result is OperationResult.Success)
        assertFalse(source.exists())
        val moved = File(destination, "source.txt")
        assertTrue(moved.exists())
        assertEquals("hello", moved.readText())
    }

    @Test
    fun `calculateFolderSize sums all nested file sizes`() = runBlocking {
        val root = tempFolder.newFolder("root")
        File(root, "a.txt").writeText("12345") // 5 bytes
        val subDir = File(root, "sub").apply { mkdir() }
        File(subDir, "b.txt").writeText("1234567890") // 10 bytes

        val result = fileManager.calculateFolderSize(root.absolutePath)

        assertTrue(result is OperationResult.Success)
        assertEquals(15L, (result as OperationResult.Success).data)
    }

    @Test
    fun `readTextPreview returns file content`() = runBlocking {
        val root = tempFolder.newFolder("root")
        val file = File(root, "notes.txt").apply { writeText("Merhaba dunya") }

        val result = fileManager.readTextPreview(file.absolutePath)

        assertTrue(result is OperationResult.Success)
        assertEquals("Merhaba dunya", (result as OperationResult.Success).data)
    }

    @Test
    fun `readTextPreview truncates content beyond maxChars`() = runBlocking {
        val root = tempFolder.newFolder("root")
        val file = File(root, "big.txt").apply { writeText("a".repeat(1000)) }

        val result = fileManager.readTextPreview(file.absolutePath, maxChars = 100)

        assertTrue(result is OperationResult.Success)
        assertEquals(100, (result as OperationResult.Success).data.length)
    }
}
