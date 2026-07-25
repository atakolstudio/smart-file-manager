package com.example.smartfilemanager.ui.screens.preview

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartfilemanager.data.FileManager
import com.example.smartfilemanager.data.FileOpener
import com.example.smartfilemanager.data.OperationResult
import com.example.smartfilemanager.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

private val textPreviewExtensions = setOf("txt", "md", "json", "xml", "html", "csv", "log", "kt", "java", "gradle", "properties")

enum class PreviewKind { IMAGE, PDF, TEXT, UNSUPPORTED }

data class PreviewUiState(
    val path: String? = null,
    val kind: PreviewKind = PreviewKind.UNSUPPORTED,
    val isLoading: Boolean = true,
    val textContent: String? = null,
    val pdfPageBitmap: Bitmap? = null,
    val pdfPageCount: Int = 0,
    val errorMessage: String? = null,
    val openError: String? = null
)

/**
 * Resim (doğrudan Coil ile), PDF (ilk sayfa - android.graphics.pdf.PdfRenderer ile) ve
 * metin tabanlı dosyaların (txt/json/xml/html/md/csv) önizlemesini yönetir.
 */
@HiltViewModel
class PreviewViewModel @Inject constructor(
    private val fileManager: FileManager,
    private val fileOpener: FileOpener,
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
        val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif")

        val kind = when {
            extension in imageExtensions -> PreviewKind.IMAGE
            extension == "pdf" -> PreviewKind.PDF
            extension in textPreviewExtensions -> PreviewKind.TEXT
            else -> PreviewKind.UNSUPPORTED
        }

        _uiState.value = PreviewUiState(path = path, kind = kind, isLoading = true)

        viewModelScope.launch {
            when (kind) {
                PreviewKind.TEXT -> loadText(path)
                PreviewKind.PDF -> loadPdfFirstPage(path)
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
