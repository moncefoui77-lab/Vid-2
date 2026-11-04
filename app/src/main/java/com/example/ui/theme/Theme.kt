package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = OrangePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4A1A00),
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = PurpleAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF321E5C),
    onSecondaryContainer = Color(0xFFE9DDFF),
    tertiary = NeonPink,
    background = DarkBackground,
    onBackground = Color(0xFFEDEDED),
    surface = DarkSurface,
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFC9C5D5),
    outline = DarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = OrangePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCF),
    onPrimaryContainer = Color(0xFF381000),
    secondary = PurpleDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9DDFF),
    onSecondaryContainer = Color(0xFF23085A),
    tertiary = NeonPink,
    background = LightBackground,
    onBackground = Color(0xFF19181E),
    surface = LightSurface,
    onSurface = Color(0xFF19181E),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF474554),
    outline = LightBorder
)

@Composable
fun VidSmartTheme(
    darkTheme: Boolean = true, // default to sleek modern dark theme for media browser
    dynamicColor: Boolean = false, // keep our signature orange-purple gradient identity
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
