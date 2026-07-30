package com.example.smartfilemanager.permission

import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class PermissionManagerTest {

    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val permissionManager = PermissionManager(context)

    @Config(sdk = [Build.VERSION_CODES.P]) // Android 9: klasik depolama izinleri gerekir
    @Test
    fun `pre-Android 11 requires classic READ-WRITE storage permissions`() {
        val permissions = permissionManager.getRequiredRuntimePermissions()
        assertEquals(
            setOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
            permissions.toSet()
        )
    }

    @Config(sdk = [Build.VERSION_CODES.R]) // Android 11: MANAGE_EXTERNAL_STORAGE modeli, runtime izin listesi boş
    @Test
    fun `Android 11 requires no classic runtime permissions`() {
        val permissions = permissionManager.getRequiredRuntimePermissions()
        assertTrue(permissions.isEmpty())
    }

    @Config(sdk = [Build.VERSION_CODES.TIRAMISU]) // Android 13: granular medya izinleri
    @Test
    fun `Android 13 requires granular media permissions`() {
        val permissions = permissionManager.getRequiredRuntimePermissions()
        assertEquals(
            setOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO,
                android.Manifest.permission.READ_MEDIA_AUDIO
            ),
            permissions.toSet()
        )
    }

    @Test
    fun `createManageAllFilesIntent targets this app's package`() {
        val intent = permissionManager.createManageAllFilesIntent()
        assertEquals(
            "package:${context.packageName}",
            intent.data.toString()
        )
    }

    @Test
    fun `createAppSettingsIntent opens application details settings`() {
        val intent = permissionManager.createAppSettingsIntent()
        assertEquals(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
        assertEquals("package:${context.packageName}", intent.data.toString())
    }
}
