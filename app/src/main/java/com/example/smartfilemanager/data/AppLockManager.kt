package com.example.smartfilemanager.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appLockDataStore by preferencesDataStore(name = "app_lock")

/**
 * Uygulama kilidinin (parmak izi/yüz/PIN ile açma) açık olup olmadığını kalıcı olarak saklar.
 * Gerçek kimlik doğrulama [androidx.biometric.BiometricPrompt] ile yapılır — bu sınıf yalnızca
 * kullanıcı tercihini tutar.
 */
@Singleton
class AppLockManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val enabledKey = booleanPreferencesKey("app_lock_enabled")

    val isEnabled: Flow<Boolean> = context.appLockDataStore.data.map { prefs ->
        prefs[enabledKey] ?: false
    }

    suspend fun setEnabled(enabled: Boolean) {
        context.appLockDataStore.edit { prefs -> prefs[enabledKey] = enabled }
    }

    /** Cihazda parmak izi/yüz/PIN gibi en az bir kimlik doğrulama yöntemi kurulu mu kontrol eder. */
    fun isAuthenticationAvailable(): Boolean {
        val biometricManager = androidx.biometric.BiometricManager.from(context)
        val result = biometricManager.canAuthenticate(
            androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK or
                androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        return result == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
    }
}
