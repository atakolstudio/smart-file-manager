package com.example.smartfilemanager.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive yaklaşımına uygun, daha yumuşak ve belirgin köşe yarıçapları.
 * Küçük bileşenler (chip, buton) hafif yuvarlak; kartlar ve büyük yüzeyler daha belirgin yuvarlak.
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)
