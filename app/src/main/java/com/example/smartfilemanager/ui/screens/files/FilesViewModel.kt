package com.example.smartfilemanager.ui.screens.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartfilemanager.data.ArchiveManager
import com.example.smartfilemanager.data.ClipboardManager
import com.example.smartfilemanager.data.ClipboardOperation
import com.example.smartfilemanager.data.FavoritesManager
import com.example.smartfilemanager.data.FileManager
import com.example.smartfilemanager.data.FileOpener
import com.example.smartfilemanager.data.OperationResult
import com.example.smartfilemanager.data.RecycleBinManager
import com.example.smartfilemanager.model.FileInfo
import com.example.smartfilemanager.model.FileItem
import com.example.smartfilemanager.util.HashAlgorithm
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortOption { NAME, SIZE, DATE, TYPE }

data class FilesUiState(
    val currentPath: String? = null,
    val isLoading: Boolean = true,
    val items: List<FileItem> = emptyList(),
    val selectedPaths: Set<String> = emptySet(),
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.NAME,
    val sortAscending: Boolean = true,
    val favoritePaths: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val hasClipboardContent: Boolean = false,
    val clipboardCount: Int = 0
) {
    val isSelectionMode: Boolean get() = selectedPaths.isNotEmpty()

    /** Arama sorgusuna ve sıralama tercihine göre filtrelenmiş/sıralanmış liste. */
    val displayedItems: List<FileItem> by lazy {
        val filtered = if (searchQuery.isBlank()) {
            items
        } else {
            val regex = runCatching { Regex(searchQuery, RegexOption.IGNORE_CASE) }.getOrNull()
            items.filter { item ->
                regex?.containsMatchIn(item.name) ?: item.name.contains(searchQuery, ignoreCase = true)
            }
        }

        val comparator = when (sortOption) {
            SortOption.NAME -> compareBy<FileItem> { it.name.lowercase() }
            SortOption.SIZE -> compareBy { it.sizeBytes }
            SortOption.DATE -> compareBy { it.lastModified }
            SortOption.TYPE -> compareBy { it.extension.lowercase() }
        }
        val directional = if (sortAscending) comparator else comparator.reversed()

        filtered.sortedWith(compareByDescending<FileItem> { it.isDirectory }.then(directional))
    }
}

/**
 * Klasör içeriğini listeler ve tüm dosya işlemlerini yönetir: kopyala/kes/yapıştır/sil
 * (çöp kutusuna taşıyarak)/yeniden adlandır/yeni klasör, çoklu seçim, arama, sıralama,
 * favoriler, sıkıştırma/çıkartma, özet (hash) hesaplama, harici uygulamada açma, dosya bilgisi.
 */
