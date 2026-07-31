package com.example.smartfilemanager.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartfilemanager.data.HomeCacheManager
import com.example.smartfilemanager.data.OperationResult
import com.example.smartfilemanager.data.StorageManager
import com.example.smartfilemanager.model.CategorySummary
import com.example.smartfilemanager.model.RecentFileEntry
import com.example.smartfilemanager.permission.PermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class StorageSummary(
    val totalBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val freeBytes: Long = 0L
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val isRescanning: Boolean = false,
    val hasPermission: Boolean = false,
    val storageSummary: StorageSummary = StorageSummary(),
    val categorySummaries: List<CategorySummary> = emptyList(),
    val recentFiles: List<RecentFileEntry> = emptyList(),
    val lastScannedAtMillis: Long? = null,
    val errorMessage: String? = null,
    val progressStep: String? = null
)

/**
 * Ana sayfanın durumunu yönetir: depolama izni kontrolü, genel depolama özetini
 * ve [StorageManager] üzerinden gerçek kategori bazlı dosya sayısı/boyutlarını hesaplar.
 *
 * ÖNEMLİ (performans): Bazı cihazlarda (çok sayıda dosya + Android'in depolama emülasyon
 * katmanının getirdiği ek gecikme yüzünden) tam tarama dakikalar sürebiliyor. Bu yüzden:
 * - Önbellekte önceki bir tarama sonucu varsa, ekran AÇILIR AÇILMAZ o sonuç gösterilir
 *   (yeniden taramadan, neredeyse anında).
 * - Yeni bir tarama yalnızca önbellek hiç yoksa (ilk açılış) otomatik başlar.
 * - Kullanıcı ne zaman isterse [refresh] ile elle yeniden tarayabilir (arka planda,
 *   ekranı kilitlemeden — mevcut veriler tarama bitene kadar görünür kalır).
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val permissionManager: PermissionManager,
    private val storageManager: StorageManager,
    private val homeCacheManager: HomeCacheManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null
    private var hasLoadedOnce = false

    companion object {
        /** Son çare üst sınır: hangi sebepten olursa olsun (beklenmedik bir donma dahil),
         * bir tarama asla bu süreden fazla sürmez. */
        private const val OVERALL_SCAN_TIMEOUT_MS = 60_000L
        private const val MAX_RECENT_FILES = 10
    }

    /**
     * Ekran her göründüğünde (ör. onResume) çağrılır. İlk çağrıda önbelleği yükler ve
     * önbellek yoksa taramayı başlatır. Sonraki çağrılarda sadece izin durumunu günceller,
     * YENİDEN TARAMAZ — kullanıcı bunu [forceRescan] ile elle tetikleyebilir.
     */
    fun refresh() {
        val hasPermission = permissionManager.hasAllFilesAccess()
        _uiState.value = _uiState.value.copy(hasPermission = hasPermission)

        if (!hasPermission) {
            _uiState.value = _uiState.value.copy(isLoading = false)
            return
        }

        // ÖNEMLİ: Bu koruma yalnızca izin GERÇEKTEN verildiğinde devreye girer. Daha önce
        // burada izin reddedilmişken bile "yüklendi" işaretleniyordu; kullanıcı sonradan
        // izni verip geri döndüğünde gerçek önbellek/tarama mantığı hiç çalışmıyor,
        // ekran sıfır/boş veriyle kalıyordu. Şimdi yalnızca izinliyken bir kez çalışır.
        if (hasLoadedOnce) return
        hasLoadedOnce = true

        viewModelScope.launch {
            val cached = homeCacheManager.loadCache()
            if (cached != null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    storageSummary = StorageSummary(cached.totalBytes, cached.usedBytes, cached.freeBytes),
                    categorySummaries = cached.categorySummaries,
                    recentFiles = cached.recentFiles,
                    lastScannedAtMillis = cached.scannedAtMillis
                )
            } else {
                startScan()
            }
        }
    }

    /** Kullanıcının elle tetiklediği yeniden tarama (ör. bir "Yenile" butonu ile). */
    fun forceRescan() {
        startScan()
    }

    private fun startScan() {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = _uiState.value.categorySummaries.isEmpty(),
                isRescanning = _uiState.value.categorySummaries.isNotEmpty(),
                errorMessage = null,
                progressStep = "Başlıyor..."
            )

            val completed = withTimeoutOrNull(OVERALL_SCAN_TIMEOUT_MS) {
                val hasPermission = permissionManager.hasAllFilesAccess()
                if (!hasPermission) {
                    _uiState.value = _uiState.value.copy(isLoading = false, isRescanning = false, hasPermission = false)
                    return@withTimeoutOrNull
                }

                _uiState.value = _uiState.value.copy(progressStep = "Depolama boyutu hesaplanıyor...")
                val (total, free) = storageManager.getTotalAndFreeBytes()
                val storageSummary = StorageSummary(
                    totalBytes = total,
                    usedBytes = total - free,
                    freeBytes = free
                )

                val recentCandidates = mutableListOf<RecentFileEntry>()
                val result = storageManager.getCategorySummaries(
                    onProgress = { directoryName ->
                        _uiState.value = _uiState.value.copy(progressStep = "Taranıyor: $directoryName...")
                    },
                    onFileVisited = { file ->
                        recentCandidates += RecentFileEntry(
                            path = file.absolutePath,
                            name = file.name,
                            lastModified = file.lastModified(),
                            sizeBytes = file.length()
                        )
                    }
                )
                val recentFiles = recentCandidates
                    .sortedByDescending { it.lastModified }
                    .take(MAX_RECENT_FILES)

                when (result) {
                    is OperationResult.Success -> {
                        homeCacheManager.saveCache(total, total - free, free, result.data, recentFiles)
                        _uiState.value = HomeUiState(
                            isLoading = false,
                            isRescanning = false,
                            hasPermission = true,
                            storageSummary = storageSummary,
                            categorySummaries = result.data,
                            recentFiles = recentFiles,
                            lastScannedAtMillis = System.currentTimeMillis()
                        )
                    }
                    is OperationResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isRescanning = false,
                            hasPermission = true,
                            storageSummary = storageSummary,
                            errorMessage = result.message
                        )
                    }
                }
            }

            if (completed == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRescanning = false,
                    errorMessage = "Tarama çok uzun sürdü ve durduruldu. Tekrar deneyebilirsiniz."
                )
            }
        }
    }

    fun directoryPathFor(categoryLabel: String): String? =
        storageManager.getCommonDirectories()[categoryLabel]?.absolutePath
}
