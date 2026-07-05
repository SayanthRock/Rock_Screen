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
    primary = Color(0xFF818CF8),        // Modern Periwinkle Indigo
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF34D399),      // Mint Emerald Accent
    onSecondary = Color(0xFF064E3B),
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = Color(0xFFC084FC),       // Warm Lavender/Purple
    onTertiary = Color(0xFF4C1D95),
    background = Color(0xFF0B0F19),     // Deep Slate Obsidian Black
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF111827),        // Rich Slate Card Base
    onSurface = Color(0xFFF9FAFB),
    surfaceVariant = Color(0xFF1F2937),  // Elevated Card Variant
    onSurfaceVariant = Color(0xFFD1D5DB),
    outline = Color(0xFF4B5563),
    outlineVariant = Color(0xFF374151),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4F46E5),        // Indigo 600
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2F6),
    onPrimaryContainer = Color(0xFF312E81),
    secondary = Color(0xFF059669),      // Emerald 600
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFECFDF5),
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = Color(0xFF7C3AED),       // Deep Purple
    onTertiary = Color.White,
    background = Color(0xFFF9FAFB),     // Clean Soft Light Gray
    onBackground = Color(0xFF111827),
    surface = Color.White,
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFF3F4F6),  // Muted Slate Gray
    onSurfaceVariant = Color(0xFF4B5563),
    outline = Color(0xFFD1D5DB),
    outlineVariant = Color(0xFFE5E7EB),
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set dynamicColor to true by default to support Material 3 dynamic color scheme (on Android 12+)
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
