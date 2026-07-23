package com.example.smartfilemanager.ui.screens.home

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val errorMessage: String? = null
)

/**
 * Ana sayfanın durumunu yönetir: depolama izni kontrolü ve genel depolama özetini hesaplar.
 * Ağır dosya sistemi işlemleri ileriki aşamada StorageManager/FileManager üzerinden yapılacaktır.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val permissionManager: PermissionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val hasPermission = permissionManager.hasAllFilesAccess()
                val summary = if (hasPermission) computeStorageSummary() else StorageSummary()
                _uiState.value = HomeUiState(
                    isLoading = false,
                    hasPermission = hasPermission,
                    storageSummary = summary
                )
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = t.message
                )
            }
        }
    }

    private fun computeStorageSummary(): StorageSummary {
        val statFs = android.os.StatFs(Environment.getExternalStorageDirectory().path)
        val total = statFs.totalBytes
        val free = statFs.availableBytes
        return StorageSummary(
            totalBytes = total,
            usedBytes = total - free,
            freeBytes = free
        )
    }
}
