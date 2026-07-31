package com.example.smartfilemanager.ui.screens.recyclebin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartfilemanager.data.TrashEntry
import com.example.smartfilemanager.util.DateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecycleBinViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showEmptyConfirm by remember { mutableStateOf(false) }
    var forgetTarget by remember { mutableStateOf<TrashEntry?>(null) }

    LaunchedEffect(uiState.errorMessage, uiState.infoMessage) {
        val message = uiState.errorMessage ?: uiState.infoMessage
        if (message != null) {
            coroutineScope.launch { snackbarHostState.showSnackbar(message) }
            viewModel.consumeMessages()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Geri Dönüşüm Kutusu") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri") }
                },
                actions = {
                    if (uiState.entries.isNotEmpty()) {
                        IconButton(onClick = { showEmptyConfirm = true }) {
                            Icon(Icons.Filled.DeleteForever, contentDescription = "Kutuyu Boşalt")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.entries.isEmpty()) {
            com.example.smartfilemanager.ui.components.EmptyState(
                icon = Icons.Filled.Delete,
                title = "Çöp kutusu boş",
                subtitle = "Sildiğiniz dosyalar burada görünür ve geri yükleyebilirsiniz",
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                items(uiState.entries, key = { it.trashPath }) { entry ->
                    TrashRow(
                        entry = entry,
                        onRestore = { viewModel.restore(entry) },
                        onDeleteForever = { forgetTarget = entry }
                    )
                }
            }
        }
    }

    if (showEmptyConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyConfirm = false },
            title = { Text("Kutuyu Boşalt") },
            text = { Text("Çöp kutusundaki tüm öğeler kalıcı olarak silinecek. Emin misiniz?") },
            confirmButton = {
                TextButton(onClick = { viewModel.emptyBin(); showEmptyConfirm = false }) { Text("Boşalt") }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyConfirm = false }) { Text("Vazgeç") }
            }
        )
    }

    forgetTarget?.let { entry ->
        AlertDialog(
            onDismissRequest = { forgetTarget = null },
            title = { Text("Kalıcı Sil") },
            text = { Text("\"${entry.name}\" kalıcı olarak silinsin mi? Bu işlem geri alınamaz.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteForever(entry); forgetTarget = null }) { Text("Sil") }
            },
            dismissButton = {
                TextButton(onClick = { forgetTarget = null }) { Text("Vazgeç") }
            }
        )
    }
}

@Composable
private fun TrashRow(entry: TrashEntry, onRestore: () -> Unit, onDeleteForever: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
            Text(text = entry.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = "Silindi: ${DateUtils.formatTimestamp(entry.deletedAt)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onRestore) {
            Icon(Icons.Filled.Restore, contentDescription = "Geri Yükle")
        }
        IconButton(onClick = onDeleteForever) {
            Icon(Icons.Filled.DeleteForever, contentDescription = "Kalıcı Sil")
        }
    }
}
