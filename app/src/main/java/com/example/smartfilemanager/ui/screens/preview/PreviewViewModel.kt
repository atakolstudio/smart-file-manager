package com.example.smartfilemanager.ui.screens.preview

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartfilemanager.data.FileManager
import com.example.smartfilemanager.data.FileOpener
import com.example.smartfilemanager.data.OperationResult
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

private val textPreviewExtensions = setOf("txt", "md", "json", "xml", "html", "csv", "log", "kt", "java", "gradle", "properties")
private val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif")
private val videoExtensions = setOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "3gp", "m4v")
private val audioExtensions = setOf("mp3", "wav", "ogg", "flac", "aac", "m4a", "wma", "opus")

enum class PreviewKind { IMAGE, PDF, TEXT, VIDEO, AUDIO, APK, UNSUPPORTED }

data class ApkPreviewInfo(
    val appName: String,
    val packageName: String,
    val versionName: String?,
    val icon: Bitmap?
)

data class PreviewUiState(
    val path: String? = null,
    val kind: PreviewKind = PreviewKind.UNSUPPORTED,
    val isLoading: Boolean = true,
    val textContent: String? = null,
    val pdfPageBitmap: Bitmap? = null,
    val pdfPageCount: Int = 0,
    val apkInfo: ApkPreviewInfo? = null,
    val errorMessage: String? = null,
    val openError: String? = null
)

/**
 * Dosya önizlemesini yönetir — "bilgisayar gibi" her yaygın dosya türü uygulama İÇİNDE
 * açılır: resim (Coil), PDF (ilk sayfa), metin, VİDEO ve SES (Media3 ExoPlayer ile gerçek
 * oynatma — PreviewScreen'de), APK (kurulum gerekmeden ad/paket/sürüm/ikon önizlemesi).
 * Sadece gerçekten uygulama içinde gösterilemeyen türler (docx, xlsx vb.) harici uygulamaya yönlendirilir.
 */
@HiltViewModel
class PreviewViewModel @Inject constructor(
    private val fileManager: FileManager,
    private val fileOpener: FileOpener,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(PreviewUiState())
    val uiState: StateFlow<PreviewUiState> = _uiState.asStateFlow()

    fun openExternally(path: String) {
        when (val result = fileOpener.openWithExternalApp(path)) {
            is OperationResult.Success -> Unit
            is OperationResult.Error -> _uiState.value = _uiState.value.copy(openError = result.message)
        }
    }

    fun consumeError() {
        _uiState.value = _uiState.value.copy(openError = null)
    }

    fun load(path: String) {
        if (_uiState.value.path == path) return
        val extension = path.substringAfterLast('.', "").lowercase()

        val kind = when {
            extension in imageExtensions -> PreviewKind.IMAGE
            extension == "pdf" -> PreviewKind.PDF
            extension in videoExtensions -> PreviewKind.VIDEO
            extension in audioExtensions -> PreviewKind.AUDIO
            extension == "apk" -> PreviewKind.APK
            extension in textPreviewExtensions -> PreviewKind.TEXT
            else -> PreviewKind.UNSUPPORTED
        }

        _uiState.value = PreviewUiState(path = path, kind = kind, isLoading = true)

        viewModelScope.launch {
            when (kind) {
                PreviewKind.TEXT -> loadText(path)
                PreviewKind.PDF -> loadPdfFirstPage(path)
                PreviewKind.APK -> loadApkInfo(path)
                // VİDEO/SES: gerçek oynatma PreviewScreen içinde ExoPlayer ile yapılır,
                // burada ekstra bir veri hazırlığına gerek yok.
                PreviewKind.VIDEO, PreviewKind.AUDIO -> _uiState.value = _uiState.value.copy(isLoading = false)
                else -> _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private suspend fun loadText(path: String) {
        when (val result = fileManager.readTextPreview(path)) {
            is OperationResult.Success -> _uiState.value = _uiState.value.copy(
                isLoading = false,
                textContent = result.data
            )
            is OperationResult.Error -> _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = result.message
            )
        }
    }

    private suspend fun loadApkInfo(path: String) {
        val info = withContext(ioDispatcher) {
            try {
                val pm = context.packageManager
                val packageInfo = pm.getPackageArchiveInfo(path, 0)
                if (packageInfo == null) {
                    null
                } else {
                    packageInfo.applicationInfo?.apply {
                        sourceDir = path
                        publicSourceDir = path
                    }
                    val appName = packageInfo.applicationInfo
                        ?.let { pm.getApplicationLabel(it).toString() }
                        ?: File(path).name
                    val icon = try {
                        packageInfo.applicationInfo?.loadIcon(pm)?.toBitmap(width = 128, height = 128)
                    } catch (t: Throwable) {
                        null
                    }
                    ApkPreviewInfo(
                        appName = appName,
                        packageName = packageInfo.packageName,
                        versionName = packageInfo.versionName,
                        icon = icon
                    )
                }
            } catch (t: Throwable) {
                android.util.Log.e("SmartFileManager", "APK bilgisi okunamadı: $path", t)
                null
            }
        }

        _uiState.value = if (info != null) {
            _uiState.value.copy(isLoading = false, apkInfo = info)
        } else {
            _uiState.value.copy(isLoading = false, errorMessage = "APK bilgisi okunamadı")
        }
    }

    private suspend fun loadPdfFirstPage(path: String) {
        val result = withContext(ioDispatcher) {
            try {
                val pfd = ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
                PdfRenderer(pfd).use { renderer ->
                    val pageCount = renderer.pageCount
                    if (pageCount == 0) return@withContext null to 0
                    renderer.openPage(0).use { page ->
                        val bitmap = Bitmap.createBitmap(
                            page.width.coerceAtLeast(1) * 2,
                            page.height.coerceAtLeast(1) * 2,
                            Bitmap.Config.ARGB_8888
                        )
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap to pageCount
                    }
                }
            } catch (t: Throwable) {
                android.util.Log.e("SmartFileManager", "PDF önizlenemedi: $path", t)
                null to 0
            }
        }

        _uiState.value = if (result.first != null) {
            _uiState.value.copy(isLoading = false, pdfPageBitmap = result.first, pdfPageCount = result.second)
        } else {
            _uiState.value.copy(isLoading = false, errorMessage = "PDF önizlenemedi")
        }
    }
}
