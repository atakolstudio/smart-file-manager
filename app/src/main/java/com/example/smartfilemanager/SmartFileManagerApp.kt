package com.example.smartfilemanager

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Uygulama giriş noktası. Hilt bağımlılık enjeksiyon grafiğini burada başlatıyoruz.
 */
@HiltAndroidApp
class SmartFileManagerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Global başlatma işlemleri (crash handler, logging vb.) burada yapılabilir.
    }
}
