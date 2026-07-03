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

private val MinimalistColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF),      // Bottom App Bar Accent / Primary Electric Cyan
    secondary = Color(0xFF10B981),    // Emerald Save Button Highlight
    tertiary = Color(0xFF8B5CF6),     // Neon Cyber Purple Accent
    background = Color(0xFF080D16),   // Deep Space Obsidian Black
    surface = Color(0xFF0F172A),      // Dark Slate Grey-Blue Panels
    surfaceVariant = Color(0xFF1E293B), // Premium Card Background / Slate Blue
    onBackground = Color(0xFFF8FAFC), // Crisp Slate 50 text
    onSurface = Color(0xFFF1F5F9),    // Crisp Slate 100 text
    onSurfaceVariant = Color(0xFF94A3B8), // Muted Slate 400 text
    outline = Color(0xFF334155),      // Borders Slate 700
    outlineVariant = Color(0xFF1E293B), // Borders Slate 800
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep dynamicColor false to preserve the customized Clean Minimalism branding
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = MinimalistColorScheme

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
