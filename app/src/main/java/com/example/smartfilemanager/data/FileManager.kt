package com.example.smartfilemanager.data

import com.example.smartfilemanager.di.IoDispatcher
import com.example.smartfilemanager.model.FileItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dosya sistemi üzerindeki temel işlemleri (listeleme, kopyalama, taşıma, silme,
 * yeniden adlandırma, klasör oluşturma) gerçekleştirir. Tüm ağır işlemler
 * Dispatchers.IO üzerinde ve try-catch koruması altında çalışır; hiçbir hata
 * üst katmana çökme (crash) olarak sızmaz, bunun yerine [OperationResult.Error] döner.
 */
@Singleton
class FileManager @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    suspend fun listFiles(directoryPath: String, showHidden: Boolean = false): OperationResult<List<FileItem>> =
        withContext(ioDispatcher) {
            safeFileOperation("Klasör listelenemedi: $directoryPath") {
                val directory = File(directoryPath)
                val entries = directory.listFiles() ?: emptyArray()
                entries
                    .filter { showHidden || !it.isHidden }
                    .map { FileItem.fromFile(it) }
                    .sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenBy { it.name.lowercase() })
            }
        }

    suspend fun createFolder(parentPath: String, folderName: String): OperationResult<FileItem> =
        withContext(ioDispatcher) {
            safeFileOperation("Klasör oluşturulamadı: $folderName") {
                val newDir = File(parentPath, folderName)
                if (newDir.exists()) {
                    throw IllegalStateException("\"$folderName\" adında bir klasör zaten var")
                }
                if (!newDir.mkdirs()) {
                    throw IllegalStateException("Klasör oluşturulamadı")
                }
                FileItem.fromFile(newDir)
            }
        }

    suspend fun createFile(parentPath: String, fileName: String): OperationResult<FileItem> =
        withContext(ioDispatcher) {
            safeFileOperation("Dosya oluşturulamadı: $fileName") {
                val newFile = File(parentPath, fileName)
                if (newFile.exists()) {
                    throw IllegalStateException("\"$fileName\" adında bir dosya zaten var")
                }
                if (!newFile.createNewFile()) {
                    throw IllegalStateException("Dosya oluşturulamadı")
                }
                FileItem.fromFile(newFile)
            }
        }

    suspend fun rename(path: String, newName: String): OperationResult<FileItem> =
        withContext(ioDispatcher) {
            safeFileOperation("Yeniden adlandırılamadı: $path") {
                val source = File(path)
                val target = File(source.parentFile, newName)
                if (target.exists()) {
                    throw IllegalStateException("\"$newName\" adında bir öğe zaten var")
                }
                if (!source.renameTo(target)) {
                    throw IllegalStateException("Yeniden adlandırma başarısız oldu")
                }
                FileItem.fromFile(target)
            }
        }

    suspend fun delete(path: String): OperationResult<Unit> =
        withContext(ioDispatcher) {
            safeFileOperation("Silinemedi: $path") {
                val target = File(path)
                if (!target.deleteRecursively()) {
                    throw IllegalStateException("Silme işlemi tamamlanamadı")
                }
            }
        }

    suspend fun copy(sourcePath: String, destinationDirectoryPath: String): OperationResult<FileItem> =
        withContext(ioDispatcher) {
            safeFileOperation("Kopyalanamadı: $sourcePath") {
                val source = File(sourcePath)
                val destination = File(destinationDirectoryPath, source.name)
                copyRecursively(source, destination)
                FileItem.fromFile(destination)
            }
        }

    suspend fun move(sourcePath: String, destinationDirectoryPath: String): OperationResult<FileItem> =
        withContext(ioDispatcher) {
            safeFileOperation("Taşınamadı: $sourcePath") {
                val source = File(sourcePath)
                val destination = File(destinationDirectoryPath, source.name)
                if (!source.renameTo(destination)) {
                    // Farklı bir disk bölümüne taşınıyorsa renameTo başarısız olabilir; kopyala + sil.
                    copyRecursively(source, destination)
                    if (!source.deleteRecursively()) {
                        throw IllegalStateException("Kaynak dosya silinemedi, taşıma tamamlanamadı")
                    }
                }
                FileItem.fromFile(destination)
            }
        }

    suspend fun calculateFolderSize(path: String): OperationResult<Long> =
        withContext(ioDispatcher) {
            safeFileOperation("Klasör boyutu hesaplanamadı: $path") {
                File(path).walkBottomUp().filter { it.isFile }.sumOf { it.length() }
            }
        }

    private fun copyRecursively(source: File, destination: File) {
        if (source.isDirectory) {
            if (!destination.exists()) destination.mkdirs()
            source.listFiles()?.forEach { child ->
                copyRecursively(child, File(destination, child.name))
            }
        } else {
            Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
