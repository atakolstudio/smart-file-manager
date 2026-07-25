package com.example.smartfilemanager.ui.screens.preview

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.io.File

/**
 * Resim (Coil), PDF (ilk sayfa) ve metin tabanlı dosyalar için önizleme ekranı.
 * Desteklenmeyen türlerde kullanıcıya harici uygulamada açma seçeneği sunulur.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    path: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PreviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(path) {
        path?.let { viewModel.load(it) }
    }

    LaunchedEffect(uiState.openError) {
        uiState.openError?.let { message ->
            coroutineScope.launch { snackbarHostState.showSnackbar(message) }
            viewModel.consumeError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = path?.substringAfterLast('/') ?: "Önizleme") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    if (path != null) {
                        IconButton(onClick = { viewModel.openExternally(path) }) {
                            Icon(Icons.Filled.OpenInNew, contentDescription = "Harici uygulamada aç")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                path == null -> CenteredMessage("Dosya bulunamadı")
                uiState.isLoading -> CenteredLoading()
                uiState.errorMessage != null -> CenteredMessage(uiState.errorMessage!!)
                uiState.kind == PreviewKind.IMAGE -> ImagePreview(path)
                uiState.kind == PreviewKind.PDF -> PdfPreview(uiState)
                uiState.kind == PreviewKind.TEXT -> TextPreview(uiState.textContent.orEmpty())
                else -> UnsupportedPreview(onOpenExternally = { viewModel.openExternally(path) })
            }
        }
    }
}

@Composable
private fun ImagePreview(path: String) {
    AsyncImage(
        model = File(path),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Fit
    )
}

@Composable
private fun PdfPreview(uiState: PreviewUiState) {
    val bitmap = uiState.pdfPageBitmap
    if (bitmap == null) {
        CenteredMessage("PDF önizlenemedi")
        return
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            contentScale = ContentScale.FillWidth
        )
        if (uiState.pdfPageCount > 1) {
            Text(
                text = "1 / ${uiState.pdfPageCount} sayfa gösteriliyor",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            )
        }
    }
}

@Composable
private fun TextPreview(content: String) {
    Text(
        text = content,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    )
}

@Composable
private fun UnsupportedPreview(onOpenExternally: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Bu dosya türü uygulama içinde önizlenemiyor",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.padding(top = 16.dp))
        Button(onClick = onOpenExternally) {
            Text("Harici Uygulamada Aç")
        }
    }
}

@Composable
private fun CenteredLoading() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun CenteredMessage(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
    }
}
