package com.example.smartfilemanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartfilemanager.data.ThemeMode
import com.example.smartfilemanager.data.ThemePreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val themePreferenceManager: ThemePreferenceManager
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = themePreferenceManager.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { themePreferenceManager.setThemeMode(mode) }
    }
}
