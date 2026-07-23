package com.example.smartfilemanager

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.smartfilemanager.navigation.SmartFileManagerNavHost
import com.example.smartfilemanager.permission.PermissionManager
import com.example.smartfilemanager.ui.theme.SmartFileManagerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var permissionManager: PermissionManager

    private val runtimePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        recheckPermissionState()
    }

    private val manageAllFilesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        recheckPermissionState()
    }

    private var hasPermissionState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        hasPermissionState = mutableStateOf(false)

        setContent {
            var hasPermission by hasPermissionState

            SmartFileManagerTheme {
                SmartFileManagerNavHost(
                    onRequestPermission = { requestStoragePermission() }
                )
            }

            androidx.compose.runtime.LaunchedEffect(Unit) {
                hasPermission = permissionManager.hasAllFilesAccess()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        recheckPermissionState()
    }

    private fun recheckPermissionState() {
        hasPermissionState.value = permissionManager.hasAllFilesAccess()
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
