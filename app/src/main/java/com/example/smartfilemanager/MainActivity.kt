package com.example.smartfilemanager

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartfilemanager.data.ThemeMode
import com.example.smartfilemanager.navigation.SmartFileManagerNavHost
import com.example.smartfilemanager.permission.PermissionManager
import com.example.smartfilemanager.ui.AppLockViewModel
import com.example.smartfilemanager.ui.MainViewModel
import com.example.smartfilemanager.ui.screens.lock.LockScreen
import com.example.smartfilemanager.ui.theme.SmartFileManagerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Uygulamanın tek Activity'si. İzin isteme akışını (Android 11+ MANAGE_EXTERNAL_STORAGE
 * veya Android 12L ve altı runtime izinleri) ve uygulama kilidi (parmak izi/yüz/PIN)
 * kimlik doğrulamasını başlatır. FragmentActivity'den türetilmesinin nedeni
 * androidx.biometric.BiometricPrompt'ın bunu gerektirmesidir.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

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
            val mainViewModel: MainViewModel = hiltViewModel()
            val themeMode by mainViewModel.themeMode.collectAsStateWithLifecycle()

            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            SmartFileManagerTheme(darkTheme = darkTheme) {
                val appLockViewModel: AppLockViewModel = hiltViewModel()
                val isLockEnabled by appLockViewModel.isLockEnabled.collectAsStateWithLifecycle()
                val isUnlocked by appLockViewModel.isUnlocked.collectAsStateWithLifecycle()

                if (isLockEnabled && !isUnlocked) {
                    LockScreen(onUnlockClick = { showBiometricPrompt(onSuccess = appLockViewModel::onUnlocked) })
                } else {
                    SmartFileManagerNavHost(
                        onRequestPermission = { requestStoragePermission() }
                    )
                }
            }
        }
    }

    private fun showBiometricPrompt(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Uygulama Kilidi")
            .setSubtitle("Devam etmek için kimliğinizi doğrulayın")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
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
