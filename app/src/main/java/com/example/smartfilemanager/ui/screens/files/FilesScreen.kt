package com.example.smartfilemanager.ui.screens.files

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Dosya listeleme ekranı. Gerçek dosya listeleme, sıralama ve çoklu seçim
 * mantığı Aşama 3'te (Dosya Listeleme + Temel İşlemler) eklenecektir.
 */
@Composable
fun FilesScreen(
    path: String?,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(text = path ?: "Dosyalar") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Dosya listeleme Aşama 3'te eklenecek",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
