package com.example.smartfilemanager.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartfilemanager.data.OperationResult
import com.example.smartfilemanager.data.StorageManager
import com.example.smartfilemanager.model.CategorySummary
import com.example.smartfilemanager.permission.PermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StorageSummary(
    val totalBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val freeBytes: Long = 0L
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val hasPermission: Boolean = false,
    val storageSummary: StorageSummary = StorageSummary(),
    val categorySummaries: List<CategorySummary> = emptyList(),
    val errorMessage: String? = null
)

/**
 * Ana sayfanın durumunu yönetir: depolama izni kontrolü, genel depolama özetini
 * ve [StorageManager] üzerinden gerçek kategori bazlı dosya sayısı/boyutlarını hesaplar.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val permissionManager: PermissionManager,
    private val storageManager: StorageManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val hasPermission = permissionManager.hasAllFilesAccess()
            if (!hasPermission) {
                _uiState.value = HomeUiState(isLoading = false, hasPermission = false)
                return@launch
            }

            val (total, free) = storageManager.getTotalAndFreeBytes()
            val storageSummary = StorageSummary(
                totalBytes = total,
                usedBytes = total - free,
                freeBytes = free
            )

            when (val result = storageManager.getCategorySummaries()) {
                is OperationResult.Success -> {
                    _uiState.value = HomeUiState(
                        isLoading = false,
                        hasPermission = true,
                        storageSummary = storageSummary,
                        categorySummaries = result.data
                    )
                }
                is OperationResult.Error -> {
                    _uiState.value = HomeUiState(
                        isLoading = false,
                        hasPermission = true,
                        storageSummary = storageSummary,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun directoryPathFor(categoryLabel: String): String? =
        storageManager.getCommonDirectories()[categoryLabel]?.absolutePath
}
