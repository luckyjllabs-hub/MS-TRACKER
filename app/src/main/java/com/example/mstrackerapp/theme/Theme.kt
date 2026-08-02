package com.example.mstrackerapp.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BrandGreen,
    onPrimary = Color.White,
    primaryContainer = BrandGreenDark,
    onPrimaryContainer = Color.White,
    secondary = BrandGreen,
    onSecondary = Color.White,
    background = BrandBackgroundDark,
    onBackground = BrandTextLight,
    surface = BrandSurfaceDark,
    onSurface = BrandTextLight,
    surfaceVariant = Color(0xFF2E332E),
    onSurfaceVariant = BrandTextLight
)

private val LightColorScheme = lightColorScheme(
    primary = BrandGreen,
    onPrimary = Color.White,
    primaryContainer = BrandGreenLight,
    onPrimaryContainer = BrandTextDark,
    secondary = BrandGreen,
    onSecondary = Color.White,
    background = BrandBackgroundLight,
    onBackground = BrandTextDark,
    surface = BrandSurfaceLight,
    onSurface = BrandTextDark,
    surfaceVariant = Color(0xFFE4E8E3),
    onSurfaceVariant = BrandTextDark
)

@Composable
fun MSTrackerAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
