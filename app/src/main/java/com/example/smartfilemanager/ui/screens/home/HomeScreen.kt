package com.example.smartfilemanager.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartfilemanager.R
import com.example.smartfilemanager.ui.theme.ApkColor
import com.example.smartfilemanager.ui.theme.AudioColor
import com.example.smartfilemanager.ui.theme.DocumentColor
import com.example.smartfilemanager.ui.theme.ImageColor
import com.example.smartfilemanager.ui.theme.VideoColor
import com.example.smartfilemanager.util.SizeFormatter

private data class HomeCategory(
    val titleRes: Int,
    val icon: ImageVector,
    val color: androidx.compose.ui.graphics.Color
)

private val homeCategories = listOf(
    HomeCategory(R.string.home_category_images, Icons.Filled.Image, ImageColor),
    HomeCategory(R.string.home_category_videos, Icons.Filled.Videocam, VideoColor),
    HomeCategory(R.string.home_category_music, Icons.Filled.Audiotrack, AudioColor),
    HomeCategory(R.string.home_category_documents, Icons.Filled.Description, DocumentColor),
    HomeCategory(R.string.home_category_downloads, Icons.Filled.Download, DocumentColor),
    HomeCategory(R.string.home_category_apps, Icons.Filled.Apps, ApkColor)
)

@Composable
fun HomeScreen(
    onCategoryClick: (Int) -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.refresh()
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
                modifier = Modifier.padding(paddingValues)
            )
            else -> HomeContent(
                paddingValues = paddingValues,
                storageSummary = uiState.storageSummary,
                onCategoryClick = onCategoryClick
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
    onCategoryClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
    ) {
        StorageSummaryCard(storageSummary)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResourceCompat(R.string.home_quick_access),
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(homeCategories.size) { index ->
                val category = homeCategories[index]
                CategoryTile(category = category, onClick = { onCategoryClick(index) })
            }
        }
    }
}

@Composable
private fun StorageSummaryCard(summary: StorageSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResourceCompat(R.string.home_internal_storage),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            val progress = if (summary.totalBytes > 0) {
                (summary.usedBytes.toFloat() / summary.totalBytes.toFloat()).coerceIn(0f, 1f)
            } else 0f

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                trackColor = MaterialTheme.colorScheme.surface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${SizeFormatter.format(summary.usedBytes)} kullanıldı",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${SizeFormatter.format(summary.freeBytes)} boş",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun CategoryTile(category: HomeCategory, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = category.color,
                modifier = Modifier.height(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResourceCompat(category.titleRes),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun stringResourceCompat(id: Int): String =
    androidx.compose.ui.res.stringResource(id = id)
