package com.example.smartfilemanager.ui.screens.apps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Environment
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartfilemanager.data.OperationResult
import com.example.smartfilemanager.data.safeFileOperation
import com.example.smartfilemanager.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class AppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String?,
    val apkPath: String,
    val sizeBytes: Long,
    val icon: Bitmap?
)

data class AppsUiState(
    val apps: List<AppInfo> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

/**
 * Cihaza kullanıcı tarafından yüklenmiş (sistem uygulaması olmayan) uygulamaları listeler
 * ve seçilen uygulamanın APK dosyasını "İndirilenler" klasörüne dışa aktarabilir.
 */
@HiltViewModel
class AppsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppsUiState())
    val uiState: StateFlow<AppsUiState> = _uiState.asStateFlow()

    private var loaded = false

    fun loadIfNeeded() {
        if (loaded) return
        loaded = true
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = withContext(ioDispatcher) {
                safeFileOperation("Uygulamalar okunamadı") {
                    val pm = context.packageManager
                    pm.getInstalledApplications(PackageManager.GET_META_DATA)
                        .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
                        .map { appInfo ->
                            val versionName = try {
                                pm.getPackageInfo(appInfo.packageName, 0).versionName
                            } catch (t: Throwable) {
                                null
                            }
                            val icon = try {
                                appInfo.loadIcon(pm).toBitmap(width = 96, height = 96)
                            } catch (t: Throwable) {
                                null
                            }
                            AppInfo(
                                packageName = appInfo.packageName,
                                appName = appInfo.loadLabel(pm).toString(),
                                versionName = versionName,
                                apkPath = appInfo.sourceDir,
                                sizeBytes = File(appInfo.sourceDir).length(),
                                icon = icon
                            )
                        }
                        .sortedBy { it.appName.lowercase() }
                }
            }
            when (result) {
                is OperationResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false, apps = result.data)
                is OperationResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }

    fun exportApk(app: AppInfo) {
        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                safeFileOperation("APK dışa aktarılamadı") {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    downloadsDir.mkdirs()
                    val safeName = app.appName.replace(Regex("[^A-Za-z0-9._-]"), "_")
                    val destination = File(downloadsDir, "${safeName}_${app.versionName ?: "apk"}.apk")
                    File(app.apkPath).copyTo(destination, overwrite = true)
                    destination
                }
            }
            when (result) {
                is OperationResult.Success -> _uiState.value = _uiState.value.copy(infoMessage = "Dışa aktarıldı: ${result.data.name}")
                is OperationResult.Error -> _uiState.value = _uiState.value.copy(errorMessage = result.message)
            }
        }
    }

    fun uninstallApp(app: AppInfo) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_DELETE).apply {
                data = android.net.Uri.parse("package:${app.packageName}")
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (t: Throwable) {
            _uiState.value = _uiState.value.copy(errorMessage = t.message ?: "Kaldırma başlatılamadı")
        }
    }

    fun consumeMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, infoMessage = null)
    }
}
