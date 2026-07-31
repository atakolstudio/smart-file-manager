package com.example.smartfilemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.smartfilemanager.model.FileCategory
import com.example.smartfilemanager.util.IconProvider
import java.io.File

/**
 * Dosya listelerinde (liste ve ızgara görünümü) kullanılan küçük resim/ikon gösterimi.
 * Fotoğraflar için gerçek küçük resim (Coil ile dosyadan), diğer türler için tonal
 * renkli kategori ikonu gösterir — popüler dosya yöneticilerinin (Solid Explorer,
 * MiXplorer vb.) ortak özelliği.
 */
@Composable
fun FileThumbnail(
    path: String,
    category: FileCategory,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 40.dp
) {
    if (category == FileCategory.IMAGE) {
        AsyncImage(
            model = File(path),
            contentDescription = null,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .background(IconProvider.colorFor(category).copy(alpha = 0.14f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = IconProvider.iconFor(category),
                contentDescription = null,
                tint = IconProvider.colorFor(category),
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}
