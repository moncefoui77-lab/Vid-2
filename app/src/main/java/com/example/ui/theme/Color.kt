package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val OrangePrimary = Color(0xFFFF6D00)
val OrangeSecondary = Color(0xFFFF9100)
val PurpleAccent = Color(0xFF7C4DFF)
val PurpleDark = Color(0xFF512DA8)
val NeonPink = Color(0xFFFF4081)

val DarkBackground = Color(0xFF0D0C14)
val DarkSurface = Color(0xFF161524)
val DarkSurfaceVariant = Color(0xFF211F35)
val DarkBorder = Color(0xFF322E4D)

val LightBackground = Color(0xFFF7F7FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEDEBF5)
val LightBorder = Color(0xFFDCD8E8)

val VidSmartGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFFFF6D00),
        Color(0xFFE040FB),
        Color(0xFF7C4DFF)
    )
)

val VidSmartPlayerGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xCC000000),
        Color(0x00000000),
        Color(0xDD000000)
    )
)
