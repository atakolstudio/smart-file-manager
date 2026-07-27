package com.example.smartfilemanager.ui.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartfilemanager.data.FavoritesManager
import com.example.smartfilemanager.model.FileItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class FavoritesUiState(
    val items: List<FileItem> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * Favori olarak işaretlenmiş dosya/klasörleri listeler. Artık var olmayan (silinmiş)
 * favoriler otomatik olarak listeden düşürülür.
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesManager: FavoritesManager
) : ViewModel() {

    val uiState: StateFlow<FavoritesUiState> = favoritesManager.favoritePaths
        .map { paths ->
            val items = paths
                .map { File(it) }
                .filter { it.exists() }
                .map { FileItem.fromFile(it) }
                .sortedBy { it.name.lowercase() }
            FavoritesUiState(items = items, isLoading = false)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FavoritesUiState())

    fun removeFavorite(path: String) {
        viewModelScope.launch { favoritesManager.removeFavorite(path) }
    }
}
