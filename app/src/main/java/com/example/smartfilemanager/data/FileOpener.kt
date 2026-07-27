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
 * cihazdaki varsayılan uygulamada açmak için kullanılır.
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
}
