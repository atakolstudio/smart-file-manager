package com.example.smartfilemanager.ui.screens.analysis

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartfilemanager.model.CategorySummary
import com.example.smartfilemanager.model.DuplicateGroup
import com.example.smartfilemanager.model.EmptyFolderEntry
import com.example.smartfilemanager.model.LargeFileEntry
import com.example.smartfilemanager.util.IconProvider
import com.example.smartfilemanager.util.SizeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageAnalysisScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StorageAnalysisViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var confirmAction by remember { mutableStateOf<ConfirmAction?>(null) }

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
                title = { Text("Depolama Analizi") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri") }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isScanning -> Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text("Taranıyor (en fazla birkaç saniye)...", style = MaterialTheme.typography.bodyMedium)
            }

            !uiState.hasScanned -> Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Resimler, videolar, müzik, belgeler ve indirilenler klasörlerinizi tarayarak en büyük dosyaları, yinelenenleri ve boş klasörleri bulur.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(onClick = { viewModel.startScan() }) { Text("Taramayı Başlat") }
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { CategoryPieCard(uiState.result.categorySummaries) }

                item {
                    OutlinedButton(onClick = { viewModel.clearAppCache() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.padding(start = 6.dp))
                        Text("Uygulama Önbelleğini Temizle")
                    }
                }

                if (uiState.result.duplicateGroups.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Yinelenen Dosyalar (${uiState.result.duplicateGroups.size} grup)",
                            actionLabel = "Temizle",
                            onAction = { confirmAction = ConfirmAction.RemoveDuplicates }
                        )
                    }
                    items(uiState.result.duplicateGroups, key = { it.paths.first() }) { group ->
                        DuplicateGroupRow(group)
                    }
                }

                if (uiState.result.emptyFolders.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Boş Klasörler (${uiState.result.emptyFolders.size})",
                            actionLabel = "Temizle",
                            onAction = { confirmAction = ConfirmAction.DeleteEmptyFolders }
                        )
                    }
                    items(uiState.result.emptyFolders, key = { it.path }) { folder ->
                        EmptyFolderRow(folder)
                    }
                }

                if (uiState.result.largestFiles.isNotEmpty()) {
                    item { SectionHeader(title = "En Büyük Dosyalar") }
                    items(uiState.result.largestFiles, key = { it.path }) { file ->
                        LargeFileRow(file)
                    }
                }

                item {
                    TextButton(onClick = { viewModel.startScan() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Yeniden Tara")
                    }
                }
            }
        }
    }

    confirmAction?.let { action ->
        val (title, message, onConfirm) = when (action) {
            ConfirmAction.RemoveDuplicates -> Triple(
                "Yinelenenleri Temizle",
                "Her grupta bir kopya bırakılıp diğerleri çöp kutusuna taşınacak. Devam edilsin mi?",
                { viewModel.removeDuplicates() }
            )
            ConfirmAction.DeleteEmptyFolders -> Triple(
                "Boş Klasörleri Sil",
                "Tespit edilen boş klasörler kalıcı olarak silinecek. Devam edilsin mi?",
                { viewModel.deleteEmptyFolders() }
            )
        }
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { onConfirm(); confirmAction = null }) { Text("Onayla") }
            },
            dismissButton = {
                TextButton(onClick = { confirmAction = null }) { Text("Vazgeç") }
            }
        )
    }
}

private enum class ConfirmAction { RemoveDuplicates, DeleteEmptyFolders }

@Composable
private fun CategoryPieCard(summaries: List<CategorySummary>) {
    val nonEmpty = summaries.filter { it.totalSizeBytes > 0 }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Kategori Dağılımı", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            if (nonEmpty.isEmpty()) {
                Text("Taranan klasörlerde dosya bulunamadı", style = MaterialTheme.typography.bodyMedium)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PieChart(nonEmpty, modifier = Modifier.size(96.dp))
                    Spacer(modifier = Modifier.padding(start = 20.dp))
                    Column {
                        nonEmpty.forEach { summary ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(IconProvider.colorFor(summary.category), CircleShape)
                                )
                                Spacer(modifier = Modifier.padding(start = 6.dp))
                                Text(
                                    text = "${categoryLabel(summary.category)} (${SizeFormatter.format(summary.totalSizeBytes)})",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PieChart(summaries: List<CategorySummary>, modifier: Modifier = Modifier) {
    val total = summaries.sumOf { it.totalSizeBytes }.toFloat().coerceAtLeast(1f)
    Canvas(modifier = modifier) {
        var startAngle = -90f
        val diameter = size.minDimension
        val topLeft = androidx.compose.ui.geometry.Offset(
            (size.width - diameter) / 2f,
            (size.height - diameter) / 2f
        )
        summaries.forEach { summary ->
            val sweep = (summary.totalSizeBytes / total) * 360f
            drawArc(
                color = IconProvider.colorFor(summary.category),
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = true,
                topLeft = topLeft,
                size = Size(diameter, diameter)
            )
            startAngle += sweep
        }
    }
}

private fun categoryLabel(category: com.example.smartfilemanager.model.FileCategory): String = when (category) {
    com.example.smartfilemanager.model.FileCategory.IMAGE -> "Resimler"
    com.example.smartfilemanager.model.FileCategory.VIDEO -> "Videolar"
    com.example.smartfilemanager.model.FileCategory.AUDIO -> "Müzik"
    com.example.smartfilemanager.model.FileCategory.DOCUMENT -> "Belgeler"
    com.example.smartfilemanager.model.FileCategory.ARCHIVE -> "Arşivler"
    com.example.smartfilemanager.model.FileCategory.APK -> "APK"
    com.example.smartfilemanager.model.FileCategory.OTHER -> "Diğer"
    com.example.smartfilemanager.model.FileCategory.FOLDER -> "Klasör"
}

@Composable
private fun SectionHeader(title: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun DuplicateGroupRow(group: DuplicateGroup) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "${group.paths.size} kopya • ${SizeFormatter.format(group.sizeBytes)} / adet",
                style = MaterialTheme.typography.bodyMedium
            )
            group.paths.forEach { path ->
                Text(
                    text = path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptyFolderRow(folder: EmptyFolderEntry) {
    Text(
        text = folder.path,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}

@Composable
private fun LargeFileRow(file: LargeFileEntry) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = file.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(text = SizeFormatter.format(file.sizeBytes), style = MaterialTheme.typography.bodyMedium)
    }
}
