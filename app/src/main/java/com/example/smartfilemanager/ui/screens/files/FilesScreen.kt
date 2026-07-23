package com.example.smartfilemanager.ui.screens.files

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartfilemanager.model.FileItem
import com.example.smartfilemanager.util.IconProvider
import com.example.smartfilemanager.util.MimeTypeHelper
import com.example.smartfilemanager.util.SizeFormatter

/**
 * Belirtilen klasörün içeriğini listeler. Kopyala/kes/sil/yeniden adlandırma gibi
 * düzenleme işlemleri Aşama 3'te bu ekrana eklenecektir; şu an salt-okunur listelemedir.
 */
@Composable
fun FilesScreen(
    path: String?,
    modifier: Modifier = Modifier,
    viewModel: FilesViewModel = hiltViewModel(),
    onOpenFolder: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(path) {
        path?.let { viewModel.load(it) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(text = path?.substringAfterLast('/') ?: "Dosyalar") })
        }
    ) { paddingValues ->
        when {
            path == null -> EmptyMessage(paddingValues, "Görüntülenecek bir klasör seçilmedi")
            uiState.isLoading -> LoadingContent(paddingValues)
            uiState.errorMessage != null -> EmptyMessage(paddingValues, uiState.errorMessage!!)
            uiState.items.isEmpty() -> EmptyMessage(paddingValues, "Bu klasör boş")
            else -> FileList(
                paddingValues = paddingValues,
                items = uiState.items,
                onItemClick = { item ->
                    if (item.isDirectory) onOpenFolder(item.path)
                }
            )
        }
    }
}

@Composable
private fun LoadingContent(paddingValues: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyMessage(paddingValues: PaddingValues, message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun FileList(
    paddingValues: PaddingValues,
    items: List<FileItem>,
    onItemClick: (FileItem) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
        items(items, key = { it.path }) { item ->
            FileRow(item = item, onClick = { onItemClick(item) })
        }
    }
}

@Composable
private fun FileRow(item: FileItem, onClick: () -> Unit) {
    val category = MimeTypeHelper.getCategory(item.name, item.isDirectory)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = IconProvider.iconFor(category),
            contentDescription = null,
            tint = IconProvider.colorFor(category)
        )
        Column(modifier = Modifier.padding(start = 16.dp).fillMaxWidth()) {
            Text(text = item.name, style = MaterialTheme.typography.bodyLarge)
            val subtitle = if (item.isDirectory) "Klasör" else SizeFormatter.format(item.sizeBytes)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
