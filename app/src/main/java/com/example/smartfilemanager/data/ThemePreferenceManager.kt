package com.example.smartfilemanager.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore by preferencesDataStore(name = "theme_preferences")

enum class ThemeMode { LIGHT, DARK, SYSTEM }

/**
 * Kullanıcının Açık/Koyu/Sistem tema tercihini kalıcı olarak saklar.
 */
@Singleton
class ThemePreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val themeModeKey = stringPreferencesKey("theme_mode")

    val themeMode: Flow<ThemeMode> = context.themeDataStore.data.map { prefs ->
        prefs[themeModeKey]?.let { stored ->
            runCatching { ThemeMode.valueOf(stored) }.getOrNull()
        } ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { prefs -> prefs[themeModeKey] = mode.name }
    }
}
