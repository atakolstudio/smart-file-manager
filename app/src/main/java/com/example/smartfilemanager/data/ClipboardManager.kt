package com.example.smartfilemanager.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class ClipboardOperation { COPY, CUT }

data class ClipboardState(
    val paths: List<String>,
    val operation: ClipboardOperation
)

/**
 * Kopyala/Kes/Yapıştır akışını uygulama genelinde (farklı klasörler arasında geçiş
 * yapılsa bile) tutan pano yöneticisi. Her [com.example.smartfilemanager.ui.screens.files.FilesViewModel]
 * örneği aynı singleton'a erişir, böylece bir klasörde "Kes" yapılan dosyalar başka bir klasörde
 * "Yapıştır" ile taşınabilir.
 */
@Singleton
class ClipboardManager @Inject constructor() {

    private val _state = MutableStateFlow<ClipboardState?>(null)
    val state: StateFlow<ClipboardState?> = _state.asStateFlow()

    fun copy(paths: List<String>) {
        _state.value = ClipboardState(paths, ClipboardOperation.COPY)
    }

    fun cut(paths: List<String>) {
        _state.value = ClipboardState(paths, ClipboardOperation.CUT)
    }

    fun clear() {
        _state.value = null
    }
}
