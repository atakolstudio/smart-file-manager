package com.example.smartfilemanager.ui.screens.preview

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Audiotrack
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.io.File

/**
 * "Bilgisayar gibi" — desteklenen tüm yaygın dosya türleri uygulama İÇİNDE açılır:
 * resim (Coil), PDF (ilk sayfa), metin, VİDEO/SES (Media3 ExoPlayer ile gerçek oynatma,
 * kontrollerle), APK (kurulum gerekmeden ad/paket/sürüm/ikon önizlemesi + Yükle butonu).
 * Sadece gerçekten uygulama içinde gösterilemeyen türler harici uygulamaya yönlendirilir.
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    if (path != null && uiState.kind != PreviewKind.VIDEO && uiState.kind != PreviewKind.AUDIO) {
                        IconButton(onClick = { viewModel.openExternally(path) }) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Harici uygulamada aç")
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
                uiState.kind == PreviewKind.VIDEO -> VideoAudioPreview(path, isVideo = true)
                uiState.kind == PreviewKind.AUDIO -> VideoAudioPreview(path, isVideo = false)
                uiState.kind == PreviewKind.APK -> ApkPreview(
                    info = uiState.apkInfo,
                    onInstallClick = { viewModel.openExternally(path) }
                )
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
private fun VideoAudioPreview(path: String, isVideo: Boolean) {
    val context = LocalContext.current

    val exoPlayer = remember(path) {
        ExoPlayer.Builder(context).build().apply {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, File(path))
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    if (isVideo) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    } else {
        // Ses dosyaları için: albüm kapağı/ikon alanı olmadan, sade kontrol çubuğu yeterli.
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Audiotrack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(96.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
            )
        }
    }
}

@Composable
private fun ApkPreview(info: ApkPreviewInfo?, onInstallClick: () -> Unit) {
    if (info == null) {
        CenteredMessage("APK bilgisi okunamadı")
        return
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (info.icon != null) {
            Image(
                bitmap = info.icon.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(96.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Text(text = info.appName, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = info.packageName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Sürüm: ${info.versionName ?: "Bilinmiyor"}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onInstallClick) {
            Text("Yükle")
        }
    }
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
