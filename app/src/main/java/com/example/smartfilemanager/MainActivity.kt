package com.example.smartfilemanager

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import com.example.smartfilemanager.navigation.SmartFileManagerNavHost
import com.example.smartfilemanager.permission.PermissionManager
import com.example.smartfilemanager.ui.theme.SmartFileManagerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Uygulamanın tek Activity'si. İzin isteme akışını (Android 11+ MANAGE_EXTERNAL_STORAGE
 * veya Android 12L ve altı runtime izinleri) başlatır; iznin gerçek zamanlı kontrolü
 * ekranlar (ör. HomeScreen) tarafından yaşam döngüsüne bağlı olarak yapılır.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var permissionManager: PermissionManager

    private val runtimePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Sonuç, ilgili ekranın onResume/refresh akışıyla otomatik olarak yansıtılır */ }

    private val manageAllFilesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* Sonuç, ilgili ekranın onResume/refresh akışıyla otomatik olarak yansıtılır */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SmartFileManagerTheme {
                SmartFileManagerNavHost(
                    onRequestPermission = { requestStoragePermission() }
                )
            }
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            manageAllFilesLauncher.launch(permissionManager.createManageAllFilesIntent())
        } else {
            val permissions = permissionManager.getRequiredRuntimePermissions()
            if (permissions.isNotEmpty()) {
                runtimePermissionLauncher.launch(permissions)
            }
        }
    }
}
