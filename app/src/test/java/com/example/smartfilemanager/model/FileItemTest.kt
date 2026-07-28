package com.example.smartfilemanager.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileItemTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `extension returns the substring after the last dot`() {
        val item = FileItem(
            name = "archive.tar.gz",
            path = "/x/archive.tar.gz",
            isDirectory = false,
            sizeBytes = 0,
            lastModified = 0,
            mimeType = null
        )
        assertEquals("gz", item.extension)
    }

    @Test
    fun `extension is empty for a directory regardless of dots in the name`() {
        val item = FileItem(
            name = "com.example.app",
            path = "/x/com.example.app",
            isDirectory = true,
            sizeBytes = 0,
            lastModified = 0,
            mimeType = null
        )
        assertEquals("", item.extension)
    }

    @Test
    fun `extension is empty when the file has no dot`() {
        val item = FileItem(
            name = "README",
            path = "/x/README",
            isDirectory = false,
            sizeBytes = 0,
            lastModified = 0,
            mimeType = null
        )
        assertEquals("", item.extension)
    }

    @Test
    fun `fromFile maps a real directory correctly`() {
        val dir = tempFolder.newFolder("myFolder")
        val item = FileItem.fromFile(dir)

        assertEquals("myFolder", item.name)
        assertTrue(item.isDirectory)
        assertEquals(0L, item.sizeBytes)
    }

    @Test
    fun `fromFile maps a real file with correct size`() {
        val file = File(tempFolder.newFolder("root"), "data.bin")
        file.writeBytes(ByteArray(42))
        val item = FileItem.fromFile(file)

        assertEquals("data.bin", item.name)
        assertFalse(item.isDirectory)
        assertEquals(42L, item.sizeBytes)
    }
}
