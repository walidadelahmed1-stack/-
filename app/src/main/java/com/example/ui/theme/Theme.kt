package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val CyberColorScheme = darkColorScheme(
    primary = RaGold,
    onPrimary = Color(0xFF1E1300),
    primaryContainer = Color(0xFF3D2800),
    onPrimaryContainer = SandText,
    secondary = AnkhCyan,
    onSecondary = Color(0xFF001F29),
    secondaryContainer = Color(0xFF003847),
    onSecondaryContainer = Color(0xFFB8F3FF),
    tertiary = HorusPurple,
    onTertiary = Color(0xFF1C004F),
    background = CyberVoid,
    onBackground = Color(0xFFF1F5F9),
    surface = CyberSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = SandMuted,
    outline = CyberBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // The game is designed as an immersive dark cyberpunk Egyptian experience with RTL support for Arabic
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = CyberColorScheme,
            typography = Typography,
            content = content
        )
    }
}
