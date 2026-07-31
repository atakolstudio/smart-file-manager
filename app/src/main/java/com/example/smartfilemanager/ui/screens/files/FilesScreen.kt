package com.example.smartfilemanager.ui.screens.files

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartfilemanager.model.FileItem
import com.example.smartfilemanager.util.DateUtils
import com.example.smartfilemanager.util.IconProvider
import com.example.smartfilemanager.util.MimeTypeHelper
import com.example.smartfilemanager.util.SizeFormatter
import kotlinx.coroutines.launch

private val previewableTextExtensions = setOf("txt", "md", "json", "xml", "html", "csv", "log", "kt", "java", "gradle", "properties")
private val previewableImageExtensions = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif")

private fun isPreviewableInApp(item: FileItem): Boolean {
    val extension = item.extension.lowercase()
    return extension == "pdf" || extension in previewableImageExtensions || extension in previewableTextExtensions
}

private enum class ViewMode { LIST, GRID }

/**
 * Klasör içeriğini listeler; arama, sıralama, kopyala/kes/yapıştır/sil/yeniden adlandırma,
 * çoklu seçim, önizleme yönlendirmesi, dosya bilgisi ve liste/ızgara görünüm değiştirmeyi destekler.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    path: String?,
    modifier: Modifier = Modifier,
    viewModel: FilesViewModel = hiltViewModel(),
    onOpenFolder: (String) -> Unit = {},
    onOpenPreview: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val fileInfo by viewModel.fileInfo.collectAsStateWithLifecycle()
    val hashResult by viewModel.hashResult.collectAsStateWithLifecycle()
    val isComputingHash by viewModel.isComputingHash.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var isSearchActive by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(ViewMode.LIST) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showCompressDialog by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<FileItem?>(null) }
    var deleteTarget by remember { mutableStateOf<FileItem?>(null) }

    LaunchedEffect(path) {
        path?.let { viewModel.load(it) }
    }

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
            when {
                uiState.isSelectionMode -> SelectionTopBar(
                    selectedCount = uiState.selectedPaths.size,
                    onClose = viewModel::clearSelection,
                    onSelectAll = viewModel::selectAll,
                    onCopy = viewModel::copySelectionToClipboard,
                    onCut = viewModel::cutSelectionToClipboard,
                    onDelete = viewModel::deleteSelected,
                    onCompress = { showCompressDialog = true },
                    onShare = viewModel::shareSelected
                )
                isSearchActive -> SearchTopBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::setSearchQuery,
                    onClose = {
                        isSearchActive = false
                        viewModel.setSearchQuery("")
                    }
                )
                else -> NormalTopBar(
                    title = path?.substringAfterLast('/') ?: "Dosyalar",
                    onSearchClick = { isSearchActive = true },
                    onSortClick = { showSortMenu = true },
                    showSortMenu = showSortMenu,
                    onSortMenuDismiss = { showSortMenu = false },
                    currentSort = uiState.sortOption,
                    sortAscending = uiState.sortAscending,
                    onSortOptionSelected = { option ->
                        viewModel.setSortOption(option)
                        showSortMenu = false
                    },
                    onToggleDirection = viewModel::toggleSortDirection,
                    viewMode = viewMode,
                    onToggleViewMode = {
                        viewMode = if (viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
                    }
                )
            }
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode && path != null) {
                Box {
                    FloatingActionButton(onClick = { showFabMenu = true }) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = "Yeni")
                    }
                    DropdownMenu(expanded = showFabMenu, onDismissRequest = { showFabMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Yeni Klasör") },
                            leadingIcon = { Icon(Icons.Filled.CreateNewFolder, contentDescription = null) },
                            onClick = { showFabMenu = false; showNewFolderDialog = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Yeni Dosya") },
                            leadingIcon = { Icon(Icons.Filled.NoteAdd, contentDescription = null) },
                            onClick = { showFabMenu = false; showNewFileDialog = true }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (path != null && !uiState.isSelectionMode && !isSearchActive) {
                BreadcrumbRow(path = path, onNavigateToPath = onOpenFolder)
            }
            Box(modifier = Modifier.weight(1f)) {
                when {
                    path == null -> com.example.smartfilemanager.ui.components.EmptyState(
                        icon = Icons.Filled.Folder,
                        title = "Görüntülenecek bir klasör seçilmedi"
                    )
                    uiState.isLoading -> LoadingContent()
                    uiState.displayedItems.isEmpty() && uiState.searchQuery.isNotBlank() ->
                        com.example.smartfilemanager.ui.components.EmptyState(
                            icon = Icons.Filled.SearchOff,
                            title = "\"${uiState.searchQuery}\" ile eşleşen sonuç yok"
                        )
                    uiState.displayedItems.isEmpty() -> com.example.smartfilemanager.ui.components.EmptyState(
                        icon = Icons.Filled.FolderOpen,
                        title = "Bu klasör boş"
                    )
                    viewMode == ViewMode.GRID -> FileGrid(
                        items = uiState.displayedItems,
                        selectedPaths = uiState.selectedPaths,
                        isSelectionMode = uiState.isSelectionMode,
                        onItemClick = { item ->
                            when {
                                uiState.isSelectionMode -> viewModel.toggleSelection(item.path)
                                item.isDirectory -> onOpenFolder(item.path)
                                isPreviewableInApp(item) -> onOpenPreview(item.path)
                                else -> viewModel.openExternally(item.path)
                            }
                        },
                        onItemLongClick = { item -> viewModel.toggleSelection(item.path) }
                    )
                    else -> FileList(
                        items = uiState.displayedItems,
                        selectedPaths = uiState.selectedPaths,
                        favoritePaths = uiState.favoritePaths,
                        isSelectionMode = uiState.isSelectionMode,
                        onItemClick = { item ->
                            when {
                                uiState.isSelectionMode -> viewModel.toggleSelection(item.path)
                                item.isDirectory -> onOpenFolder(item.path)
                                isPreviewableInApp(item) -> onOpenPreview(item.path)
                                else -> viewModel.openExternally(item.path)
                            }
                        },
                        onItemLongClick = { item -> viewModel.toggleSelection(item.path) },
                        onRenameClick = { item -> renameTarget = item },
                        onDeleteClick = { item -> deleteTarget = item },
                        onInfoClick = { item -> viewModel.loadFileInfo(item.path) },
                        onOpenExternallyClick = { item -> viewModel.openExternally(item.path) },
                        onShareClick = { item -> viewModel.shareFile(item.path) },
                        onToggleFavoriteClick = { item -> viewModel.toggleFavorite(item.path) },
                        onExtractClick = { item -> viewModel.extractArchive(item.path) }
                    )
                }
            }

            if (uiState.hasClipboardContent && !uiState.isSelectionMode) {
                PasteBar(count = uiState.clipboardCount, onPaste = viewModel::pasteFromClipboard)
            }
        }
    }

    if (showNewFolderDialog) {
        NameInputDialog(
            title = "Yeni Klasör",
            label = "Klasör adı",
            onConfirm = { name -> viewModel.createFolder(name); showNewFolderDialog = false },
            onDismiss = { showNewFolderDialog = false }
        )
    }

    if (showNewFileDialog) {
        NameInputDialog(
            title = "Yeni Dosya",
            label = "Dosya adı",
            onConfirm = { name -> viewModel.createFile(name); showNewFileDialog = false },
            onDismiss = { showNewFileDialog = false }
        )
    }

    renameTarget?.let { item ->
        NameInputDialog(
            title = "Yeniden Adlandır",
            label = "Yeni ad",
            initialValue = item.name,
            onConfirm = { name -> viewModel.rename(item.path, name); renameTarget = null },
            onDismiss = { renameTarget = null }
        )
    }

    deleteTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Sil") },
            text = { Text("\"${item.name}\" silinsin mi? Bu işlem geri alınamaz.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteSingle(item.path); deleteTarget = null }) { Text("Sil") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Vazgeç") }
            }
        )
    }

    if (showCompressDialog) {
        NameInputDialog(
            title = "Sıkıştır",
            label = "Zip dosya adı",
            initialValue = "arsiv",
            onConfirm = { name -> viewModel.compressSelected(name); showCompressDialog = false },
            onDismiss = { showCompressDialog = false }
        )
    }

    fileInfo?.let { info ->
        FileInfoDialog(
            info = info,
            hashResult = hashResult,
            isComputingHash = isComputingHash,
            onComputeHash = { algorithm -> viewModel.computeHash(info.path, algorithm) },
            onDismiss = { viewModel.clearFileInfo() }
        )
    }
}

@Composable
private fun BreadcrumbRow(path: String, onNavigateToPath: (String) -> Unit) {
    // "/storage/emulated/0" kök olarak "Dahili Depolama" ismiyle gösterilir; sonraki
    // segmentler tıklanabilir, kullanıcı bir üst klasöre tek dokunuşla atlayabilir.
    val internalStorageRoot = "/storage/emulated/0"
    val relativePath = path.removePrefix(internalStorageRoot).trim('/')
    val segments = if (relativePath.isEmpty()) emptyList() else relativePath.split('/')

    val crumbs = buildList {
        add("Dahili Depolama" to internalStorageRoot)
        var accumulated = internalStorageRoot
        segments.forEach { segment ->
            accumulated = "$accumulated/$segment"
            add(segment to accumulated)
        }
    }

    androidx.compose.foundation.lazy.LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        itemsIndexed(crumbs) { index, (label, crumbPath) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                val isLast = index == crumbs.lastIndex
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isLast) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clickable(enabled = !isLast) { onNavigateToPath(crumbPath) }
                        .padding(vertical = 6.dp, horizontal = 4.dp)
                )
                if (!isLast) {
                    Text(
                        text = "/",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NormalTopBar(
    title: String,
    onSearchClick: () -> Unit,
    onSortClick: () -> Unit,
    showSortMenu: Boolean,
    onSortMenuDismiss: () -> Unit,
    currentSort: SortOption,
    sortAscending: Boolean,
    onSortOptionSelected: (SortOption) -> Unit,
    onToggleDirection: () -> Unit,
    viewMode: ViewMode,
    onToggleViewMode: () -> Unit
) {
    TopAppBar(
        title = { Text(text = title) },
        actions = {
            IconButton(onClick = onToggleViewMode) {
                Icon(
                    if (viewMode == ViewMode.LIST) Icons.Filled.GridView else Icons.AutoMirrored.Filled.ViewList,
                    contentDescription = if (viewMode == ViewMode.LIST) "Izgara görünümü" else "Liste görünümü"
                )
            }
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Filled.Search, contentDescription = "Ara")
            }
            Box {
                IconButton(onClick = onSortClick) {
                    Icon(Icons.Filled.Sort, contentDescription = "Sırala")
                }
                DropdownMenu(expanded = showSortMenu, onDismissRequest = onSortMenuDismiss) {
                    SortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(sortOptionLabel(option)) },
                            leadingIcon = {
                                RadioButton(selected = currentSort == option, onClick = { onSortOptionSelected(option) })
                            },
                            onClick = { onSortOptionSelected(option) }
                        )
                    }
                    androidx.compose.material3.HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(if (sortAscending) "Artan" else "Azalan") },
                        onClick = onToggleDirection
                    )
                }
            }
        }
    )
}

private fun sortOptionLabel(option: SortOption): String = when (option) {
    SortOption.NAME -> "Ada göre"
    SortOption.SIZE -> "Boyuta göre"
    SortOption.DATE -> "Tarihe göre"
    SortOption.TYPE -> "Türe göre"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    TopAppBar(
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Dosya ara...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Aramayı kapat")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(
    selectedCount: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit,
    onCompress: () -> Unit,
    onShare: () -> Unit
) {
    var showMoreMenu by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text("$selectedCount seçildi") },
        navigationIcon = {
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Seçimi kapat") }
        },
        actions = {
            IconButton(onClick = onSelectAll) { Icon(Icons.Filled.Check, contentDescription = "Tümünü seç") }
            IconButton(onClick = onCopy) { Icon(Icons.Filled.ContentCopy, contentDescription = "Kopyala") }
            IconButton(onClick = onCut) { Icon(Icons.Filled.ContentCut, contentDescription = "Kes") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Sil") }
            Box {
                IconButton(onClick = { showMoreMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Diğer işlemler")
                }
                DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Paylaş") },
                        leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                        onClick = { showMoreMenu = false; onShare() }
                    )
                    DropdownMenuItem(
                        text = { Text("Sıkıştır") },
                        leadingIcon = { Icon(Icons.Filled.FolderZip, contentDescription = null) },
                        onClick = { showMoreMenu = false; onCompress() }
                    )
                }
            }
        }
    )
}

@Composable
private fun PasteBar(count: Int, onPaste: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "$count öğe panoda", style = MaterialTheme.typography.bodyMedium)
        ExtendedFloatingActionButton(
            onClick = onPaste,
            icon = { Icon(Icons.Filled.ContentPaste, contentDescription = null) },
            text = { Text("Yapıştır") }
        )
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) { CircularProgressIndicator() }
}

@Composable
private fun FileGrid(
    items: List<FileItem>,
    selectedPaths: Set<String>,
    isSelectionMode: Boolean,
    onItemClick: (FileItem) -> Unit,
    onItemLongClick: (FileItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        gridItems(items, key = { it.path }) { item ->
            FileGridTile(
                item = item,
                isSelected = selectedPaths.contains(item.path),
                isSelectionMode = isSelectionMode,
                onClick = { onItemClick(item) },
                onLongClick = { onItemLongClick(item) }
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun FileGridTile(
    item: FileItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val category = MimeTypeHelper.getCategory(item.name, item.isDirectory)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                MaterialTheme.shapes.medium
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            com.example.smartfilemanager.ui.components.FileThumbnail(
                path = item.path,
                category = category,
                size = 88.dp
            )
            if (isSelectionMode && isSelected) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Spacer(modifier = Modifier.padding(top = 4.dp))
        Text(
            text = item.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FileList(
    items: List<FileItem>,
    selectedPaths: Set<String>,
    favoritePaths: Set<String>,
    isSelectionMode: Boolean,
    onItemClick: (FileItem) -> Unit,
    onItemLongClick: (FileItem) -> Unit,
    onRenameClick: (FileItem) -> Unit,
    onDeleteClick: (FileItem) -> Unit,
    onInfoClick: (FileItem) -> Unit,
    onOpenExternallyClick: (FileItem) -> Unit,
    onShareClick: (FileItem) -> Unit,
    onToggleFavoriteClick: (FileItem) -> Unit,
    onExtractClick: (FileItem) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items, key = { it.path }) { item ->
            FileRow(
                item = item,
                isSelected = selectedPaths.contains(item.path),
                isFavorite = favoritePaths.contains(item.path),
                isSelectionMode = isSelectionMode,
                onClick = { onItemClick(item) },
                onLongClick = { onItemLongClick(item) },
                onRenameClick = { onRenameClick(item) },
                onDeleteClick = { onDeleteClick(item) },
                onInfoClick = { onInfoClick(item) },
                onOpenExternallyClick = { onOpenExternallyClick(item) },
                onShareClick = { onShareClick(item) },
                onToggleFavoriteClick = { onToggleFavoriteClick(item) },
                onExtractClick = { onExtractClick(item) }
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun FileRow(
    item: FileItem,
    isSelected: Boolean,
    isFavorite: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onInfoClick: () -> Unit,
    onOpenExternallyClick: () -> Unit,
    onShareClick: () -> Unit,
    onToggleFavoriteClick: () -> Unit,
    onExtractClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val category = MimeTypeHelper.getCategory(item.name, item.isDirectory)
    val isZip = item.extension.equals("zip", ignoreCase = true)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            Icon(
                imageVector = if (isSelected) Icons.Filled.Check else Icons.Filled.Add,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.padding(start = 8.dp))
        }

        com.example.smartfilemanager.ui.components.FileThumbnail(
            path = item.path,
            category = category
        )

        Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isFavorite) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Favori",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp).size(14.dp)
                    )
                }
            }
            val subtitle = if (item.isDirectory) "Klasör" else SizeFormatter.format(item.sizeBytes)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (!isSelectionMode) {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Diğer işlemler")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(if (isFavorite) "Favorilerden Çıkar" else "Favorilere Ekle") },
                        leadingIcon = {
                            Icon(
                                if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = null
                            )
                        },
                        onClick = { showMenu = false; onToggleFavoriteClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Yeniden Adlandır") },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = { showMenu = false; onRenameClick() }
                    )
                    if (isZip) {
                        DropdownMenuItem(
                            text = { Text("Çıkart") },
                            leadingIcon = { Icon(Icons.Filled.FolderZip, contentDescription = null) },
                            onClick = { showMenu = false; onExtractClick() }
                        )
                    }
                    if (!item.isDirectory) {
                        DropdownMenuItem(
                            text = { Text("Harici Uygulamada Aç") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
                            onClick = { showMenu = false; onOpenExternallyClick() }
                        )
                        DropdownMenuItem(
                            text = { Text("Paylaş") },
                            leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                            onClick = { showMenu = false; onShareClick() }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Bilgi") },
                        leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                        onClick = { showMenu = false; onInfoClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Sil") },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                        onClick = { showMenu = false; onDeleteClick() }
                    )
                }
            }
        }
    }
}

@Composable
private fun NameInputDialog(
    title: String,
    label: String,
    initialValue: String = "",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text) }) { Text("Tamam") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Vazgeç") }
        }
    )
}

@Composable
private fun FileInfoDialog(
    info: com.example.smartfilemanager.model.FileInfo,
    hashResult: String?,
    isComputingHash: Boolean,
    onComputeHash: (com.example.smartfilemanager.util.HashAlgorithm) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dosya Bilgisi") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                InfoRow("Ad", info.name)
                InfoRow("Yol", info.path)
                InfoRow("Tür", if (info.isDirectory) "Klasör" else (info.mimeType ?: "Bilinmiyor"))
                if (!info.isDirectory) {
                    InfoRow("Boyut", SizeFormatter.format(info.sizeBytes))
                }
                InfoRow("Son Değiştirilme", DateUtils.formatTimestamp(info.lastModified))
                info.creationTime?.let { InfoRow("Oluşturulma", DateUtils.formatTimestamp(it)) }
                InfoRow("İzinler", buildString {
                    append(if (info.canRead) "Okunabilir " else "")
                    append(if (info.canWrite) "Yazılabilir " else "")
                    append(if (info.canExecute) "Çalıştırılabilir" else "")
                }.ifBlank { "Yok" })

                if (!info.isDirectory) {
                    Spacer(modifier = Modifier.padding(top = 12.dp))
                    Text(text = "Özet (Hash)", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.padding(top = 4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        com.example.smartfilemanager.util.HashAlgorithm.entries.forEach { algorithm ->
                            androidx.compose.material3.AssistChip(
                                onClick = { onComputeHash(algorithm) },
                                label = { Text(algorithm.label) },
                                enabled = !isComputingHash
                            )
                        }
                    }
                    Spacer(modifier = Modifier.padding(top = 8.dp))
                    when {
                        isComputingHash -> androidx.compose.material3.LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        hashResult != null -> Text(
                            text = hashResult,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Kapat") }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = "$label: ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
