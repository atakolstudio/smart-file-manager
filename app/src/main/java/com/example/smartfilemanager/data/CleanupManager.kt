package com.example.smartfilemanager.data

import android.content.Context
import com.example.smartfilemanager.di.IoDispatcher
import com.example.smartfilemanager.model.DuplicateGroup
import com.example.smartfilemanager.model.EmptyFolderEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Depolama analizi sonuçlarına dayalı temizleme işlemlerini gerçekleştirir.
 * Yinelenen dosyalar kalıcı silinmez, güvenlik için çöp kutusuna taşınır.
 */
@Singleton
class CleanupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recycleBinManager: RecycleBinManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    suspend fun deleteEmptyFolders(folders: List<EmptyFolderEntry>): OperationResult<Int> =
        withContext(ioDispatcher) {
            safeFileOperation("Boş klasörler silinemedi") {
                var deletedCount = 0
                folders.forEach { entry ->
                    val dir = File(entry.path)
                    if (dir.exists() && dir.isDirectory && dir.listFiles()?.isEmpty() == true) {
                        if (dir.delete()) deletedCount++
                    }
                }
                deletedCount
            }
        }

    /**
     * Her grupta ilk dosyayı ("orijinal") korur, geri kalanları çöp kutusuna taşır.
     */
    suspend fun removeDuplicates(groups: List<DuplicateGroup>): OperationResult<Int> =
        withContext(ioDispatcher) {
            safeFileOperation("Yinelenen dosyalar temizlenemedi") {
                var removedCount = 0
                groups.forEach { group ->
                    group.paths.drop(1).forEach { path ->
                        val result = recycleBinManager.moveToTrash(path)
                        if (result is OperationResult.Success) removedCount++
                    }
                }
                removedCount
            }
        }

    suspend fun clearAppCache(): OperationResult<Long> =
        withContext(ioDispatcher) {
            safeFileOperation("Önbellek temizlenemedi") {
                var freedBytes = 0L
                fun deleteRecursivelyAndSum(dir: File) {
                    dir.listFiles()?.forEach { file ->
                        if (file.isDirectory) {
                            deleteRecursivelyAndSum(file)
                        } else {
                            freedBytes += file.length()
                            file.delete()
                        }
                    }
                }
                context.cacheDir?.let { deleteRecursivelyAndSum(it) }
                context.externalCacheDir?.let { deleteRecursivelyAndSum(it) }
                freedBytes
            }
        }
}
