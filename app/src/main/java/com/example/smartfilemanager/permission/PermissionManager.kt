package com.example.smartfilemanager.permission

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uygulamanın ihtiyaç duyduğu farklı Android sürümlerine ait depolama izinlerini
 * tek noktadan yönetir (Android 10 ve altı, Android 11+ MANAGE_EXTERNAL_STORAGE,
 * Android 13+ granular medya izinleri).
 */
@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Runtime izin listesinde istenmesi gereken standart izinler (Android 12L ve altı).
     */
    fun getRequiredRuntimePermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO,
                android.Manifest.permission.READ_MEDIA_AUDIO
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> emptyArray()
            else -> arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    /**
     * Tüm dosyalara erişim izninin (MANAGE_EXTERNAL_STORAGE) verilip verilmediğini kontrol eder.
     * Android 11 öncesi sürümlerde klasik izinlere bakar.
     */
    fun hasAllFilesAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            hasClassicStoragePermission()
        }
    }

    private fun hasClassicStoragePermission(): Boolean {
        return getRequiredRuntimePermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasMediaPermissions(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return getRequiredRuntimePermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Android 11+ üzerinde "Tüm dosyalara erişim izni" ekranını açan Intent'i üretir.
     */
    fun createManageAllFilesIntent(): Intent {
        return Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    /**
     * Kullanıcıyı doğrudan uygulama ayarlarına yönlendiren Intent (izin kalıcı reddedildiğinde).
     */
    fun createAppSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }
}
