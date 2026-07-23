package com.example.smartfilemanager.ui.screens.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartfilemanager.data.FileManager
import com.example.smartfilemanager.data.OperationResult
import com.example.smartfilemanager.model.FileItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FilesUiState(
    val currentPath: String? = null,
    val isLoading: Boolean = true,
    val items: List<FileItem> = emptyList(),
    val errorMessage: String? = null
)

/**
 * Belirli bir klasörün içeriğini listeler. Kopyala/kes/sil gibi düzenleme
 * işlemleri Aşama 3'te bu ViewModel üzerine eklenecektir.
 */
@HiltViewModel
class FilesViewModel @Inject constructor(
    private val fileManager: FileManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(FilesUiState())
    val uiState: StateFlow<FilesUiState> = _uiState.asStateFlow()

    fun load(path: String) {
        if (_uiState.value.currentPath == path && _uiState.value.items.isNotEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, currentPath = path, errorMessage = null)
            when (val result = fileManager.listFiles(path)) {
                is OperationResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, items = result.data)
                }
                is OperationResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }
}
