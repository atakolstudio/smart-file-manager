package com.example.smartfilemanager.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.favoritesDataStore by preferencesDataStore(name = "favorites")

/**
 * Favori dosya/klasör yollarını DataStore Preferences üzerinde kalıcı olarak saklar.
 */
@Singleton
class FavoritesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val favoritesKey = stringSetPreferencesKey("favorite_paths")

    val favoritePaths: Flow<Set<String>> = context.favoritesDataStore.data.map { prefs ->
        prefs[favoritesKey] ?: emptySet()
    }

    suspend fun toggleFavorite(path: String) {
        context.favoritesDataStore.edit { prefs ->
            val current = prefs[favoritesKey] ?: emptySet()
            prefs[favoritesKey] = if (current.contains(path)) current - path else current + path
        }
    }

    suspend fun removeFavorite(path: String) {
        context.favoritesDataStore.edit { prefs ->
            val current = prefs[favoritesKey] ?: emptySet()
            prefs[favoritesKey] = current - path
        }
    }

    suspend fun isFavorite(path: String): Boolean = favoritePaths.first().contains(path)

    suspend fun clearAll() {
        context.favoritesDataStore.edit { prefs -> prefs[favoritesKey] = emptySet() }
    }
}
