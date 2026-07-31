package com.example.smartfilemanager.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartfilemanager.R
import com.example.smartfilemanager.data.ThemeMode
import com.example.smartfilemanager.ui.AppLockViewModel
import com.example.smartfilemanager.ui.MainViewModel
import com.example.smartfilemanager.ui.theme.ApkColor
import com.example.smartfilemanager.ui.theme.AudioColor
import com.example.smartfilemanager.ui.theme.DocumentColor
import com.example.smartfilemanager.ui.theme.ImageColor

/**
 * Ayarlar ekranı. Görünüm (tema), Depolama Analizi, Geri Dönüşüm Kutusu ve
 * Uygulama Kilidi tercihi burada.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToRecycleBin: () -> Unit = {},
    onNavigateToStorageAnalysis: () -> Unit = {},
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel = hiltViewModel(),
    appLockViewModel: AppLockViewModel = hiltViewModel()
) {
    val themeMode by mainViewModel.themeMode.collectAsStateWithLifecycle()
    val isLockEnabled by appLockViewModel.isLockEnabled.collectAsStateWithLifecycle()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLockUnavailableDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(text = stringResource(id = R.string.nav_settings)) })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            SettingsRow(
                icon = Icons.Filled.Brightness6,
                iconColor = ImageColor,
                title = "Görünüm",
                subtitle = themeModeLabel(themeMode),
                onClick = { showThemeDialog = true }
            )
            Spacer(modifier = Modifier.padding(top = 12.dp))
            SettingsSwitchRow(
                icon = Icons.Filled.Fingerprint,
                iconColor = AudioColor,
                title = "Uygulama Kilidi",
                subtitle = "Parmak izi, yüz veya PIN ile aç",
                checked = isLockEnabled,
                onCheckedChange = { enabled ->
                    if (enabled && !appLockViewModel.isAuthenticationAvailable()) {
                        showLockUnavailableDialog = true
                    } else {
                        appLockViewModel.setLockEnabled(enabled)
                    }
                }
            )
            Spacer(modifier = Modifier.padding(top = 12.dp))
            SettingsRow(
                icon = Icons.Filled.BarChart,
                iconColor = DocumentColor,
                title = "Depolama Analizi",
                subtitle = "Yinelenen dosyalar, en büyük dosyalar, boş klasörler",
                onClick = onNavigateToStorageAnalysis
            )
            Spacer(modifier = Modifier.padding(top = 12.dp))
            SettingsRow(
                icon = Icons.Filled.DeleteSweep,
                iconColor = ApkColor,
                title = "Geri Dönüşüm Kutusu",
                subtitle = "Silinen dosyaları görüntüle, geri yükle",
                onClick = onNavigateToRecycleBin
            )
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Görünüm") },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    mainViewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = themeMode == mode, onClick = {
                                mainViewModel.setThemeMode(mode)
                                showThemeDialog = false
                            })
                            Text(text = themeModeLabel(mode), modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Kapat") }
            }
        )
    }

    if (showLockUnavailableDialog) {
        AlertDialog(
            onDismissRequest = { showLockUnavailableDialog = false },
            title = { Text("Kimlik Doğrulama Kurulu Değil") },
            text = { Text("Uygulama kilidini kullanabilmek için cihazınızda parmak izi, yüz tanıma veya PIN/desen kurulu olmalı. Lütfen önce cihaz ayarlarından bir kilit ekran yöntemi kurun.") },
            confirmButton = {
                TextButton(onClick = { showLockUnavailableDialog = false }) { Text("Tamam") }
            }
        )
    }
}

private fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.LIGHT -> "Açık"
    ThemeMode.DARK -> "Koyu"
    ThemeMode.SYSTEM -> "Sistem varsayılanı"
}

@Composable
private fun SettingsSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color = iconColor.copy(alpha = 0.16f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color = iconColor.copy(alpha = 0.16f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
