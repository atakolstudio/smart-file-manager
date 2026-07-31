package com.example.smartfilemanager.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.smartfilemanager.util.MimeTypeHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uygulama içinde önizlenemeyen dosya türlerini (video, ses, apk, arşiv vb.)
 * cihazdaki varsayılan uygulamada açmak için ve dosyaları diğer uygulamalarla
 * (WhatsApp, e-posta, Bluetooth vb.) paylaşmak için kullanılır.
 */
@Singleton
class FileOpener @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun openWithExternalApp(path: String): OperationResult<Unit> {
        return try {
            val file = File(path)
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            val mimeType = MimeTypeHelper.getMimeType(file.name) ?: "*/*"

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(context.packageManager) == null) {
                throw IllegalStateException("Bu dosya türünü açabilecek bir uygulama bulunamadı")
            }

            context.startActivity(intent)
            OperationResult.Success(Unit)
        } catch (t: Throwable) {
            android.util.Log.e("SmartFileManager", "Dosya açılamadı: $path", t)
            OperationResult.Error(t.message ?: "Dosya açılamadı", t)
        }
    }

    /** Tek bir dosyayı Android'in paylaşım menüsüyle (WhatsApp, e-posta, Bluetooth vb.) paylaşır. */
    fun shareFile(path: String): OperationResult<Unit> = shareFiles(listOf(path))

    /** Birden fazla dosyayı tek seferde paylaşır (çoklu seçimden). */
    fun shareFiles(paths: List<String>): OperationResult<Unit> {
        return try {
            if (paths.isEmpty()) throw IllegalStateException("Paylaşılacak dosya seçilmedi")

            val authority = "${context.packageName}.fileprovider"
            val uris = paths.map { path ->
                FileProvider.getUriForFile(context, authority, File(path))
            }

            val mimeTypes = paths.map { MimeTypeHelper.getMimeType(File(it).name) }
            val commonMimeType = if (mimeTypes.toSet().size == 1) mimeTypes.first() ?: "*/*" else "*/*"

            val intent = if (uris.size == 1) {
                Intent(Intent.ACTION_SEND).apply {
                    type = commonMimeType
                    putExtra(Intent.EXTRA_STREAM, uris.first())
                }
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = commonMimeType
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                }
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            val chooser = Intent.createChooser(intent, "Paylaş").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            OperationResult.Success(Unit)
        } catch (t: Throwable) {
            android.util.Log.e("SmartFileManager", "Paylaşılamadı: $paths", t)
            OperationResult.Error(t.message ?: "Paylaşılamadı", t)
        }
    }
}