@HiltViewModel
class FilesViewModel @Inject constructor(
    private val fileManager: FileManager,
    private val clipboardManager: ClipboardManager,
    private val fileOpener: FileOpener,
    private val favoritesManager: FavoritesManager,
    private val recycleBinManager: RecycleBinManager,
    private val archiveManager: ArchiveManager
) : ViewModel() {

    private val _currentPath = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(true)
    private val _items = MutableStateFlow<List<FileItem>>(emptyList())
    private val _selectedPaths = MutableStateFlow<Set<String>>(emptySet())
    private val _searchQuery = MutableStateFlow("")
    private val _sortOption = MutableStateFlow(SortOption.NAME)
    private val _sortAscending = MutableStateFlow(true)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _infoMessage = MutableStateFlow<String?>(null)

    private val _fileInfo = MutableStateFlow<FileInfo?>(null)
    val fileInfo: StateFlow<FileInfo?> = _fileInfo

    private val _hashResult = MutableStateFlow<String?>(null)
    val hashResult: StateFlow<String?> = _hashResult

    private val _isComputingHash = MutableStateFlow(false)
    val isComputingHash: StateFlow<Boolean> = _isComputingHash

    val uiState: StateFlow<FilesUiState> = combine(
        _currentPath, _isLoading, _items, _selectedPaths, _searchQuery,
        _sortOption, _sortAscending, _errorMessage, _infoMessage,
        clipboardManager.state, favoritesManager.favoritePaths
    ) { values ->
        val currentPath = values[0] as String?
        val isLoading = values[1] as Boolean
        @Suppress("UNCHECKED_CAST")
        val items = values[2] as List<FileItem>
        @Suppress("UNCHECKED_CAST")
        val selectedPaths = values[3] as Set<String>
        val searchQuery = values[4] as String
        val sortOption = values[5] as SortOption
        val sortAscending = values[6] as Boolean
        val errorMessage = values[7] as String?
        val infoMessage = values[8] as String?
        val clipboard = values[9] as com.example.smartfilemanager.data.ClipboardState?
        @Suppress("UNCHECKED_CAST")
        val favoritePaths = values[10] as Set<String>

        FilesUiState(
            currentPath = currentPath,
            isLoading = isLoading,
            items = items,
            selectedPaths = selectedPaths,
            searchQuery = searchQuery,
            sortOption = sortOption,
            sortAscending = sortAscending,
            favoritePaths = favoritePaths,
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

    // --- Arama / Sıralama ---

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSortOption(option: SortOption) { _sortOption.value = option }
    fun toggleSortDirection() { _sortAscending.value = !_sortAscending.value }

    // --- Çoklu seçim ---

    fun toggleSelection(path: String) {
        val current = _selectedPaths.value
        _selectedPaths.value = if (current.contains(path)) current - path else current + path
    }

    fun selectAll() { _selectedPaths.value = _items.value.map { it.path }.toSet() }
    fun clearSelection() { _selectedPaths.value = emptySet() }

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

    // --- Sil (çöp kutusuna taşı) / Yeniden adlandır / Oluştur ---

    fun deleteSelected() {
        val paths = _selectedPaths.value
        if (paths.isEmpty()) return
        viewModelScope.launch {
            var lastError: String? = null
            paths.forEach { path ->
                val result = recycleBinManager.moveToTrash(path)
                if (result is OperationResult.Error) lastError = result.message
            }
            clearSelection()
            _errorMessage.value = lastError
            _infoMessage.value = if (lastError == null) "Çöp kutusuna taşındı" else null
            refresh()
        }
    }

    fun deleteSingle(path: String) {
        viewModelScope.launch {
            when (val result = recycleBinManager.moveToTrash(path)) {
                is OperationResult.Success -> {
                    _infoMessage.value = "Çöp kutusuna taşındı"
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

    // --- Harici açma / Dosya bilgisi / Hash ---

    fun openExternally(path: String) {
        when (val result = fileOpener.openWithExternalApp(path)) {
            is OperationResult.Success -> Unit
            is OperationResult.Error -> _errorMessage.value = result.message
        }
    }

    fun loadFileInfo(path: String) {
        _hashResult.value = null
        viewModelScope.launch {
            when (val result = fileManager.getFileInfo(path)) {
                is OperationResult.Success -> _fileInfo.value = result.data
                is OperationResult.Error -> _errorMessage.value = result.message
            }
        }
    }

    fun clearFileInfo() {
        _fileInfo.value = null
        _hashResult.value = null
    }

    fun computeHash(path: String, algorithm: HashAlgorithm) {
        viewModelScope.launch {
            _isComputingHash.value = true
            _hashResult.value = null
            when (val result = fileManager.calculateHash(path, algorithm)) {
                is OperationResult.Success -> _hashResult.value = result.data
                is OperationResult.Error -> _errorMessage.value = result.message
            }
            _isComputingHash.value = false
        }
    }

    // --- Favoriler ---

    fun toggleFavorite(path: String) {
        viewModelScope.launch { favoritesManager.toggleFavorite(path) }
    }

    // --- Sıkıştırma / Çıkartma ---

    fun compressSelected(zipName: String) {
        val destinationDir = _currentPath.value ?: return
        val paths = _selectedPaths.value.toList()
        if (paths.isEmpty()) return
        val fileName = if (zipName.endsWith(".zip", ignoreCase = true)) zipName else "$zipName.zip"
        viewModelScope.launch {
            val destinationPath = "$destinationDir/$fileName"
            when (val result = archiveManager.compress(paths, destinationPath)) {
                is OperationResult.Success -> {
                    _infoMessage.value = "Sıkıştırıldı: $fileName"
                    clearSelection()
                    refresh()
                }
                is OperationResult.Error -> _errorMessage.value = result.message
            }
        }
    }

    fun extractArchive(path: String) {
        val parentDir = _currentPath.value ?: return
        val folderName = path.substringAfterLast('/').substringBeforeLast('.')
        viewModelScope.launch {
            val destinationPath = "$parentDir/$folderName"
            when (val result = archiveManager.extract(path, destinationPath)) {
                is OperationResult.Success -> {
                    _infoMessage.value = "Çıkartıldı: $folderName"
                    refresh()
                }
                is OperationResult.Error -> _errorMessage.value = result.message
            }
        }
    }

    fun consumeMessages() {
        _errorMessage.value = null
        _infoMessage.value = null
    }
}
