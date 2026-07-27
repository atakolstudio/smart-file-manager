package com.example.smartfilemanager.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.smartfilemanager.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.inject.Inject
import javax.inject.Singleton

private val Context.recycleBinDataStore by preferencesDataStore(name = "recycle_bin")

data class TrashEntry(
    val originalPath: String,
    val trashPath: String,
    val deletedAt: Long,
    val name: String
) {
    fun encode(): String = listOf(originalPath, trashPath, deletedAt.toString(), name)
        .joinToString(SEPARATOR) { it.replace(SEPARATOR, "") }

    companion object {
        private const val SEPARATOR = ":::"

        fun decode(raw: String): TrashEntry? {
            val parts = raw.split(SEPARATOR)
            if (parts.size != 4) return null
            val deletedAt = parts[2].toLongOrNull() ?: return null
            return TrashEntry(originalPath = parts[0], trashPath = parts[1], deletedAt = deletedAt, name = parts[3])
        }
    }
}

/**
 * "Sil" işleminde dosyaları kalıcı olarak silmek yerine uygulamanın kendi dahili
 * depolamasındaki (izin gerektirmeyen) bir çöp kutusu klasörüne taşır; kullanıcı
 * isterse geri yükleyebilir ya da kalıcı olarak silebilir.
 */
@Singleton
class RecycleBinManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val entriesKey = stringSetPreferencesKey("entries")

    private val trashDir: File
        get() = File(context.filesDir, "trash").apply { mkdirs() }

    val entries: Flow<List<TrashEntry>> = context.recycleBinDataStore.data.map { prefs ->
        (prefs[entriesKey] ?: emptySet())
            .mapNotNull { TrashEntry.decode(it) }
            .sortedByDescending { it.deletedAt }
    }

    suspend fun moveToTrash(path: String): OperationResult<Unit> =
        withContext(ioDispatcher) {
            safeFileOperation("Çöp kutusuna taşınamadı: $path") {
                val source = File(path)
                if (!source.exists()) throw IllegalStateException("Dosya bulunamadı")

                val trashName = "${System.currentTimeMillis()}_${source.name}"
                val destination = File(trashDir, trashName)

                copyRecursively(source, destination)
                if (!source.deleteRecursively()) {
                    destination.deleteRecursively()
                    throw IllegalStateException("Kaynak dosya silinemedi")
                }

                val entry = TrashEntry(
                    originalPath = source.absolutePath,
                    trashPath = destination.absolutePath,
                    deletedAt = System.currentTimeMillis(),
                    name = source.name
                )
                context.recycleBinDataStore.edit { prefs ->
                    val current = prefs[entriesKey] ?: emptySet()
                    prefs[entriesKey] = current + entry.encode()
                }
            }
        }

    suspend fun restore(entry: TrashEntry): OperationResult<Unit> =
        withContext(ioDispatcher) {
            safeFileOperation("Geri yüklenemedi: ${entry.name}") {
                val trashFile = File(entry.trashPath)
                if (!trashFile.exists()) throw IllegalStateException("Çöp kutusunda dosya bulunamadı")

                val originalFile = File(entry.originalPath)
                originalFile.parentFile?.mkdirs()
                if (originalFile.exists()) {
                    throw IllegalStateException("Asıl konumda aynı adda bir öğe zaten var")
                }

                copyRecursively(trashFile, originalFile)
                trashFile.deleteRecursively()
                removeEntry(entry)
            }
        }

    suspend fun deleteForever(entry: TrashEntry): OperationResult<Unit> =
        withContext(ioDispatcher) {
            safeFileOperation("Kalıcı olarak silinemedi: ${entry.name}") {
                File(entry.trashPath).deleteRecursively()
                removeEntry(entry)
            }
        }

    suspend fun emptyBin(): OperationResult<Unit> =
        withContext(ioDispatcher) {
            safeFileOperation("Çöp kutusu boşaltılamadı") {
                trashDir.listFiles()?.forEach { it.deleteRecursively() }
                context.recycleBinDataStore.edit { prefs -> prefs[entriesKey] = emptySet() }
            }
        }

    private suspend fun removeEntry(entry: TrashEntry) {
        context.recycleBinDataStore.edit { prefs ->
            val current = prefs[entriesKey] ?: emptySet()
            prefs[entriesKey] = current.filterNot { TrashEntry.decode(it)?.trashPath == entry.trashPath }.toSet()
        }
    }

    private fun copyRecursively(source: File, destination: File) {
        if (source.isDirectory) {
            destination.mkdirs()
            source.listFiles()?.forEach { child ->
                copyRecursively(child, File(destination, child.name))
            }
        } else {
            destination.parentFile?.mkdirs()
            Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
