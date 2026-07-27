package com.example.smartfilemanager.ui.screens.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartfilemanager.data.CleanupManager
import com.example.smartfilemanager.data.OperationResult
import com.example.smartfilemanager.data.StorageAnalysisManager
import com.example.smartfilemanager.model.StorageAnalysisResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StorageAnalysisUiState(
    val hasScanned: Boolean = false,
    val isScanning: Boolean = false,
    val result: StorageAnalysisResult = StorageAnalysisResult(),
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

/**
 * Depolama analizi ekranının durumunu yönetir. Tarama kullanıcı tetiklemesiyle başlar
 * (ekran açılır açılmaz otomatik taramaz) — böylece gezinme hiçbir zaman bloklanmaz.
 */
@HiltViewModel
class StorageAnalysisViewModel @Inject constructor(
    private val analysisManager: StorageAnalysisManager,
    private val cleanupManager: CleanupManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(StorageAnalysisUiState())
    val uiState: StateFlow<StorageAnalysisUiState> = _uiState.asStateFlow()

    fun startScan() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, errorMessage = null)
            when (val result = analysisManager.analyze()) {
                is OperationResult.Success -> _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    hasScanned = true,
                    result = result.data,
                    infoMessage = if (result.data.scanTruncated) {
                        "Tarama süre sınırına takıldı, kısmi sonuçlar gösteriliyor"
                    } else null
                )
                is OperationResult.Error -> _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    errorMessage = result.message
                )
            }
        }
    }

    fun deleteEmptyFolders() {
        val folders = _uiState.value.result.emptyFolders
        if (folders.isEmpty()) return
        viewModelScope.launch {
            when (val result = cleanupManager.deleteEmptyFolders(folders)) {
                is OperationResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        infoMessage = "${result.data} boş klasör silindi",
                        result = _uiState.value.result.copy(emptyFolders = emptyList())
                    )
                }
                is OperationResult.Error -> _uiState.value = _uiState.value.copy(errorMessage = result.message)
            }
        }
    }

    fun removeDuplicates() {
        val groups = _uiState.value.result.duplicateGroups
        if (groups.isEmpty()) return
        viewModelScope.launch {
            when (val result = cleanupManager.removeDuplicates(groups)) {
                is OperationResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        infoMessage = "${result.data} yinelenen dosya çöp kutusuna taşındı",
                        result = _uiState.value.result.copy(duplicateGroups = emptyList())
                    )
                }
                is OperationResult.Error -> _uiState.value = _uiState.value.copy(errorMessage = result.message)
            }
        }
    }

    fun clearAppCache() {
        viewModelScope.launch {
            when (val result = cleanupManager.clearAppCache()) {
                is OperationResult.Success -> _uiState.value = _uiState.value.copy(
                    infoMessage = "Uygulama önbelleği temizlendi: ${com.example.smartfilemanager.util.SizeFormatter.format(result.data)}"
                )
                is OperationResult.Error -> _uiState.value = _uiState.value.copy(errorMessage = result.message)
            }
        }
    }

    fun consumeMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, infoMessage = null)
    }
}
