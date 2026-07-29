package com.example.smartfilemanager.data

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * NOT: Robolectric ortamında DataStore'un dosya sistemi durumu test sınıfı çalıştırmaları
 * arasında (bazı yapılandırmalarda) kalıcı olabiliyor. Bu yüzden her testten önce
 * [RecycleBinManager.emptyBin] ile durumu sıfırlıyoruz — testler birbirinden tamamen
 * bağımsız (izole) olmalı, önceki bir testin bıraktığı duruma güvenmemeli.
 */
@RunWith(RobolectricTestRunner::class)
class RecycleBinManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val recycleBinManager = RecycleBinManager(context, Dispatchers.IO)

    @Before
    fun clearState() {
        runBlocking {
            recycleBinManager.emptyBin()
        }
    }

    @Test
    fun `moveToTrash removes the source file and records a trash entry`() = runBlocking {
        val source = File(tempFolder.newFolder("source"), "note.txt").apply { writeText("hello") }

        val result = recycleBinManager.moveToTrash(source.absolutePath)

        assertTrue(result is OperationResult.Success)
        assertFalse(source.exists())

        val entries = recycleBinManager.entries.first()
        assertEquals(1, entries.size)
        assertEquals("note.txt", entries.first().name)
        assertTrue(File(entries.first().trashPath).exists())
    }

    @Test
    fun `restore puts the file back at its original path with original content`() = runBlocking {
        val sourceDir = tempFolder.newFolder("source")
        val source = File(sourceDir, "note.txt").apply { writeText("hello world") }

        recycleBinManager.moveToTrash(source.absolutePath)
        val entry = recycleBinManager.entries.first().first()

        val restoreResult = recycleBinManager.restore(entry)

        assertTrue(restoreResult is OperationResult.Success)
        assertTrue(source.exists())
        assertEquals("hello world", source.readText())
        assertTrue(recycleBinManager.entries.first().isEmpty())
    }

    @Test
    fun `deleteForever removes the trashed file and its entry permanently`() = runBlocking {
        val source = File(tempFolder.newFolder("source"), "note.txt").apply { writeText("hello") }
        recycleBinManager.moveToTrash(source.absolutePath)
        val entry = recycleBinManager.entries.first().first()

        val result = recycleBinManager.deleteForever(entry)

        assertTrue(result is OperationResult.Success)
        assertFalse(File(entry.trashPath).exists())
        assertTrue(recycleBinManager.entries.first().isEmpty())
    }

    @Test
    fun `emptyBin removes all trashed files and entries`() = runBlocking {
        val sourceDir = tempFolder.newFolder("source")
        val fileA = File(sourceDir, "a.txt").apply { writeText("a") }
        val fileB = File(sourceDir, "b.txt").apply { writeText("b") }
        recycleBinManager.moveToTrash(fileA.absolutePath)
        recycleBinManager.moveToTrash(fileB.absolutePath)

        val entriesBeforeEmpty = recycleBinManager.entries.first()
        assertEquals(2, entriesBeforeEmpty.size)

        val result = recycleBinManager.emptyBin()

        assertTrue(result is OperationResult.Success)
        assertTrue(recycleBinManager.entries.first().isEmpty())
        entriesBeforeEmpty.forEach { entry ->
            assertFalse(File(entry.trashPath).exists())
        }
    }

    @Test
    fun `restore fails when an item already exists at the original path`() = runBlocking {
        val sourceDir = tempFolder.newFolder("source")
        val source = File(sourceDir, "note.txt").apply { writeText("original") }
        recycleBinManager.moveToTrash(source.absolutePath)
        val entry = recycleBinManager.entries.first().first()

        // Aynı yola başka bir dosya yeniden oluşturulursa geri yükleme çakışmalı.
        File(entry.originalPath).writeText("conflicting file")

        val result = recycleBinManager.restore(entry)

        assertTrue(result is OperationResult.Error)
    }
}
