package com.example.smartfilemanager.util

import java.io.File
import java.security.MessageDigest
import java.util.zip.CRC32

enum class HashAlgorithm(val label: String) {
    MD5("MD5"),
    SHA1("SHA-1"),
    SHA256("SHA-256"),
    CRC32("CRC32")
}

/**
 * Dosyaların bütünlük doğrulaması için MD5, SHA1, SHA256 ve CRC32 özetlerini hesaplar.
 * Büyük dosyalarda belleği korumak için akış tamponlu (buffered) okuma kullanılır.
 */
object HashCalculator {

    fun calculate(file: File, algorithm: HashAlgorithm): String {
        return if (algorithm == HashAlgorithm.CRC32) {
            calculateCrc32(file)
        } else {
            calculateDigest(file, algorithm.label)
        }
    }

    private fun calculateDigest(file: File, algorithmName: String): String {
        val digest = MessageDigest.getInstance(algorithmName)
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun calculateCrc32(file: File): String {
        val crc = CRC32()
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                crc.update(buffer, 0, read)
            }
        }
        return crc.value.toString(16)
    }

    private const val DEFAULT_BUFFER_SIZE = 8192
}
