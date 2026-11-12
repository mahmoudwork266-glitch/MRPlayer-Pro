package com.mahmoudrateb.player

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Accent = Color(0xFF8E7CFF)
val Soft = Color(0xFFB9B0FF)
val GlassBg = Color(0x33FFFFFF)

fun Modifier.appBackground(): Modifier = background(
    Brush.linearGradient(listOf(Color(0xFF1B1464), Color(0xFF3F1D3D)))
)

@Composable
fun MRTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            background = Color(0xFF14102E),
            surface = Color(0xFF241C4A)
        ),
        content = content
    )
}
