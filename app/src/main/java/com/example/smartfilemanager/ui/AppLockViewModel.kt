package com.example.smartfilemanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartfilemanager.data.AppLockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Uygulama kilidinin durumunu yönetir. [isUnlocked], her uygulama süreci (process) başına
 * bir kez sıfırlanır (session-scoped) — yani uygulama tamamen kapatılıp yeniden açıldığında
 * kilit tekrar devreye girer, ama aynı oturum içinde (ör. arka plana alıp geri dönme) tekrar
 * kimlik doğrulama istenmez. Bu, gerçek dosya yöneticilerindeki (Fossify vb.) standart davranıştır.
 */
@HiltViewModel
class AppLockViewModel @Inject constructor(
    private val appLockManager: AppLockManager
) : ViewModel() {

    val isLockEnabled: StateFlow<Boolean> = appLockManager.isEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked

    fun onUnlocked() {
        _isUnlocked.value = true
    }

    fun setLockEnabled(enabled: Boolean) {
        viewModelScope.launch { appLockManager.setEnabled(enabled) }
    }

    fun isAuthenticationAvailable(): Boolean = appLockManager.isAuthenticationAvailable()
}
