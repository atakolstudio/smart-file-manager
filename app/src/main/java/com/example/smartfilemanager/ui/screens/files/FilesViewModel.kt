package com.example.smartfilemanager.ui.screens.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartfilemanager.data.ClipboardManager
import com.example.smartfilemanager.data.ClipboardOperation
import com.example.smartfilemanager.data.FileManager
import com.example.smartfilemanager.data.OperationResult
import com.example.smartfilemanager.model.FileItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FilesUiState(
    val currentPath: String? = null,
    val isLoading: Boolean = true,
    val items: List<FileItem> = emptyList(),
    val selectedPaths: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val hasClipboardContent: Boolean = false,
    val clipboardCount: Int = 0
) {
    val isSelectionMode: Boolean get() = selectedPaths.isNotEmpty()
}

/**
 * Klasör içeriğini listeler ve Aşama 3 kapsamındaki tüm temel dosya işlemlerini
 * (kopyala/kes/yapıştır/sil/yeniden adlandır/yeni klasör, çoklu seçim) yönetir.
 */
@HiltViewModel
class FilesViewModel @Inject constructor(
    private val fileManager: FileManager,
    private val clipboardManager: ClipboardManager
) : ViewModel() {

    private val _currentPath = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(true)
    private val _items = MutableStateFlow<List<FileItem>>(emptyList())
    private val _selectedPaths = MutableStateFlow<Set<String>>(emptySet())
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _infoMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<FilesUiState> = combine(
        _currentPath, _isLoading, _items, _selectedPaths, _errorMessage, _infoMessage, clipboardManager.state
    ) { values ->
        val currentPath = values[0] as String?
        val isLoading = values[1] as Boolean
        @Suppress("UNCHECKED_CAST")
        val items = values[2] as List<FileItem>
        @Suppress("UNCHECKED_CAST")
        val selectedPaths = values[3] as Set<String>
        val errorMessage = values[4] as String?
        val infoMessage = values[5] as String?
        val clipboard = values[6] as com.example.smartfilemanager.data.ClipboardState?

        FilesUiState(
            currentPath = currentPath,
            isLoading = isLoading,
            items = items,
            selectedPaths = selectedPaths,
            errorMessage = errorMessage,
            infoMessage = infoMessage,
            hasClipboardContent = clipboard != null,
            clipboardCount = clipboard?.paths?.size ?: 0
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        FilesUiState()
    )

    fun load(path: String) {
        if (_currentPath.value == path && _items.value.isNotEmpty()) return
        _currentPath.value = path
        refresh()
    }

    fun refresh() {
        val path = _currentPath.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            when (val result = fileManager.listFiles(path)) {
                is OperationResult.Success -> {
                    _items.value = result.data
                    _isLoading.value = false
                }
                is OperationResult.Error -> {
                    _errorMessage.value = result.message
                    _isLoading.value = false
                }
            }
        }
    }

    // --- Çoklu seçim ---

    fun toggleSelection(path: String) {
        val current = _selectedPaths.value
        _selectedPaths.value = if (current.contains(path)) current - path else current + path
    }

    fun selectAll() {
        _selectedPaths.value = _items.value.map { it.path }.toSet()
    }

    fun clearSelection() {
        _selectedPaths.value = emptySet()
    }

    // --- Kopyala / Kes / Yapıştır ---

    fun copySelectionToClipboard() {
        clipboardManager.copy(_selectedPaths.value.toList())
        clearSelection()
    }

    fun cutSelectionToClipboard() {
        clipboardManager.cut(_selectedPaths.value.toList())
        clearSelection()
    }

    fun pasteFromClipboard() {
        val clipboard = clipboardManager.state.value ?: return
        val destination = _currentPath.value ?: return
        viewModelScope.launch {
            var lastError: String? = null
            clipboard.paths.forEach { sourcePath ->
                val result = when (clipboard.operation) {
                    ClipboardOperation.COPY -> fileManager.copy(sourcePath, destination)
                    ClipboardOperation.CUT -> fileManager.move(sourcePath, destination)
                }
                if (result is OperationResult.Error) lastError = result.message
            }
            clipboardManager.clear()
            _errorMessage.value = lastError
            _infoMessage.value = if (lastError == null) "Yapıştırma tamamlandı" else null
            refresh()
        }
    }

    // --- Sil / Yeniden adlandır / Oluştur ---

    fun deleteSelected() {
        val paths = _selectedPaths.value
        if (paths.isEmpty()) return
        viewModelScope.launch {
            var lastError: String? = null
            paths.forEach { path ->
                val result = fileManager.delete(path)
                if (result is OperationResult.Error) lastError = result.message
            }
            clearSelection()
            _errorMessage.value = lastError
            _infoMessage.value = if (lastError == null) "Silindi" else null
            refresh()
        }
    }

    fun deleteSingle(path: String) {
        viewModelScope.launch {
            when (val result = fileManager.delete(path)) {
                is OperationResult.Success -> {
                    _infoMessage.value = "Silindi"
                    refresh()
                }
                is OperationResult.Error -> _errorMessage.value = result.message
            }
        }
    }

    fun rename(path: String, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            when (val result = fileManager.rename(path, newName)) {
                is OperationResult.Success -> {
                    _infoMessage.value = "Yeniden adlandırıldı"
                    refresh()
                }
                is OperationResult.Error -> _errorMessage.value = result.message
            }
        }
    }

    fun createFolder(name: String) {
        val parent = _currentPath.value ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            when (val result = fileManager.createFolder(parent, name)) {
                is OperationResult.Success -> refresh()
                is OperationResult.Error -> _errorMessage.value = result.message
            }
        }
    }

    fun createFile(name: String) {
        val parent = _currentPath.value ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            when (val result = fileManager.createFile(parent, name)) {
                is OperationResult.Success -> refresh()
                is OperationResult.Error -> _errorMessage.value = result.message
            }
        }
    }

    fun consumeMessages() {
        _errorMessage.value = null
        _infoMessage.value = null
    }
}
