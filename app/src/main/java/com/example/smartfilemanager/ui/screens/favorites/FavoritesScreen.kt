package com.example.smartfilemanager.ui.screens.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartfilemanager.R
import com.example.smartfilemanager.model.FileItem
import com.example.smartfilemanager.util.IconProvider
import com.example.smartfilemanager.util.MimeTypeHelper
import com.example.smartfilemanager.util.SizeFormatter

/**
 * Favori olarak işaretlenmiş dosya/klasörleri listeler. Bir klasöre dokunmak Dosyalar
 * ekranında o klasörü açar; bir dosyaya dokunmak (desteklenen türlerde) önizleme açar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onOpenFolder: (String) -> Unit,
    onOpenPreview: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(text = stringResource(id = R.string.nav_favorites)) })
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { CircularProgressIndicator() }

            uiState.items.isEmpty() -> Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    text = "Henüz favori eklenmedi",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                items(uiState.items, key = { it.path }) { item ->
                    FavoriteRow(
                        item = item,
                        onClick = {
                            if (item.isDirectory) onOpenFolder(item.path) else onOpenPreview(item.path)
                        },
                        onRemove = { viewModel.removeFavorite(item.path) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteRow(item: FileItem, onClick: () -> Unit, onRemove: () -> Unit) {
    val category = MimeTypeHelper.getCategory(item.name, item.isDirectory)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = IconProvider.iconFor(category), contentDescription = null, tint = IconProvider.colorFor(category))
        Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
            Text(text = item.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val subtitle = if (item.isDirectory) "Klasör" else SizeFormatter.format(item.sizeBytes)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Star, contentDescription = "Favorilerden çıkar", tint = MaterialTheme.colorScheme.primary)
        }
    }
}
