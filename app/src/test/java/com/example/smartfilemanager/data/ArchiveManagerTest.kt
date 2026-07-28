package com.example.smartfilemanager.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ArchiveManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val archiveManager = ArchiveManager(Dispatchers.IO)

    @Test
    fun `compress then extract restores original file content`() = runBlocking {
        val sourceDir = tempFolder.newFolder("source")
        val fileA = File(sourceDir, "a.txt").apply { writeText("content A") }
        val subDir = File(sourceDir, "sub").apply { mkdir() }
        val fileB = File(subDir, "b.txt").apply { writeText("content B") }

        val zipPath = File(tempFolder.root, "archive.zip").absolutePath
        val compressResult = archiveManager.compress(listOf(fileA.absolutePath, subDir.absolutePath), zipPath)
        assertTrue(compressResult is OperationResult.Success)
        assertTrue(File(zipPath).exists())

        val destinationDir = File(tempFolder.root, "extracted").absolutePath
        val extractResult = archiveManager.extract(zipPath, destinationDir)
        assertTrue(extractResult is OperationResult.Success)

        val extractedA = File(destinationDir, "a.txt")
        assertTrue(extractedA.exists())
        assertEquals("content A", extractedA.readText())

        val extractedB = File(destinationDir, "sub/b.txt")
        assertTrue(extractedB.exists())
        assertEquals("content B", extractedB.readText())
    }

    @Test
    fun `compress an empty folder still creates a directory entry`() = runBlocking {
        val sourceDir = tempFolder.newFolder("source")
        val emptyDir = File(sourceDir, "empty").apply { mkdir() }

        val zipPath = File(tempFolder.root, "archive.zip").absolutePath
        val compressResult = archiveManager.compress(listOf(emptyDir.absolutePath), zipPath)
        assertTrue(compressResult is OperationResult.Success)

        val destinationDir = File(tempFolder.root, "extracted").absolutePath
        archiveManager.extract(zipPath, destinationDir)

        assertTrue(File(destinationDir, "empty").isDirectory)
    }

    @Test
    fun `extract rejects a zip entry that attempts to escape the destination directory (Zip Slip)`() = runBlocking {
        // Manuel olarak, dizin dışına çıkmaya çalışan kötü niyetli bir isimle zip oluştur.
        val maliciousZip = File(tempFolder.root, "malicious.zip")
        ZipOutputStream(maliciousZip.outputStream()).use { zos ->
            zos.putNextEntry(ZipEntry("../../evil.txt"))
            zos.write("pwned".toByteArray())
            zos.closeEntry()
        }

        val destinationDir = File(tempFolder.root, "safe_extract").absolutePath
        val result = archiveManager.extract(maliciousZip.absolutePath, destinationDir)

        assertTrue(result is OperationResult.Error)
        // Hedef dizinin dışına hiçbir dosya yazılmamış olmalı.
        assertTrue(!File(tempFolder.root, "evil.txt").exists())
    }
}
