package com.example.smartfilemanager.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.smartfilemanager.model.FileCategory
import com.example.smartfilemanager.ui.theme.ApkColor
import com.example.smartfilemanager.ui.theme.ArchiveColor
import com.example.smartfilemanager.ui.theme.AudioColor
import com.example.smartfilemanager.ui.theme.DocumentColor
import com.example.smartfilemanager.ui.theme.FolderColor
import com.example.smartfilemanager.ui.theme.ImageColor
import com.example.smartfilemanager.ui.theme.OtherFileColor
import com.example.smartfilemanager.ui.theme.VideoColor
import androidx.compose.ui.graphics.Color

/**
 * Dosya kategorisine göre gösterilecek ikon ve rengi tek noktadan sağlar.
 */
object IconProvider {

    fun iconFor(category: FileCategory): ImageVector = when (category) {
        FileCategory.FOLDER -> Icons.Filled.Folder
        FileCategory.IMAGE -> Icons.Filled.Image
        FileCategory.VIDEO -> Icons.Filled.Videocam
        FileCategory.AUDIO -> Icons.Filled.Audiotrack
        FileCategory.DOCUMENT -> Icons.Filled.Description
        FileCategory.ARCHIVE -> Icons.Filled.FolderZip
        FileCategory.APK -> Icons.Filled.Apps
        FileCategory.OTHER -> Icons.Filled.InsertDriveFile
    }

    fun colorFor(category: FileCategory): Color = when (category) {
        FileCategory.FOLDER -> FolderColor
        FileCategory.IMAGE -> ImageColor
        FileCategory.VIDEO -> VideoColor
        FileCategory.AUDIO -> AudioColor
        FileCategory.DOCUMENT -> DocumentColor
        FileCategory.ARCHIVE -> ArchiveColor
        FileCategory.APK -> ApkColor
        FileCategory.OTHER -> OtherFileColor
    }
}
