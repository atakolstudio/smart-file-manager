package com.example.smartfilemanager.ui.screens.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartfilemanager.R
import com.example.smartfilemanager.model.FileCategory
import com.example.smartfilemanager.ui.theme.ApkColor
import com.example.smartfilemanager.ui.theme.AudioColor
import com.example.smartfilemanager.ui.theme.DocumentColor
import com.example.smartfilemanager.ui.theme.ImageColor
import com.example.smartfilemanager.ui.theme.VideoColor
import com.example.smartfilemanager.util.SizeFormatter
import kotlin.math.roundToInt

/**
 * @param directoryLabel [StorageManager.getCommonDirectories] içindeki anahtarla eşleşir.
 * null ise (ör. Uygulamalar) bu kategori bir klasöre değil ayrı bir ekrana yönlendirilecektir.
 */
private data class HomeCategory(
    val titleRes: Int,
    val icon: ImageVector,
    val color: Color,
    val fileCategory: FileCategory?,
    val directoryLabel: String?
)

private val homeCategories = listOf(
    HomeCategory(R.string.home_category_images, Icons.Filled.Image, ImageColor, FileCategory.IMAGE, "Pictures"),
    HomeCategory(R.string.home_category_videos, Icons.Filled.Videocam, VideoColor, FileCategory.VIDEO, "Movies"),
    HomeCategory(R.string.home_category_music, Icons.Filled.Audiotrack, AudioColor, FileCategory.AUDIO, "Music"),
    HomeCategory(R.string.home_category_documents, Icons.Filled.Description, DocumentColor, FileCategory.DOCUMENT, "Documents"),
    HomeCategory(R.string.home_category_downloads, Icons.Filled.Download, DocumentColor, null, "Download"),
    HomeCategory(R.string.home_category_apps, Icons.Filled.Apps, ApkColor, FileCategory.APK, null)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToFolder: (String) -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current

    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(text = stringResourceCompat(R.string.home_title)) })
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> LoadingContent(paddingValues)
            !uiState.hasPermission -> com.example.smartfilemanager.ui.screens.permission.PermissionScreen(
                onGrantPermissionClick = onRequestPermission,
                onRecheckClick = { viewModel.refresh() },
                modifier = Modifier.padding(paddingValues)
            )
            else -> HomeContent(
                paddingValues = paddingValues,
                storageSummary = uiState.storageSummary,
                categorySummaries = uiState.categorySummaries,
                onCategoryClick = { category ->
                    category.directoryLabel
                        ?.let { viewModel.directoryPathFor(it) }
                        ?.let(onNavigateToFolder)
                }
            )
        }
    }
}

@Composable
private fun LoadingContent(paddingValues: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun HomeContent(
    paddingValues: PaddingValues,
    storageSummary: StorageSummary,
    categorySummaries: List<com.example.smartfilemanager.model.CategorySummary>,
    onCategoryClick: (HomeCategory) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
    ) {
        StorageSummaryCard(storageSummary)

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResourceCompat(R.string.home_quick_access),
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(homeCategories.size) { index ->
                val category = homeCategories[index]
                val fileCount = categorySummaries
                    .firstOrNull { it.category == category.fileCategory }
                    ?.fileCount
                CategoryTile(
                    category = category,
                    fileCount = fileCount,
                    onClick = { onCategoryClick(category) }
                )
            }
        }
    }
}

@Composable
private fun StorageSummaryCard(summary: StorageSummary) {
    val progress = if (summary.totalBytes > 0) {
        (summary.usedBytes.toFloat() / summary.totalBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 900),
        label = "storage-progress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StorageRing(progress = animatedProgress)

                Spacer(modifier = Modifier.padding(start = 20.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResourceCompat(R.string.home_internal_storage),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${SizeFormatter.format(summary.usedBytes)} kullanıldı",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${SizeFormatter.format(summary.freeBytes)} boş alan",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StorageRing(progress: Float, modifier: Modifier = Modifier) {
    val trackColor = MaterialTheme.colorScheme.surface
    val progressColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier.size(84.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(84.dp)) {
            val strokeWidth = 10.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2f, strokeWidth / 2f)
            val arcSize = Size(diameter, diameter)

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Text(
            text = "${(progress * 100).roundToInt()}%",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun CategoryTile(category: HomeCategory, fileCount: Int?, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color = category.color.copy(alpha = 0.16f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = category.color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResourceCompat(category.titleRes),
                style = MaterialTheme.typography.labelSmall
            )
            if (fileCount != null) {
                Text(
                    text = "$fileCount öğe",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun stringResourceCompat(id: Int): String =
    androidx.compose.ui.res.stringResource(id = id)
