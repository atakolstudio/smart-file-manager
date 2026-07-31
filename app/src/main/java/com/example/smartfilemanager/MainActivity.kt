package com.example.smartfilemanager

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val mainViewModel: com.example.smartfilemanager.ui.MainViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val themeMode by mainViewModel.themeMode.collectAsStateWithLifecycle()

            val darkTheme = when (themeMode) {
                com.example.smartfilemanager.data.ThemeMode.LIGHT -> false
                com.example.smartfilemanager.data.ThemeMode.DARK -> true
                com.example.smartfilemanager.data.ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            SmartFileManagerTheme(darkTheme = darkTheme) {
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
