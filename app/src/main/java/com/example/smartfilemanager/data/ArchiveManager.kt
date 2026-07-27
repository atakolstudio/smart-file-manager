package com.example.smartfilemanager.data

import com.example.smartfilemanager.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seçili dosya/klasörleri ZIP olarak sıkıştırır ve ZIP arşivlerini açar.
 * java.util.zip kullanır; ek bir kütüphane gerektirmez.
 */
@Singleton
class ArchiveManager @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    suspend fun compress(sourcePaths: List<String>, destinationZipPath: String): OperationResult<File> =
        withContext(ioDispatcher) {
            safeFileOperation("Sıkıştırılamadı") {
                val zipFile = File(destinationZipPath)
                ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                    sourcePaths.forEach { path ->
                        val file = File(path)
                        addToZip(file, file.name, zos)
                    }
                }
                zipFile
            }
        }

    private fun addToZip(file: File, entryName: String, zos: ZipOutputStream) {
        if (file.isDirectory) {
            val children = file.listFiles()
            if (children.isNullOrEmpty()) {
                zos.putNextEntry(ZipEntry("$entryName/"))
                zos.closeEntry()
            } else {
                children.forEach { child -> addToZip(child, "$entryName/${child.name}", zos) }
            }
        } else {
            zos.putNextEntry(ZipEntry(entryName))
            file.inputStream().use { it.copyTo(zos) }
            zos.closeEntry()
        }
    }

    suspend fun extract(zipPath: String, destinationDirPath: String): OperationResult<File> =
        withContext(ioDispatcher) {
            safeFileOperation("Arşiv çıkartılamadı") {
                val destinationDir = File(destinationDirPath).apply { mkdirs() }
                ZipInputStream(BufferedInputStream(FileInputStream(zipPath))).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val outFile = File(destinationDir, entry.name)

                        // Zip Slip saldırısına karşı: çıkış dosyasının hedef klasör dışına taşmadığından emin ol.
                        if (!outFile.canonicalPath.startsWith(destinationDir.canonicalPath + File.separator) &&
                            outFile.canonicalPath != destinationDir.canonicalPath
                        ) {
                            throw SecurityException("Güvensiz arşiv girdisi: ${entry.name}")
                        }

                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            outFile.outputStream().use { output -> zis.copyTo(output) }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
                destinationDir
            }
        }
}
