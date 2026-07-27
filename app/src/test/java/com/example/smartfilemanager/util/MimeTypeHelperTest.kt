package com.example.smartfilemanager.util

import com.example.smartfilemanager.model.FileCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class MimeTypeHelperTest {

    @Test
    fun `directories are categorized as FOLDER regardless of name`() {
        assertEquals(FileCategory.FOLDER, MimeTypeHelper.getCategory("Photos", isDirectory = true))
        assertEquals(FileCategory.FOLDER, MimeTypeHelper.getCategory("archive.zip", isDirectory = true))
    }

    @Test
    fun `image extensions are categorized as IMAGE`() {
        listOf("photo.jpg", "photo.jpeg", "photo.PNG", "anim.gif", "pic.webp", "scan.heic").forEach { name ->
            assertEquals("Failed for $name", FileCategory.IMAGE, MimeTypeHelper.getCategory(name, isDirectory = false))
        }
    }

    @Test
    fun `video extensions are categorized as VIDEO`() {
        listOf("movie.mp4", "clip.MKV", "video.avi", "home.mov").forEach { name ->
            assertEquals("Failed for $name", FileCategory.VIDEO, MimeTypeHelper.getCategory(name, isDirectory = false))
        }
    }

    @Test
    fun `audio extensions are categorized as AUDIO`() {
        listOf("song.mp3", "track.WAV", "beat.flac").forEach { name ->
            assertEquals("Failed for $name", FileCategory.AUDIO, MimeTypeHelper.getCategory(name, isDirectory = false))
        }
    }

    @Test
    fun `document extensions are categorized as DOCUMENT`() {
        listOf("report.pdf", "notes.txt", "sheet.XLSX", "data.json").forEach { name ->
            assertEquals("Failed for $name", FileCategory.DOCUMENT, MimeTypeHelper.getCategory(name, isDirectory = false))
        }
    }

    @Test
    fun `archive extensions are categorized as ARCHIVE`() {
        listOf("backup.zip", "files.RAR", "data.7z").forEach { name ->
            assertEquals("Failed for $name", FileCategory.ARCHIVE, MimeTypeHelper.getCategory(name, isDirectory = false))
        }
    }

    @Test
    fun `apk extensions are categorized as APK`() {
        assertEquals(FileCategory.APK, MimeTypeHelper.getCategory("app.apk", isDirectory = false))
    }

    @Test
    fun `unknown extensions are categorized as OTHER`() {
        assertEquals(FileCategory.OTHER, MimeTypeHelper.getCategory("mystery.xyz123", isDirectory = false))
    }

    @Test
    fun `files without extension are categorized as OTHER`() {
        assertEquals(FileCategory.OTHER, MimeTypeHelper.getCategory("README", isDirectory = false))
    }
}
