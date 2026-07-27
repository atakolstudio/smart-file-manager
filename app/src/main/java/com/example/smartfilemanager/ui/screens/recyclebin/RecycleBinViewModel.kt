package com.example.smartfilemanager.ui.screens.recyclebin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartfilemanager.data.OperationResult
import com.example.smartfilemanager.data.RecycleBinManager
import com.example.smartfilemanager.data.TrashEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecycleBinUiState(
    val entries: List<TrashEntry> = emptyList(),
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

/**
 * Çöp kutusundaki öğeleri listeler; geri yükleme, kalıcı silme ve kutuyu boşaltma işlemlerini yönetir.
 */
@HiltViewModel
class RecycleBinViewModel @Inject constructor(
    private val recycleBinManager: RecycleBinManager
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _infoMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<RecycleBinUiState> = combine(
        recycleBinManager.entries, _errorMessage, _infoMessage
    ) { entries, error, info ->
        RecycleBinUiState(entries = entries, errorMessage = error, infoMessage = info)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecycleBinUiState())

    fun restore(entry: TrashEntry) {
        viewModelScope.launch {
            when (val result = recycleBinManager.restore(entry)) {
                is OperationResult.Success -> _infoMessage.value = "Geri yüklendi"
                is OperationResult.Error -> _errorMessage.value = result.message
            }
        }
    }

    fun deleteForever(entry: TrashEntry) {
        viewModelScope.launch {
            when (val result = recycleBinManager.deleteForever(entry)) {
                is OperationResult.Success -> _infoMessage.value = "Kalıcı olarak silindi"
                is OperationResult.Error -> _errorMessage.value = result.message
            }
        }
    }

    fun emptyBin() {
        viewModelScope.launch {
            when (val result = recycleBinManager.emptyBin()) {
                is OperationResult.Success -> _infoMessage.value = "Çöp kutusu boşaltıldı"
                is OperationResult.Error -> _errorMessage.value = result.message
            }
        }
    }

    fun consumeMessages() {
        _errorMessage.value = null
        _infoMessage.value = null
    }
}
