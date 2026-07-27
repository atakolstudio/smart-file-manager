package com.example.smartfilemanager.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.security.MessageDigest
import java.util.zip.CRC32

class HashCalculatorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun fileWithContent(content: String): File {
        val file = tempFolder.newFile()
        file.writeText(content)
        return file
    }

    @Test
    fun `md5 matches reference MessageDigest computation`() {
        val file = fileWithContent("hello world")
        val expected = MessageDigest.getInstance("MD5").digest(file.readBytes())
            .joinToString("") { "%02x".format(it) }
        assertEquals(expected, HashCalculator.calculate(file, HashAlgorithm.MD5))
    }

    @Test
    fun `sha1 matches reference MessageDigest computation`() {
        val file = fileWithContent("hello world")
        val expected = MessageDigest.getInstance("SHA-1").digest(file.readBytes())
            .joinToString("") { "%02x".format(it) }
        assertEquals(expected, HashCalculator.calculate(file, HashAlgorithm.SHA1))
    }

    @Test
    fun `sha256 matches reference MessageDigest computation`() {
        val file = fileWithContent("hello world")
        val expected = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
            .joinToString("") { "%02x".format(it) }
        assertEquals(expected, HashCalculator.calculate(file, HashAlgorithm.SHA256))
    }

    @Test
    fun `crc32 matches reference CRC32 computation`() {
        val file = fileWithContent("hello world")
        val crc = CRC32()
        crc.update(file.readBytes())
        assertEquals(crc.value.toString(16), HashCalculator.calculate(file, HashAlgorithm.CRC32))
    }

    @Test
    fun `identical content produces identical hash`() {
        val fileA = fileWithContent("identical content")
        val fileB = fileWithContent("identical content")
        assertEquals(
            HashCalculator.calculate(fileA, HashAlgorithm.SHA256),
            HashCalculator.calculate(fileB, HashAlgorithm.SHA256)
        )
    }

    @Test
    fun `different content produces different hash`() {
        val fileA = fileWithContent("content A")
        val fileB = fileWithContent("content B")
        assertNotEquals(
            HashCalculator.calculate(fileA, HashAlgorithm.SHA256),
            HashCalculator.calculate(fileB, HashAlgorithm.SHA256)
        )
    }

    @Test
    fun `hash computation works across a buffer-size boundary (large file)`() {
        // DEFAULT_BUFFER_SIZE is 8192 internally; verify content larger than one buffer works.
        val largeContent = "x".repeat(50_000)
        val file = fileWithContent(largeContent)
        val expected = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
            .joinToString("") { "%02x".format(it) }
        assertEquals(expected, HashCalculator.calculate(file, HashAlgorithm.SHA256))
    }
}
