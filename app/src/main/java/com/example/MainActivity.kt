package com.example

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.ImageDecoder
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true, dynamicColor = false) {
                HiShootApp()
            }
        }
    }
}

// ==========================================
// MODELS & CONFIGURATION DEFINITIONS
// ==========================================

enum class TabCategory(val label: String, val icon: ImageVector) {
    SCREENSHOT("Screenshot", Icons.Default.AddPhotoAlternate),
    BACKGROUND("Background", Icons.Default.Palette),
    TEMPLATE("Template", Icons.Default.Smartphone),
    CANVAS("Canvas", Icons.Default.Layers),
    WATERMARK("Watermark", Icons.Default.Verified),
    EXPORT("Export", Icons.Default.Save),
    BATCH("Batch Queue", Icons.Default.Collections)
}

enum class CanvasRatio(val label: String, val ratio: Float) {
    PORTRAIT_9_16("Mobile Customer Wallpaper (9:16)", 9f / 16f),
    LANDSCAPE_16_9("Landscape (16:9)", 16f / 9f),
    SQUARE_1_1("Square (1:1)", 1f),
    TABLET_4_3("Tablet (4:3)", 4f / 3f),
    MOBILE_WALLPAPER_TALL("Mobile Customer Wallpaper (9:19.5)", 9f / 19.5f)
}

enum class BackgroundType(val label: String) {
    SOLID("Solid Color"),
    GRADIENT("Linear Gradient"),
    PATTERN("Custom Pattern"),
    AMBIENT_BLUR("Ambient Blur"),
    LIQUID_FLOW("Liquid Blur"),
    IMAGE("Gallery Image")
}

enum class PatternType(val label: String) {
    DOTS("Dot Matrix"),
    GRID("Sleek Grid"),
    STRIPES("Diagonal Carbon"),
    WAVES("Topographic Wave"),
    HEXAGON("Sci-Fi Honeycomb"),
    STARS("Stellar Space")
}

enum class MockupTemplate(val label: String) {
    PIXEL_MODERN("Sleek Pixel (Punch Hole)"),
    IPHONE_NOTCH("Notch Pro"),
    DYNAMIC_ISLAND("Dynamic Island"),
    MINIMAL_BORDER("Edge-to-Edge Minimal")
}

enum class WatermarkPosition(val label: String, val alignment: Alignment) {
    TOP_LEFT("Top Left", Alignment.TopStart),
    TOP_RIGHT("Top Right", Alignment.TopEnd),
    BOTTOM_LEFT("Bottom Left", Alignment.BottomStart),
    BOTTOM_CENTER("Bottom Center", Alignment.BottomCenter),
    BOTTOM_RIGHT("Bottom Right", Alignment.BottomEnd)
}

data class CustomGradient(val name: String, val colors: List<Color>)

val PremiumGradients = listOf(
    CustomGradient("Midnight Glow", listOf(Color(0xFF0F172A), Color(0xFF1E293B))),
    CustomGradient("Sunset Breeze", listOf(Color(0xFFFF5E62), Color(0xFFFF9966))),
    CustomGradient("Ocean Surge", listOf(Color(0xFF00C6FF), Color(0xFF0072FF))),
    CustomGradient("Neon Matrix", listOf(Color(0xFF00FF87), Color(0xFF60EFFF))),
    CustomGradient("Cyberpunk Sky", listOf(Color(0xFFF355DA), Color(0xFF7000FF), Color(0xFF00F0FF))),
    CustomGradient("Cosmic Abyss", listOf(Color(0xFF1A0B2E), Color(0xFF0F051D))),
    CustomGradient("Emerald Jade", listOf(Color(0xFF11998e), Color(0xFF38ef7d)))
)

val PresetColors = listOf(
    Color(0xFF0F172A), // Deep Slate
    Color(0xFF1E293B), // Medium Slate
    Color(0xFF121212), // Dark Charcoal
    Color(0xFF0D1E2D), // Deep Cyan Blue
    Color(0xFF3B0764), // Dark Violet
    Color(0xFF022C22), // Dark Emerald
    Color(0xFF450A0A)  // Dark Maroon
)

object PatternConfig {
    var selectedPatternType: PatternType = PatternType.DOTS
    var patternPrimaryColor: Color = Color(0xFF818CF8)
    var patternSecondaryColor: Color = Color(0xFFC084FC)
    var patternBgColor: Color = Color(0xFF0F172A)
    var patternScale: Float = 1.0f
}

data class PhoneModelPreset(
    val name: String,
    val brand: String,
    val template: MockupTemplate,
    val bezelThickness: Float,
    val screenCornerRadius: Float,
    val bezelColor: Color,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

val PhonePresets = listOf(
    PhoneModelPreset(
        name = "Pixel 8 Pro",
        brand = "Google",
        template = MockupTemplate.PIXEL_MODERN,
        bezelThickness = 10f,
        screenCornerRadius = 28f,
        bezelColor = Color(0xFF2E2E2E),
        description = "Sleek flat profile with a centered camera punch-hole.",
        icon = androidx.compose.material.icons.Icons.Default.Smartphone
    ),
    PhoneModelPreset(
        name = "Pixel Classic",
        brand = "Google",
        template = MockupTemplate.PIXEL_MODERN,
        bezelThickness = 16f,
        screenCornerRadius = 20f,
        bezelColor = Color(0xFF1E293B),
        description = "Classic uniform bezels with rounded corner hardware accents.",
        icon = androidx.compose.material.icons.Icons.Default.PhoneAndroid
    ),
    PhoneModelPreset(
        name = "iPhone 15 Pro",
        brand = "Apple",
        template = MockupTemplate.DYNAMIC_ISLAND,
        bezelThickness = 8f,
        screenCornerRadius = 32f,
        bezelColor = Color(0xFF1F1F1F),
        description = "Ultra-thin symmetric bezels with the Dynamic Island sensor.",
        icon = androidx.compose.material.icons.Icons.Default.PhonelinkSetup
    ),
    PhoneModelPreset(
        name = "iPhone Classic Notch",
        brand = "Apple",
        template = MockupTemplate.IPHONE_NOTCH,
        bezelThickness = 12f,
        screenCornerRadius = 30f,
        bezelColor = Color(0xFF0F172A),
        description = "Symmetric rectangular screen and classic notch cut-out.",
        icon = androidx.compose.material.icons.Icons.Default.Inbox
    ),
    PhoneModelPreset(
        name = "Galaxy S24 Ultra",
        brand = "Samsung",
        template = MockupTemplate.PIXEL_MODERN,
        bezelThickness = 6f,
        screenCornerRadius = 8f,
        bezelColor = Color(0xFF0A0A0A),
        description = "Sharp, squared profile with micro bezel design borders.",
        icon = androidx.compose.material.icons.Icons.Default.CropSquare
    ),
    PhoneModelPreset(
        name = "Galaxy Infinity",
        brand = "Samsung",
        template = MockupTemplate.MINIMAL_BORDER,
        bezelThickness = 4f,
        screenCornerRadius = 18f,
        bezelColor = Color(0xFF121212),
        description = "Pure edge-to-edge frame optimized for immersive captures.",
        icon = androidx.compose.material.icons.Icons.Default.CropFree
    ),
    PhoneModelPreset(
        name = "Rock Screen Pro",
        brand = "HiShoot",
        template = MockupTemplate.MINIMAL_BORDER,
        bezelThickness = 5f,
        screenCornerRadius = 32f,
        bezelColor = Color(0xFF00E5FF),
        description = "Premium bezel-less mockup frame with dynamic neon obsidian accents.",
        icon = androidx.compose.material.icons.Icons.Default.Smartphone
    )
)

data class DeviceFrameStyle(
    val id: String,
    val name: String,
    val template: MockupTemplate,
    val bezelThickness: Float,
    val screenCornerRadius: Float,
    val bezelColor: Color,
    val defaultAspectRatio: Float,
    val isTablet: Boolean = false,
    val isDesktop: Boolean = false,
    val description: String = "",
    val width: Float = 300f,
    val height: Float = 620f,
    val buttonStyle: String = "none",
    val hasFoldLine: Boolean = false
)

val DeviceFrameStylePresets = listOf(
    DeviceFrameStyle(
        id = "pixel_8_pro",
        name = "Pixel 8 Pro (Phone)",
        template = MockupTemplate.PIXEL_MODERN,
        bezelThickness = 10f,
        screenCornerRadius = 42f,
        bezelColor = Color(0xFF2E2E2E),
        defaultAspectRatio = 300f / 630f,
        description = "Google flat profile with a centered camera punch-hole and dual-button accents.",
        width = 300f,
        height = 630f,
        buttonStyle = "android"
    ),
    DeviceFrameStyle(
        id = "pixel_7",
        name = "Pixel 7 (Phone)",
        template = MockupTemplate.PIXEL_MODERN,
        bezelThickness = 10f,
        screenCornerRadius = 40f,
        bezelColor = Color(0xFF1F2024),
        defaultAspectRatio = 295f / 620f,
        description = "Google uniform bezels with rounded corner hardware accents.",
        width = 295f,
        height = 620f,
        buttonStyle = "android"
    ),
    DeviceFrameStyle(
        id = "iphone_15_pro",
        name = "iPhone 15 Pro (Phone)",
        template = MockupTemplate.DYNAMIC_ISLAND,
        bezelThickness = 12f,
        screenCornerRadius = 48f,
        bezelColor = Color(0xFF1F1F1F),
        defaultAspectRatio = 300f / 620f,
        description = "Apple ultra-thin symmetric bezels with the Dynamic Island sensor.",
        width = 300f,
        height = 620f,
        buttonStyle = "iphone"
    ),
    DeviceFrameStyle(
        id = "iphone_14_pro",
        name = "iPhone 14 Pro (Phone)",
        template = MockupTemplate.DYNAMIC_ISLAND,
        bezelThickness = 12f,
        screenCornerRadius = 48f,
        bezelColor = Color(0xFF2C2C2C),
        defaultAspectRatio = 300f / 620f,
        description = "Apple symmetric bezel-less design with premium high-contrast island cutout.",
        width = 300f,
        height = 620f,
        buttonStyle = "iphone"
    ),
    DeviceFrameStyle(
        id = "iphone_13",
        name = "iPhone 13 (Phone)",
        template = MockupTemplate.IPHONE_NOTCH,
        bezelThickness = 12f,
        screenCornerRadius = 44f,
        bezelColor = Color(0xFF0F172A),
        defaultAspectRatio = 300f / 620f,
        description = "Apple symmetric rectangular screen with a classic notch cutout.",
        width = 300f,
        height = 620f,
        buttonStyle = "iphone"
    ),
    DeviceFrameStyle(
        id = "iphone_se",
        name = "iPhone SE (Phone)",
        template = MockupTemplate.MINIMAL_BORDER,
        bezelThickness = 18f,
        screenCornerRadius = 34f,
        bezelColor = Color(0xFF1E293B),
        defaultAspectRatio = 285f / 560f,
        description = "Classic iPhone design with thick status and action borders.",
        width = 285f,
        height = 560f,
        buttonStyle = "iphone"
    ),
    DeviceFrameStyle(
        id = "samsung_s24_ultra",
        name = "Galaxy S24 Ultra (Phone)",
        template = MockupTemplate.PIXEL_MODERN,
        bezelThickness = 9f,
        screenCornerRadius = 30f,
        bezelColor = Color(0xFF0A0A0A),
        defaultAspectRatio = 305f / 640f,
        description = "Sharp, squared profile with micro bezel design borders.",
        width = 305f,
        height = 640f,
        buttonStyle = "android"
    ),
    DeviceFrameStyle(
        id = "samsung_s23",
        name = "Galaxy S23 (Phone)",
        template = MockupTemplate.PIXEL_MODERN,
        bezelThickness = 9f,
        screenCornerRadius = 40f,
        bezelColor = Color(0xFF111111),
        defaultAspectRatio = 300f / 625f,
        description = "Symmetric modern curved bezel frame with smooth corners.",
        width = 300f,
        height = 625f,
        buttonStyle = "android"
    ),
    DeviceFrameStyle(
        id = "oneplus_12",
        name = "OnePlus 12 (Phone)",
        template = MockupTemplate.PIXEL_MODERN,
        bezelThickness = 9f,
        screenCornerRadius = 42f,
        bezelColor = Color(0xFF1A1A1A),
        defaultAspectRatio = 305f / 635f,
        description = "Chic tall display with elegant curves and dynamic centered punch-hole.",
        width = 305f,
        height = 635f,
        buttonStyle = "android"
    ),
    DeviceFrameStyle(
        id = "xiaomi_14",
        name = "Xiaomi 14 (Phone)",
        template = MockupTemplate.PIXEL_MODERN,
        bezelThickness = 9f,
        screenCornerRadius = 38f,
        bezelColor = Color(0xFF1E1E1E),
        defaultAspectRatio = 298f / 620f,
        description = "Ultra-compact profile with micro bezel screen borders.",
        width = 298f,
        height = 620f,
        buttonStyle = "android"
    ),
    DeviceFrameStyle(
        id = "generic_android",
        name = "Generic Android (Phone)",
        template = MockupTemplate.PIXEL_MODERN,
        bezelThickness = 10f,
        screenCornerRadius = 38f,
        bezelColor = Color(0xFF050505),
        defaultAspectRatio = 300f / 620f,
        description = "Standard dynamic Android smartphone mock frame.",
        width = 300f,
        height = 620f,
        buttonStyle = "android"
    ),
    DeviceFrameStyle(
        id = "galaxy_z_fold_open",
        name = "Galaxy Z Fold Open (Phone)",
        template = MockupTemplate.PIXEL_MODERN,
        bezelThickness = 12f,
        screenCornerRadius = 38f,
        bezelColor = Color(0xFF0F0F12),
        defaultAspectRatio = 1.0f,
        description = "Unique open dual-pane folding display with a subtle center crease line.",
        width = 620f,
        height = 620f,
        buttonStyle = "android",
        hasFoldLine = true
    ),
    DeviceFrameStyle(
        id = "generic_tablet",
        name = "Generic Tablet (4:3)",
        template = MockupTemplate.MINIMAL_BORDER,
        bezelThickness = 14f,
        screenCornerRadius = 16f,
        bezelColor = Color(0xFF1C1B1F),
        defaultAspectRatio = 4f / 3f,
        isTablet = true,
        description = "Modern symmetric thin-bezel tablet style in landscape or portrait.",
        width = 600f,
        height = 800f,
        buttonStyle = "none"
    ),
    DeviceFrameStyle(
        id = "pixel_tablet",
        name = "Pixel Tablet (Tablet)",
        template = MockupTemplate.PIXEL_MODERN,
        bezelThickness = 12f,
        screenCornerRadius = 24f,
        bezelColor = Color(0xFF1F2024),
        defaultAspectRatio = 16f / 10f,
        isTablet = true,
        description = "Symmetric widescreen tablet frame with clean rounded corner screen aesthetics.",
        width = 800f,
        height = 500f,
        buttonStyle = "none"
    ),
    DeviceFrameStyle(
        id = "material_desktop",
        name = "Studio Display (Desktop)",
        template = MockupTemplate.MINIMAL_BORDER,
        bezelThickness = 8f,
        screenCornerRadius = 12f,
        bezelColor = Color(0xFF232529),
        defaultAspectRatio = 16f / 10f,
        isDesktop = true,
        description = "Chic widescreen aluminum studio monitor with modern immersive borders.",
        width = 960f,
        height = 600f,
        buttonStyle = "none"
    )
)

// ==========================================
// STATE HISTORY SNAPSHOT MODEL
// ==========================================

data class MockupState(
    val selectedImageUri: Uri? = null,
    val autoMatchDeviceFrameRatio: Boolean = true,
    val detectedScreenshotAspectRatio: Float = 9f / 19.5f,
    val selectedDeviceFrameStyleId: String = "pixel_8_pro",
    val selectedBackgroundUri: Uri? = null,
    val screenshotScale: Float = 1.0f,
    val screenshotOffsetX: Float = 0f,
    val screenshotOffsetY: Float = 0f,
    val deviceFrameScale: Float = 0.72f,
    val backgroundType: BackgroundType = BackgroundType.GRADIENT,
    val selectedSolidColor: Color = Color(0xFF0F172A), // PresetColors[0] is not directly resolved in a global constant if not loaded, so using Color(0xFF0F172A)
    val customHue: Float = 200f,
    val customSaturation: Float = 0.8f,
    val customValue: Float = 0.3f,
    val selectedGradientIndex: Int = 0,
    val ambientBlurRadius: Float = 15f,
    val backgroundBlurRadius: Float = 0f,
    val liquidThemeIndex: Int = 0,
    val liquidScale: Float = 1.2f,
    val showDisplayGlassBlur: Boolean = false,
    val displayGlassBlurColor: Color = Color(0xFF00E5FF),
    val displayGlassBlurOpacity: Float = 0.4f,
    val liquidNoiseEnabled: Boolean = true,
    val selectedPatternType: PatternType = PatternType.DOTS,
    val patternPrimaryColorHue: Float = 240f,
    val patternSecondaryColorHue: Float = 280f,
    val patternBgColor: Color = Color(0xFF0F172A),
    val patternScale: Float = 1.0f,
    val activeTemplate: MockupTemplate = MockupTemplate.MINIMAL_BORDER,
    val bezelColor: Color = Color(0xFF00E5FF),
    val bezelThickness: Float = 5f,
    val screenCornerRadius: Float = 32f,
    val showStatusBarIcons: Boolean = true,
    val showGlossyReflection: Boolean = true,
    val glossyReflectionOpacity: Float = 0.18f,
    val activeRatio: CanvasRatio = CanvasRatio.PORTRAIT_9_16,
    val tiltX: Float = 15f,
    val tiltY: Float = -15f,
    val tiltZ: Float = 5f,
    val perspectiveDepth: Float = 8f,
    val shadowStrength: Float = 0.4f,
    val shadowEnabled: Boolean = true,
    val shadowBlurRadius: Float = 25f,
    val shadowOffsetX: Float = 10f,
    val shadowOffsetY: Float = 15f,
    val shadowColor: Color = Color.Black,
    val showWatermark: Boolean = true,
    val watermarkText: String = "Sayanth Rock",
    val watermarkPosition: WatermarkPosition = WatermarkPosition.BOTTOM_CENTER,
    val watermarkColor: Color = Color(0xBB00E5FF),
    val watermarkSize: Float = 14f,
    val watermarkOpacity: Float = 0.7f,
    val screenshotGrayscale: Float = 0f,
    val screenshotSepia: Float = 0f,
    val screenshotBrightness: Float = 1f,
    val screenshotContrast: Float = 1f,
    val screenshotLiquidGlass: Boolean = false,
    val exportFormat: String = "PNG",
    val exportQuality: Float = 90f,
    val deviceFrameWidth: Float = 300f,
    val deviceFrameHeight: Float = 630f,
    val buttonStyle: String = "android",
    val hasFoldLine: Boolean = false
)

// ==========================================
// CORE APPLICATION LAYOUT
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiShootApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Configuration states
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var autoMatchDeviceFrameRatio by remember { mutableStateOf(true) }
    var detectedScreenshotAspectRatio by remember { mutableStateOf(9f / 19.5f) }
    var selectedDeviceFrameStyleId by remember { mutableStateOf("pixel_8_pro") }
    var isDeviceDropdownOpen by remember { mutableStateOf(false) }
    var selectedBackgroundUri by remember { mutableStateOf<Uri?>(null) }
    var screenshotScale by remember { mutableStateOf(1.0f) }
    var screenshotOffsetX by remember { mutableStateOf(0f) }
    var screenshotOffsetY by remember { mutableStateOf(0f) }
    var deviceFrameScale by remember { mutableStateOf(0.72f) } // 0.3f to 0.95f
    var deviceFrameWidth by remember { mutableStateOf(300f) }
    var deviceFrameHeight by remember { mutableStateOf(630f) }
    var buttonStyle by remember { mutableStateOf("android") } // "iphone", "android", "none"
    var hasFoldLine by remember { mutableStateOf(false) }

    val selectedStyle = remember(selectedDeviceFrameStyleId) {
        DeviceFrameStylePresets.find { it.id == selectedDeviceFrameStyleId } ?: DeviceFrameStylePresets[0]
    }

    val deviceFrameAspectRatio = if (autoMatchDeviceFrameRatio && selectedImageUri != null) {
        detectedScreenshotAspectRatio
    } else {
        deviceFrameWidth / deviceFrameHeight
    }

    var backgroundType by remember { mutableStateOf(BackgroundType.GRADIENT) }
    var selectedSolidColor by remember { mutableStateOf(PresetColors[0]) }
    // Custom Solid Color sliders (Hue, Saturation, Value)
    var customHue by remember { mutableStateOf(200f) }
    var customSaturation by remember { mutableStateOf(0.8f) }
    var customValue by remember { mutableStateOf(0.3f) }
    
    // Auto sync custom slider to solid color
    LaunchedEffect(customHue, customSaturation, customValue) {
        selectedSolidColor = Color.hsv(customHue, customSaturation, customValue)
    }

    var selectedGradientIndex by remember { mutableStateOf(0) } // Midnight Glow default
    var ambientBlurRadius by remember { mutableStateOf(15f) } // 5 to 25
    var backgroundBlurRadius by remember { mutableStateOf(0f) } // 0 to 25 (0 means no blur)

    // Liquid Flow / Display Glass Blur Premium states
    var liquidThemeIndex by remember { mutableStateOf(0) }
    var liquidScale by remember { mutableStateOf(1.2f) }
    var showDisplayGlassBlur by remember { mutableStateOf(false) }
    var displayGlassBlurColor by remember { mutableStateOf(Color(0xFF00E5FF)) }
    var displayGlassBlurOpacity by remember { mutableStateOf(0.4f) }
    var liquidNoiseEnabled by remember { mutableStateOf(true) }

    // Pattern background state variables
    var selectedPatternType by remember { mutableStateOf(PatternType.DOTS) }
    var patternPrimaryColorHue by remember { mutableStateOf(240f) } // Blue/Indigo default
    var patternPrimaryColor by remember { mutableStateOf(Color(0xFF818CF8)) }
    LaunchedEffect(patternPrimaryColorHue) {
        patternPrimaryColor = Color.hsv(patternPrimaryColorHue, 0.7f, 0.9f)
    }
    var patternSecondaryColorHue by remember { mutableStateOf(280f) } // Purple default
    var patternSecondaryColor by remember { mutableStateOf(Color(0xFFC084FC)) }
    LaunchedEffect(patternSecondaryColorHue) {
        patternSecondaryColor = Color.hsv(patternSecondaryColorHue, 0.7f, 0.9f)
    }
    var patternBgColor by remember { mutableStateOf(PresetColors[0]) }
    var patternScale by remember { mutableStateOf(1.0f) } // 0.4f to 2.5f

    LaunchedEffect(selectedPatternType, patternPrimaryColor, patternSecondaryColor, patternBgColor, patternScale) {
        PatternConfig.selectedPatternType = selectedPatternType
        PatternConfig.patternPrimaryColor = patternPrimaryColor
        PatternConfig.patternSecondaryColor = patternSecondaryColor
        PatternConfig.patternBgColor = patternBgColor
        PatternConfig.patternScale = patternScale
    }

    var activeTemplate by remember { mutableStateOf(MockupTemplate.MINIMAL_BORDER) }
    var bezelColor by remember { mutableStateOf(Color(0xFF00E5FF)) }
    var bezelThickness by remember { mutableStateOf(5f) } // 4dp to 24dp
    var screenCornerRadius by remember { mutableStateOf(32f) } // 0dp to 40dp
    var showStatusBarIcons by remember { mutableStateOf(true) }
    var showGlossyReflection by remember { mutableStateOf(true) }
    var glossyReflectionOpacity by remember { mutableStateOf(0.18f) }

    var activeRatio by remember { mutableStateOf(CanvasRatio.PORTRAIT_9_16) }
    var tiltX by remember { mutableStateOf(15f) } // -45 to 45
    var tiltY by remember { mutableStateOf(-15f) } // -45 to 45
    var tiltZ by remember { mutableStateOf(5f) } // -90 to 90
    var perspectiveDepth by remember { mutableStateOf(8f) } // 4 to 20 (camera distance)
    var shadowStrength by remember { mutableStateOf(0.4f) } // 0 to 1
    var shadowEnabled by remember { mutableStateOf(true) }
    var shadowBlurRadius by remember { mutableStateOf(25f) } // 0 to 80
    var shadowOffsetX by remember { mutableStateOf(10f) } // -50 to 50
    var shadowOffsetY by remember { mutableStateOf(15f) } // -50 to 50
    var shadowColor by remember { mutableStateOf(Color.Black) }

    var showWatermark by remember { mutableStateOf(true) }
    var watermarkText by remember { mutableStateOf("Sayanth Rock") }
    var watermarkPosition by remember { mutableStateOf(WatermarkPosition.BOTTOM_CENTER) }
    var watermarkColor by remember { mutableStateOf(Color(0xBB00E5FF)) }
    var watermarkSize by remember { mutableStateOf(14f) } // 10 to 24
    var watermarkOpacity by remember { mutableStateOf(0.7f) }

    // Screenshot Image Filter states
    var screenshotGrayscale by remember { mutableStateOf(0f) } // 0f to 1f
    var screenshotSepia by remember { mutableStateOf(0f) } // 0f to 1f
    var screenshotBrightness by remember { mutableStateOf(1f) } // 0.5f to 2f
    var screenshotContrast by remember { mutableStateOf(1f) } // 0.5f to 2f
    var screenshotLiquidGlass by remember { mutableStateOf(false) }

    var exportFormat by remember { mutableStateOf("PNG") } // "PNG", "JPEG", "WebP"
    var exportQuality by remember { mutableStateOf(90f) } // 10f to 100f

    // ==========================================
    // STATE HISTORY ENGINE (UNDO/REDO)
    // ==========================================
    val undoStack = remember { mutableStateListOf<MockupState>() }
    val redoStack = remember { mutableStateListOf<MockupState>() }
    var isUndoingRedoing by remember { mutableStateOf(false) }
    var lastSavedState by remember { mutableStateOf<MockupState?>(null) }

    fun captureCurrentState() = MockupState(
        selectedImageUri = selectedImageUri,
        autoMatchDeviceFrameRatio = autoMatchDeviceFrameRatio,
        detectedScreenshotAspectRatio = detectedScreenshotAspectRatio,
        selectedDeviceFrameStyleId = selectedDeviceFrameStyleId,
        selectedBackgroundUri = selectedBackgroundUri,
        screenshotScale = screenshotScale,
        screenshotOffsetX = screenshotOffsetX,
        screenshotOffsetY = screenshotOffsetY,
        deviceFrameScale = deviceFrameScale,
        backgroundType = backgroundType,
        selectedSolidColor = selectedSolidColor,
        customHue = customHue,
        customSaturation = customSaturation,
        customValue = customValue,
        selectedGradientIndex = selectedGradientIndex,
        ambientBlurRadius = ambientBlurRadius,
        backgroundBlurRadius = backgroundBlurRadius,
        liquidThemeIndex = liquidThemeIndex,
        liquidScale = liquidScale,
        showDisplayGlassBlur = showDisplayGlassBlur,
        displayGlassBlurColor = displayGlassBlurColor,
        displayGlassBlurOpacity = displayGlassBlurOpacity,
        liquidNoiseEnabled = liquidNoiseEnabled,
        selectedPatternType = selectedPatternType,
        patternPrimaryColorHue = patternPrimaryColorHue,
        patternSecondaryColorHue = patternSecondaryColorHue,
        patternBgColor = patternBgColor,
        patternScale = patternScale,
        activeTemplate = activeTemplate,
        bezelColor = bezelColor,
        bezelThickness = bezelThickness,
        screenCornerRadius = screenCornerRadius,
        showStatusBarIcons = showStatusBarIcons,
        showGlossyReflection = showGlossyReflection,
        glossyReflectionOpacity = glossyReflectionOpacity,
        activeRatio = activeRatio,
        tiltX = tiltX,
        tiltY = tiltY,
        tiltZ = tiltZ,
        perspectiveDepth = perspectiveDepth,
        shadowStrength = shadowStrength,
        shadowEnabled = shadowEnabled,
        shadowBlurRadius = shadowBlurRadius,
        shadowOffsetX = shadowOffsetX,
        shadowOffsetY = shadowOffsetY,
        shadowColor = shadowColor,
        showWatermark = showWatermark,
        watermarkText = watermarkText,
        watermarkPosition = watermarkPosition,
        watermarkColor = watermarkColor,
        watermarkSize = watermarkSize,
        watermarkOpacity = watermarkOpacity,
        screenshotGrayscale = screenshotGrayscale,
        screenshotSepia = screenshotSepia,
        screenshotBrightness = screenshotBrightness,
        screenshotContrast = screenshotContrast,
        screenshotLiquidGlass = screenshotLiquidGlass,
        exportFormat = exportFormat,
        exportQuality = exportQuality,
        deviceFrameWidth = deviceFrameWidth,
        deviceFrameHeight = deviceFrameHeight,
        buttonStyle = buttonStyle,
        hasFoldLine = hasFoldLine
    )

    fun restoreState(state: MockupState) {
        selectedImageUri = state.selectedImageUri
        autoMatchDeviceFrameRatio = state.autoMatchDeviceFrameRatio
        detectedScreenshotAspectRatio = state.detectedScreenshotAspectRatio
        selectedDeviceFrameStyleId = state.selectedDeviceFrameStyleId
        selectedBackgroundUri = state.selectedBackgroundUri
        screenshotScale = state.screenshotScale
        screenshotOffsetX = state.screenshotOffsetX
        screenshotOffsetY = state.screenshotOffsetY
        deviceFrameScale = state.deviceFrameScale
        backgroundType = state.backgroundType
        selectedSolidColor = state.selectedSolidColor
        customHue = state.customHue
        customSaturation = state.customSaturation
        customValue = state.customValue
        selectedGradientIndex = state.selectedGradientIndex
        ambientBlurRadius = state.ambientBlurRadius
        backgroundBlurRadius = state.backgroundBlurRadius
        liquidThemeIndex = state.liquidThemeIndex
        liquidScale = state.liquidScale
        showDisplayGlassBlur = state.showDisplayGlassBlur
        displayGlassBlurColor = state.displayGlassBlurColor
        displayGlassBlurOpacity = state.displayGlassBlurOpacity
        liquidNoiseEnabled = state.liquidNoiseEnabled
        selectedPatternType = state.selectedPatternType
        patternPrimaryColorHue = state.patternPrimaryColorHue
        patternSecondaryColorHue = state.patternSecondaryColorHue
        patternBgColor = state.patternBgColor
        patternScale = state.patternScale
        activeTemplate = state.activeTemplate
        bezelColor = state.bezelColor
        bezelThickness = state.bezelThickness
        screenCornerRadius = state.screenCornerRadius
        showStatusBarIcons = state.showStatusBarIcons
        showGlossyReflection = state.showGlossyReflection
        glossyReflectionOpacity = state.glossyReflectionOpacity
        activeRatio = state.activeRatio
        tiltX = state.tiltX
        tiltY = state.tiltY
        tiltZ = state.tiltZ
        perspectiveDepth = state.perspectiveDepth
        shadowStrength = state.shadowStrength
        shadowEnabled = state.shadowEnabled
        shadowBlurRadius = state.shadowBlurRadius
        shadowOffsetX = state.shadowOffsetX
        shadowOffsetY = state.shadowOffsetY
        shadowColor = state.shadowColor
        showWatermark = state.showWatermark
        watermarkText = state.watermarkText
        watermarkPosition = state.watermarkPosition
        watermarkColor = state.watermarkColor
        watermarkSize = state.watermarkSize
        watermarkOpacity = state.watermarkOpacity
        screenshotGrayscale = state.screenshotGrayscale
        screenshotSepia = state.screenshotSepia
        screenshotBrightness = state.screenshotBrightness
        screenshotContrast = state.screenshotContrast
        screenshotLiquidGlass = state.screenshotLiquidGlass
        exportFormat = state.exportFormat
        exportQuality = state.exportQuality
        deviceFrameWidth = state.deviceFrameWidth
        deviceFrameHeight = state.deviceFrameHeight
        buttonStyle = state.buttonStyle
        hasFoldLine = state.hasFoldLine
    }

    val performUndo = {
        if (undoStack.isNotEmpty()) {
            coroutineScope.launch {
                isUndoingRedoing = true
                val currentState = captureCurrentState()
                val previousState = undoStack.removeAt(undoStack.lastIndex)
                
                // Push current state to redo stack
                redoStack.add(currentState)
                
                // Restore previous state
                restoreState(previousState)
                lastSavedState = previousState
                
                delay(150) // Allow states to settle and LaunchedEffects to run/ignore
                isUndoingRedoing = false
            }
        }
    }

    val performRedo = {
        if (redoStack.isNotEmpty()) {
            coroutineScope.launch {
                isUndoingRedoing = true
                val currentState = captureCurrentState()
                val nextState = redoStack.removeAt(redoStack.lastIndex)
                
                // Push current state to undo stack
                undoStack.add(currentState)
                
                // Restore next state
                restoreState(nextState)
                lastSavedState = nextState
                
                delay(150) // Allow states to settle and LaunchedEffects to run/ignore
                isUndoingRedoing = false
            }
        }
    }

    // Capture changes made in the sidebar with a debounce
    LaunchedEffect(
        selectedImageUri, autoMatchDeviceFrameRatio, detectedScreenshotAspectRatio, selectedDeviceFrameStyleId,
        selectedBackgroundUri, screenshotScale, screenshotOffsetX, screenshotOffsetY, deviceFrameScale,
        backgroundType, selectedSolidColor, customHue, customSaturation, customValue,
        selectedGradientIndex, ambientBlurRadius, backgroundBlurRadius, liquidThemeIndex, liquidScale,
        showDisplayGlassBlur, displayGlassBlurColor, displayGlassBlurOpacity, liquidNoiseEnabled,
        selectedPatternType, patternPrimaryColorHue, patternSecondaryColorHue, patternBgColor, patternScale,
        activeTemplate, bezelColor, bezelThickness, screenCornerRadius, showStatusBarIcons,
        showGlossyReflection, glossyReflectionOpacity, activeRatio, tiltX, tiltY, tiltZ,
        perspectiveDepth, shadowStrength, shadowEnabled, shadowBlurRadius, shadowOffsetX, shadowOffsetY,
        shadowColor, showWatermark, watermarkText, watermarkPosition, watermarkColor, watermarkSize,
        watermarkOpacity, screenshotGrayscale, screenshotSepia, screenshotBrightness, screenshotContrast,
        screenshotLiquidGlass, exportFormat, exportQuality,
        deviceFrameWidth, deviceFrameHeight, buttonStyle, hasFoldLine
    ) {
        if (isUndoingRedoing) {
            return@LaunchedEffect
        }

        // Debounce state saving so dragging a slider doesn't flood the stack
        delay(600)

        val newState = captureCurrentState()
        if (lastSavedState == null) {
            lastSavedState = newState
        } else if (newState != lastSavedState) {
            lastSavedState?.let { prev ->
                if (undoStack.size >= 50) {
                    undoStack.removeAt(0)
                }
                undoStack.add(prev)
                redoStack.clear()
            }
            lastSavedState = newState
        }
    }

    var activeTab by remember { mutableStateOf(TabCategory.SCREENSHOT) }
    var isSaving by remember { mutableStateOf(false) }

    var previewZoom by remember { mutableStateOf(1.0f) }
    var previewPanX by remember { mutableStateOf(0f) }
    var previewPanY by remember { mutableStateOf(0f) }

    // Media Picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                selectedImageUri = uri
                val ratio = getScreenshotAspectRatio(context, uri)
                detectedScreenshotAspectRatio = ratio
                Toast.makeText(context, "Screenshot loaded successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // File input component launcher for local screenshot files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                selectedImageUri = uri
                val ratio = getScreenshotAspectRatio(context, uri)
                detectedScreenshotAspectRatio = ratio
                Toast.makeText(context, "Screenshot file loaded successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val batchQueue = remember { mutableStateListOf<Uri>() }
    var isBatchProcessing by remember { mutableStateOf(false) }
    var batchProgressIndex by remember { mutableStateOf(0) }
    var batchTotalItems by remember { mutableStateOf(0) }

    val batchPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                batchQueue.addAll(uris)
                Toast.makeText(context, "${uris.size} screenshots added to the batch queue!", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val backgroundPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                selectedBackgroundUri = uri
                backgroundType = BackgroundType.IMAGE
                Toast.makeText(context, "Background image loaded successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Reset everything to clean defaults
    val resetToDefaults = {
        selectedImageUri = null
        autoMatchDeviceFrameRatio = true
        detectedScreenshotAspectRatio = 9f / 19.5f
        selectedDeviceFrameStyleId = "pixel_8_pro"
        selectedBackgroundUri = null
        screenshotScale = 1.0f
        screenshotOffsetX = 0f
        screenshotOffsetY = 0f
        deviceFrameScale = 0.72f
        backgroundType = BackgroundType.GRADIENT
        selectedSolidColor = PresetColors[0]
        customHue = 200f
        customSaturation = 0.8f
        customValue = 0.3f
        selectedGradientIndex = 0
        ambientBlurRadius = 15f
        backgroundBlurRadius = 0f
        liquidThemeIndex = 0
        liquidScale = 1.2f
        showDisplayGlassBlur = false
        displayGlassBlurColor = Color(0xFF00E5FF)
        displayGlassBlurOpacity = 0.4f
        liquidNoiseEnabled = true
        activeTemplate = MockupTemplate.MINIMAL_BORDER
        bezelColor = Color(0xFF00E5FF)
        bezelThickness = 5f
        screenCornerRadius = 32f
        showStatusBarIcons = true
        deviceFrameWidth = 300f
        deviceFrameHeight = 630f
        buttonStyle = "android"
        hasFoldLine = false
        activeRatio = CanvasRatio.PORTRAIT_9_16
        tiltX = 15f
        tiltY = -15f
        tiltZ = 5f
        perspectiveDepth = 8f
        shadowStrength = 0.4f
        shadowEnabled = true
        shadowBlurRadius = 25f
        shadowOffsetX = 10f
        shadowOffsetY = 15f
        shadowColor = Color.Black
        showWatermark = true
        watermarkText = "Sayanth Rock"
        watermarkPosition = WatermarkPosition.BOTTOM_CENTER
        watermarkColor = Color(0xBB00E5FF)
        watermarkSize = 14f
        watermarkOpacity = 0.7f
        previewZoom = 1.0f
        previewPanX = 0f
        previewPanY = 0f
        exportFormat = "PNG"
        exportQuality = 90f
        batchQueue.clear()
        undoStack.clear()
        redoStack.clear()
        lastSavedState = null
    }

    val processBatchQueue = {
        if (batchQueue.isNotEmpty()) {
            isBatchProcessing = true
            batchTotalItems = batchQueue.size
            batchProgressIndex = 0
            coroutineScope.launch {
                try {
                    for (i in 0 until batchQueue.size) {
                        val uri = batchQueue[i]
                        batchProgressIndex = i + 1
                        
                        // If autoMatchDeviceFrameRatio is enabled, dynamically calculate detected aspect ratio for this screenshot!
                        val actualDeviceFrameAspectRatio = if (autoMatchDeviceFrameRatio) {
                            getScreenshotAspectRatio(context, uri)
                        } else {
                            deviceFrameAspectRatio
                        }

                        val highResBitmap = renderHighResMockup(
                            context, activeRatio, backgroundType, selectedSolidColor, 
                            PremiumGradients.getOrNull(selectedGradientIndex) ?: PremiumGradients[0],
                            uri,
                            actualDeviceFrameAspectRatio,
                            ambientBlurRadius, screenshotScale, screenshotOffsetX, screenshotOffsetY,
                            activeTemplate, bezelColor, bezelThickness, screenCornerRadius, showStatusBarIcons,
                            tiltX, tiltY, tiltZ, perspectiveDepth, shadowStrength, showWatermark,
                            watermarkText, watermarkPosition, watermarkColor, watermarkSize, watermarkOpacity,
                            liquidThemeIndex, liquidScale, showDisplayGlassBlur, displayGlassBlurColor, displayGlassBlurOpacity,
                            deviceFrameScale = deviceFrameScale,
                            shadowEnabled = shadowEnabled,
                            shadowBlurRadius = shadowBlurRadius,
                            shadowOffsetX = shadowOffsetX,
                            shadowOffsetY = shadowOffsetY,
                            shadowColor = shadowColor,
                            backgroundUri = selectedBackgroundUri,
                            backgroundBlurRadius = backgroundBlurRadius,
                            showGlossyReflection = showGlossyReflection,
                            glossyReflectionOpacity = glossyReflectionOpacity,
                            buttonStyle = buttonStyle,
                            hasFoldLine = hasFoldLine
                        )

                        val batchExtension = when (exportFormat.uppercase()) {
                            "JPEG" -> "jpg"
                            "WEBP" -> "webp"
                            else -> "png"
                        }
                        val batchMimeType = when (exportFormat.uppercase()) {
                            "JPEG" -> "image/jpeg"
                            "WEBP" -> "image/webp"
                            else -> "image/png"
                        }
                        val batchCompressFormat = when (exportFormat.uppercase()) {
                            "JPEG" -> Bitmap.CompressFormat.JPEG
                            "WEBP" -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    if (exportQuality < 100f) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP_LOSSLESS
                                } else {
                                    @Suppress("DEPRECATION")
                                    Bitmap.CompressFormat.WEBP
                                }
                            }
                            else -> Bitmap.CompressFormat.PNG
                        }

                        // Save each bitmap to Public MediaStore
                        val filename = "HiShoot_Batch_${System.currentTimeMillis()}_$i.$batchExtension"
                        val resolver = context.contentResolver
                        val contentValues = ContentValues().apply {
                            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                            put(MediaStore.Images.Media.MIME_TYPE, batchMimeType)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/HiShootStudio")
                                put(MediaStore.Images.Media.IS_PENDING, 1)
                            }
                        }

                        val imageUriResult = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                        if (imageUriResult != null) {
                            resolver.openOutputStream(imageUriResult)?.use { outputStream ->
                                highResBitmap.compress(batchCompressFormat, exportQuality.toInt(), outputStream)
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                contentValues.clear()
                                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                                resolver.update(imageUriResult, contentValues, null, null)
                            }
                        }
                    }
                    Toast.makeText(context, "Successfully exported all $batchTotalItems mockups to Gallery!", Toast.LENGTH_LONG).show()
                    batchQueue.clear()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Batch export error: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isBatchProcessing = false
                }
            }
        }
    }

    var isSidebarOpen by remember { mutableStateOf(false) }

    Scaffold(
            modifier = Modifier.fillMaxSize().navigationBarsPadding(),
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                isSidebarOpen = !isSidebarOpen
                            },
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.04f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isSidebarOpen) Icons.Default.MenuOpen else Icons.Default.Menu,
                                contentDescription = "Open Sidebar Menu",
                                tint = Color(0xFF818CF8), // Matching primary Indigo
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "HiShoot Studio",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = Color.White,
                                        letterSpacing = 0.2.sp
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFF818CF8), Color(0xFFC084FC))
                                                )
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "PRO",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.Black,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                                Text(
                                    text = "Premium Device Mockup Canvas",
                                    fontSize = 10.5.sp,
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    },
                    actions = {
                        // Quick Device Frame Selection Dropdown Menu
                        Box {
                            Row(
                                modifier = Modifier
                                    .testTag("quick_device_dropdown_trigger")
                                    .padding(end = 12.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                    .clickable { isDeviceDropdownOpen = true }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = when {
                                        selectedStyle.isDesktop -> Icons.Default.Laptop
                                        selectedStyle.isTablet -> Icons.Default.TabletAndroid
                                        else -> Icons.Default.Smartphone
                                    },
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = selectedStyle.name.substringBefore(" ("),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Expand quick device styles menu",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = isDeviceDropdownOpen,
                                onDismissRequest = { isDeviceDropdownOpen = false },
                                modifier = Modifier
                                    .width(220.dp)
                                    .background(Color(0xFF0F172A))
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            ) {
                                DeviceFrameStylePresets.forEach { style ->
                                    val isSelected = style.id == selectedDeviceFrameStyleId
                                    DropdownMenuItem(
                                        modifier = Modifier.testTag("quick_device_item_${style.id}"),
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = when {
                                                        style.isDesktop -> Icons.Default.Laptop
                                                        style.isTablet -> Icons.Default.TabletAndroid
                                                        else -> Icons.Default.Smartphone
                                                    },
                                                    contentDescription = null,
                                                    tint = if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = style.name.substringBefore(" ("),
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        fontSize = 12.5.sp,
                                                        color = if (isSelected) Color(0xFF00E5FF) else Color.White
                                                    )
                                                    Text(
                                                        text = when {
                                                            style.isDesktop -> "Desktop"
                                                            style.isTablet -> "Tablet"
                                                            else -> "Phone"
                                                        },
                                                        fontSize = 9.sp,
                                                        color = Color.White.copy(alpha = 0.4f)
                                                    )
                                                }
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = Color(0xFF00E5FF),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedDeviceFrameStyleId = style.id
                                            activeTemplate = style.template
                                            bezelThickness = style.bezelThickness
                                            screenCornerRadius = style.screenCornerRadius
                                            bezelColor = style.bezelColor
                                            autoMatchDeviceFrameRatio = false
                                            isDeviceDropdownOpen = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Undo Button
                        val undoEnabled = undoStack.isNotEmpty()
                        IconButton(
                            onClick = { if (undoEnabled) performUndo() },
                            enabled = undoEnabled,
                            modifier = Modifier
                                .testTag("undo_button_topbar")
                                .padding(end = 4.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (undoEnabled) Color.White.copy(alpha = 0.06f) else Color.Transparent)
                                .border(1.dp, if (undoEnabled) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo Last Action",
                                tint = if (undoEnabled) Color(0xFF818CF8) else Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Redo Button
                        val redoEnabled = redoStack.isNotEmpty()
                        IconButton(
                            onClick = { if (redoEnabled) performRedo() },
                            enabled = redoEnabled,
                            modifier = Modifier
                                .testTag("redo_button_topbar")
                                .padding(end = 8.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (redoEnabled) Color.White.copy(alpha = 0.06f) else Color.Transparent)
                                .border(1.dp, if (redoEnabled) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Redo,
                                contentDescription = "Redo Undone Action",
                                tint = if (redoEnabled) Color(0xFFC084FC) else Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (!isSaving) {
                                    isSaving = true
                                    saveMockupImage(
                                        context = context,
                                        aspectRatio = activeRatio,
                                        backgroundType = backgroundType,
                                        solidColor = selectedSolidColor,
                                        gradient = PremiumGradients[selectedGradientIndex],
                                        imageUri = selectedImageUri,
                                        ambientBlurRadius = ambientBlurRadius,
                                        screenshotScale = screenshotScale,
                                        screenshotOffsetX = screenshotOffsetX,
                                        screenshotOffsetY = screenshotOffsetY,
                                        activeTemplate = activeTemplate,
                                        bezelColor = bezelColor,
                                        bezelThickness = bezelThickness,
                                        screenCornerRadius = screenCornerRadius,
                                        showStatusBarIcons = showStatusBarIcons,
                                        tiltX = tiltX,
                                        tiltY = tiltY,
                                        tiltZ = tiltZ,
                                        perspectiveDepth = perspectiveDepth,
                                        shadowStrength = shadowStrength,
                                        showWatermark = showWatermark,
                                        watermarkText = watermarkText,
                                        watermarkPosition = watermarkPosition,
                                        watermarkColor = watermarkColor,
                                        watermarkSize = watermarkSize,
                                        watermarkOpacity = watermarkOpacity,
                                        coroutineScope = coroutineScope,
                                        liquidThemeIndex = liquidThemeIndex,
                                        liquidScale = liquidScale,
                                        showDisplayGlassBlur = showDisplayGlassBlur,
                                        displayGlassBlurColor = displayGlassBlurColor,
                                        displayGlassBlurOpacity = displayGlassBlurOpacity,
                                        deviceFrameScale = deviceFrameScale,
                                        shadowEnabled = shadowEnabled,
                                        shadowBlurRadius = shadowBlurRadius,
                                        shadowOffsetX = shadowOffsetX,
                                        shadowOffsetY = shadowOffsetY,
                                        shadowColor = shadowColor,
                                        backgroundUri = selectedBackgroundUri,
                                        backgroundBlurRadius = backgroundBlurRadius,
                                        showGlossyReflection = showGlossyReflection,
                                        glossyReflectionOpacity = glossyReflectionOpacity,
                                        screenshotGrayscale = screenshotGrayscale,
                                        screenshotSepia = screenshotSepia,
                                        screenshotBrightness = screenshotBrightness,
                                        screenshotContrast = screenshotContrast,
                                        screenshotLiquidGlass = screenshotLiquidGlass,
                                        isDesktop = selectedStyle.isDesktop,
                                        buttonStyle = buttonStyle,
                                        hasFoldLine = hasFoldLine,
                                         exportFormat = exportFormat,
                                         exportQuality = exportQuality
                                    ) {
                                        isSaving = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .testTag("download_button_topbar")
                                .padding(end = 4.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF34D399).copy(alpha = 0.12f))
                                .border(1.dp, Color(0xFF34D399).copy(alpha = 0.3f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download Mockup as High-Res PNG",
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = resetToDefaults,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.04f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Layout Parameters",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0B0F19),
                        titleContentColor = Color.White
                    )
                )
            }
        ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedVisibility(
                visible = isSidebarOpen,
                enter = androidx.compose.animation.slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                exit = androidx.compose.animation.slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
            ) {
                DeviceFrameSidebar(
                    selectedStyleId = selectedDeviceFrameStyleId,
                    onStyleSelect = { style ->
                        selectedDeviceFrameStyleId = style.id
                        activeTemplate = style.template
                        bezelThickness = style.bezelThickness
                        screenCornerRadius = style.screenCornerRadius
                        bezelColor = style.bezelColor
                        deviceFrameWidth = style.width
                        deviceFrameHeight = style.height
                        buttonStyle = style.buttonStyle
                        hasFoldLine = style.hasFoldLine
                        autoMatchDeviceFrameRatio = false
                    },
                    onClose = { isSidebarOpen = false },
                    backgroundType = backgroundType,
                    onBackgroundTypeChange = { backgroundType = it },
                    selectedSolidColor = selectedSolidColor,
                    onSolidColorChange = { selectedSolidColor = it },
                    customHue = customHue,
                    onCustomHueChange = { customHue = it },
                    customSaturation = customSaturation,
                    onCustomSaturationChange = { customSaturation = it },
                    customValue = customValue,
                    onCustomValueChange = { customValue = it },
                    selectedGradientIndex = selectedGradientIndex,
                    onGradientIndexChange = { selectedGradientIndex = it },
                    selectedPatternType = selectedPatternType,
                    onPatternTypeChange = { selectedPatternType = it },
                    patternPrimaryColorHue = patternPrimaryColorHue,
                    onPatternPrimaryColorHueChange = { patternPrimaryColorHue = it },
                    patternSecondaryColorHue = patternSecondaryColorHue,
                    onPatternSecondaryColorHueChange = { patternSecondaryColorHue = it },
                    patternBgColor = patternBgColor,
                    onPatternBgColorChange = { patternBgColor = it },
                    patternScale = patternScale,
                    onPatternScaleChange = { patternScale = it },
                    shadowEnabled = shadowEnabled,
                    onShadowEnabledChange = { shadowEnabled = it },
                    shadowStrength = shadowStrength,
                    onShadowStrengthChange = { shadowStrength = it },
                    shadowBlurRadius = shadowBlurRadius,
                    onShadowBlurRadiusChange = { shadowBlurRadius = it },
                    shadowOffsetX = shadowOffsetX,
                    onShadowOffsetXChange = { shadowOffsetX = it },
                    shadowOffsetY = shadowOffsetY,
                    onShadowOffsetYChange = { shadowOffsetY = it },
                    shadowColor = shadowColor,
                    onShadowColorChange = { shadowColor = it }
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Horizontal Popular Device Selection Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "QUICK FRAME:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.4f),
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    DeviceFrameStylePresets.forEach { style ->
                        val isSelected = style.id == selectedDeviceFrameStyleId
                        Row(
                            modifier = Modifier
                                .testTag("quick_chip_${style.id}")
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.15f)
                                    else Color.White.copy(alpha = 0.04f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    selectedDeviceFrameStyleId = style.id
                                    activeTemplate = style.template
                                    bezelThickness = style.bezelThickness
                                    screenCornerRadius = style.screenCornerRadius
                                    bezelColor = style.bezelColor
                                    autoMatchDeviceFrameRatio = false
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    style.isDesktop -> Icons.Default.Laptop
                                    style.isTablet -> Icons.Default.TabletAndroid
                                    else -> Icons.Default.Smartphone
                                },
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = style.name.substringBefore(" ("),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF00E5FF) else Color.White
                            )
                        }
                    }
                }

                // Adaptive Grid / Splitted Area: Top: Preview, Bottom: Sliders & Controls
            Box(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0F172A)) // Deep space slate background
                    .studioGridBackground()
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Zoomable & Pannable wrapper
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoomChange, _ ->
                                val newZoom = (previewZoom * zoomChange).coerceIn(0.5f, 4.0f)
                                previewZoom = newZoom
                                if (newZoom > 1.0f) {
                                    previewPanX += pan.x * newZoom
                                    previewPanY += pan.y * newZoom
                                } else {
                                    previewPanX = 0f
                                    previewPanY = 0f
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = previewZoom
                                scaleY = previewZoom
                                translationX = previewPanX
                                translationY = previewPanY
                            }
                    ) {
                        MockupCanvasContainer(
                            aspectRatio = activeRatio,
                            backgroundType = backgroundType,
                            solidColor = selectedSolidColor,
                            gradient = PremiumGradients[selectedGradientIndex],
                            imageUri = selectedImageUri,
                            deviceFrameAspectRatio = deviceFrameAspectRatio,
                            ambientBlurRadius = ambientBlurRadius,
                            screenshotScale = screenshotScale,
                            screenshotOffsetX = screenshotOffsetX,
                            screenshotOffsetY = screenshotOffsetY,
                            activeTemplate = activeTemplate,
                            isDesktop = selectedStyle.isDesktop,
                            bezelColor = bezelColor,
                            bezelThickness = bezelThickness,
                            screenCornerRadius = screenCornerRadius,
                            showStatusBarIcons = showStatusBarIcons,
                            tiltX = tiltX,
                            tiltY = tiltY,
                            tiltZ = tiltZ,
                            perspectiveDepth = perspectiveDepth,
                            shadowStrength = shadowStrength,
                            showWatermark = showWatermark,
                            watermarkText = watermarkText,
                            watermarkPosition = watermarkPosition,
                            watermarkColor = watermarkColor,
                            watermarkSize = watermarkSize,
                            watermarkOpacity = watermarkOpacity,
                            liquidThemeIndex = liquidThemeIndex,
                            liquidScale = liquidScale,
                            showDisplayGlassBlur = showDisplayGlassBlur,
                            displayGlassBlurColor = displayGlassBlurColor,
                            displayGlassBlurOpacity = displayGlassBlurOpacity,
                            liquidNoiseEnabled = liquidNoiseEnabled,
                            deviceFrameScale = deviceFrameScale,
                            shadowEnabled = shadowEnabled,
                            shadowBlurRadius = shadowBlurRadius,
                            shadowOffsetX = shadowOffsetX,
                            shadowOffsetY = shadowOffsetY,
                            shadowColor = shadowColor,
                            backgroundUri = selectedBackgroundUri,
                            backgroundBlurRadius = backgroundBlurRadius,
                            showGlossyReflection = showGlossyReflection,
                            glossyReflectionOpacity = glossyReflectionOpacity,
                            screenshotGrayscale = screenshotGrayscale,
                            screenshotSepia = screenshotSepia,
                            screenshotBrightness = screenshotBrightness,
                            screenshotContrast = screenshotContrast,
                            screenshotLiquidGlass = screenshotLiquidGlass,
                            buttonStyle = buttonStyle,
                            hasFoldLine = hasFoldLine,
                            onCanvasClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                    }
                }

                // Floating Zoom Slider and Export Dock
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(14.dp)
                        .fillMaxWidth(0.95f)
                        .pointerInput(Unit) {}, // prevent clicks on controls from triggering picker
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Glassmorphic Zoom Control Bar
                    Surface(
                        color = Color(0xCC0B0F19), // Match theme background with opacity
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = "Zoom Scale",
                                tint = Color(0xFF818CF8),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${(previewZoom * 100).roundToInt()}%",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.width(36.dp)
                            )
                            Slider(
                                value = previewZoom,
                                onValueChange = {
                                    previewZoom = it
                                    if (it <= 1.0f) {
                                        previewPanX = 0f
                                        previewPanY = 0f
                                    }
                                },
                                valueRange = 0.5f..3.0f,
                                modifier = Modifier
                                    .width(90.dp)
                                    .height(20.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF818CF8),
                                    activeTrackColor = Color(0xFF818CF8),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                                )
                            )
                            if (previewZoom != 1.0f || previewPanX != 0f || previewPanY != 0f) {
                                IconButton(
                                    onClick = {
                                        previewZoom = 1.0f
                                        previewPanX = 0f
                                        previewPanY = 0f
                                    },
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ZoomOutMap,
                                        contentDescription = "Reset Zoom & Pan",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Premium Gradient Export Button
                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF34D399), Color(0xFF059669)) // Neon emerald to deep forest
                                )
                            )
                            .clickable {
                                if (!isSaving) {
                                    isSaving = true
                                    saveMockupImage(
                                        context = context,
                                        aspectRatio = activeRatio,
                                        backgroundType = backgroundType,
                                        solidColor = selectedSolidColor,
                                        gradient = PremiumGradients[selectedGradientIndex],
                                        imageUri = selectedImageUri,
                                        deviceFrameAspectRatio = deviceFrameAspectRatio,
                                        ambientBlurRadius = ambientBlurRadius,
                                        screenshotScale = screenshotScale,
                                        screenshotOffsetX = screenshotOffsetX,
                                        screenshotOffsetY = screenshotOffsetY,
                                        activeTemplate = activeTemplate,
                                        bezelColor = bezelColor,
                                        bezelThickness = bezelThickness,
                                        screenCornerRadius = screenCornerRadius,
                                        showStatusBarIcons = showStatusBarIcons,
                                        tiltX = tiltX,
                                        tiltY = tiltY,
                                        tiltZ = tiltZ,
                                        perspectiveDepth = perspectiveDepth,
                                        shadowStrength = shadowStrength,
                                        showWatermark = showWatermark,
                                        watermarkText = watermarkText,
                                        watermarkPosition = watermarkPosition,
                                        watermarkColor = watermarkColor,
                                        watermarkSize = watermarkSize,
                                        watermarkOpacity = watermarkOpacity,
                                        coroutineScope = coroutineScope,
                                        liquidThemeIndex = liquidThemeIndex,
                                        liquidScale = liquidScale,
                                        showDisplayGlassBlur = showDisplayGlassBlur,
                                        displayGlassBlurColor = displayGlassBlurColor,
                                        displayGlassBlurOpacity = displayGlassBlurOpacity,
                                        deviceFrameScale = deviceFrameScale,
                                        shadowEnabled = shadowEnabled,
                                        shadowBlurRadius = shadowBlurRadius,
                                        shadowOffsetX = shadowOffsetX,
                                        shadowOffsetY = shadowOffsetY,
                                        shadowColor = shadowColor,
                                        backgroundUri = selectedBackgroundUri,
                                        backgroundBlurRadius = backgroundBlurRadius,
                                        showGlossyReflection = showGlossyReflection,
                                        glossyReflectionOpacity = glossyReflectionOpacity,
                                        screenshotGrayscale = screenshotGrayscale,
                                        screenshotSepia = screenshotSepia,
                                        screenshotBrightness = screenshotBrightness,
                                        screenshotContrast = screenshotContrast,
                                        screenshotLiquidGlass = screenshotLiquidGlass,
                                        isDesktop = selectedStyle.isDesktop,
                                        buttonStyle = buttonStyle,
                                        hasFoldLine = hasFoldLine,
                                         exportFormat = exportFormat,
                                         exportQuality = exportQuality
                                    ) {
                                        isSaving = false
                                    }
                                }
                            }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Export PNG",
                                tint = Color(0xFF0B0F19), // high-contrast dark color on bright green background
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Export PNG".uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0B0F19),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                // Indication bubble if saving
                if (isSaving) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(24.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(color = Color(0xFF00E5FF))
                                Text("Rendering mockup in high-res...", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                if (isBatchProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.padding(32.dp).fillMaxWidth().testTag("batch_processing_dialog")
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(color = Color(0xFF00E5FF))
                                Text(
                                    text = "Batch Processing Queue",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Processing screenshot $batchProgressIndex of $batchTotalItems...",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                val fraction = if (batchTotalItems > 0) batchProgressIndex.toFloat() / batchTotalItems else 0f
                                LinearProgressIndicator(
                                    progress = fraction,
                                    color = Color(0xFF00E5FF),
                                    trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                                )
                                Text(
                                    text = "Exported files are being saved to your gallery.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // CONTROLS CATEGORY TAB ROW (Premium Pill Style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0B0F19))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabCategory.values().forEach { tab ->
                    val isSelected = activeTab == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) Color(0xFF818CF8).copy(alpha = 0.15f)
                                else Color.White.copy(alpha = 0.03f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFF818CF8).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.06f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { activeTab = tab }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) Color(0xFF818CF8) else Color.White.copy(alpha = 0.5f)
                            )
                            Text(
                                text = tab.label,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // CONTROLS DISPLAY CONTAINER
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    when (activeTab) {
                        TabCategory.SCREENSHOT -> {
                            ScreenshotTabContent(
                                selectedImageUri = selectedImageUri,
                                screenshotScale = screenshotScale,
                                screenshotOffsetX = screenshotOffsetX,
                                screenshotOffsetY = screenshotOffsetY,
                                autoMatchDeviceFrameRatio = autoMatchDeviceFrameRatio,
                                onAutoMatchDeviceFrameRatioChange = { autoMatchDeviceFrameRatio = it },
                                detectedScreenshotAspectRatio = detectedScreenshotAspectRatio,
                                onScaleChange = { screenshotScale = it },
                                onOffsetXChange = { screenshotOffsetX = it },
                                onOffsetYChange = { screenshotOffsetY = it },
                                screenshotGrayscale = screenshotGrayscale,
                                onGrayscaleChange = { screenshotGrayscale = it },
                                screenshotSepia = screenshotSepia,
                                onSepiaChange = { screenshotSepia = it },
                                screenshotBrightness = screenshotBrightness,
                                onBrightnessChange = { screenshotBrightness = it },
                                screenshotContrast = screenshotContrast,
                                onContrastChange = { screenshotContrast = it },
                                screenshotLiquidGlass = screenshotLiquidGlass,
                                onLiquidGlassChange = { screenshotLiquidGlass = it },
                                onPickImage = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onPickFile = {
                                    filePickerLauncher.launch("image/*")
                                },
                                onClearImage = { selectedImageUri = null },
                                onReset = {
                                    showGlossyReflection = true
                                    glossyReflectionOpacity = 0.18f
                                    selectedImageUri = null
                                    autoMatchDeviceFrameRatio = true
                                    detectedScreenshotAspectRatio = 9f / 19.5f
                                    selectedDeviceFrameStyleId = "pixel_8_pro"
                                    activeTemplate = MockupTemplate.MINIMAL_BORDER
                                    bezelColor = Color(0xFF00E5FF)
                                    bezelThickness = 5f
                                    screenCornerRadius = 32f
                                    showStatusBarIcons = true
                                    screenshotScale = 1.0f
                                    screenshotOffsetX = 0f
                                    screenshotOffsetY = 0f
                                    screenshotGrayscale = 0f
                                    screenshotSepia = 0f
                                    screenshotBrightness = 1f
                                    screenshotContrast = 1f
                                    screenshotLiquidGlass = false
                                }
                            )
                        }
                        TabCategory.BACKGROUND -> {
                            BackgroundTabContent(
                                backgroundType = backgroundType,
                                selectedSolidColor = selectedSolidColor,
                                selectedGradientIndex = selectedGradientIndex,
                                ambientBlurRadius = ambientBlurRadius,
                                backgroundBlurRadius = backgroundBlurRadius,
                                customHue = customHue,
                                customSaturation = customSaturation,
                                customValue = customValue,
                                hasScreenshot = selectedImageUri != null,
                                selectedBackgroundUri = selectedBackgroundUri,
                                onPickBackground = {
                                    backgroundPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onClearBackground = { selectedBackgroundUri = null },
                                onBackgroundTypeChange = { backgroundType = it },
                                onSolidColorSelect = { color ->
                                    selectedSolidColor = color
                                    val hsv = FloatArray(3)
                                    android.graphics.Color.colorToHSV(
                                        android.graphics.Color.argb(
                                            (color.alpha * 255f).roundToInt(),
                                            (color.red * 255f).roundToInt(),
                                            (color.green * 255f).roundToInt(),
                                            (color.blue * 255f).roundToInt()
                                        ),
                                        hsv
                                    )
                                    customHue = hsv[0]
                                    customSaturation = hsv[1]
                                    customValue = hsv[2]
                                },
                                onGradientSelect = { selectedGradientIndex = it },
                                onBlurRadiusChange = { ambientBlurRadius = it },
                                onBackgroundBlurRadiusChange = { backgroundBlurRadius = it },
                                onHueChange = { customHue = it },
                                onSaturationChange = { customSaturation = it },
                                onValueChange = { customValue = it },
                                liquidThemeIndex = liquidThemeIndex,
                                liquidScale = liquidScale,
                                showDisplayGlassBlur = showDisplayGlassBlur,
                                displayGlassBlurColor = displayGlassBlurColor,
                                displayGlassBlurOpacity = displayGlassBlurOpacity,
                                liquidNoiseEnabled = liquidNoiseEnabled,
                                onLiquidThemeChange = { liquidThemeIndex = it },
                                onLiquidScaleChange = { liquidScale = it },
                                onShowDisplayGlassBlurChange = { showDisplayGlassBlur = it },
                                onDisplayGlassBlurColorChange = { displayGlassBlurColor = it },
                                onDisplayGlassBlurOpacityChange = { displayGlassBlurOpacity = it },
                                onLiquidNoiseChange = { liquidNoiseEnabled = it }
                            )
                        }
                        TabCategory.TEMPLATE -> {
                            TemplateTabContent(
                                selectedDeviceFrameStyleId = selectedDeviceFrameStyleId,
                                onDeviceFrameStyleChange = { styleId ->
                                    selectedDeviceFrameStyleId = styleId
                                    DeviceFrameStylePresets.find { it.id == styleId }?.let { style ->
                                        activeTemplate = style.template
                                        bezelThickness = style.bezelThickness
                                        screenCornerRadius = style.screenCornerRadius
                                        bezelColor = style.bezelColor
                                        deviceFrameWidth = style.width
                                        deviceFrameHeight = style.height
                                        buttonStyle = style.buttonStyle
                                        hasFoldLine = style.hasFoldLine
                                        autoMatchDeviceFrameRatio = false
                                    }
                                },
                                activeTemplate = activeTemplate,
                                bezelColor = bezelColor,
                                bezelThickness = bezelThickness,
                                screenCornerRadius = screenCornerRadius,
                                showStatusBarIcons = showStatusBarIcons,
                                showGlossyReflection = showGlossyReflection,
                                glossyReflectionOpacity = glossyReflectionOpacity,
                                deviceFrameScale = deviceFrameScale,
                                onDeviceFrameScaleChange = { deviceFrameScale = it },
                                onTemplateChange = { activeTemplate = it },
                                onBezelColorChange = { bezelColor = it },
                                onThicknessChange = { bezelThickness = it },
                                onCornerRadiusChange = { screenCornerRadius = it },
                                onShowStatusBarChange = { showStatusBarIcons = it },
                                onShowGlossyReflectionChange = { showGlossyReflection = it },
                                onGlossyReflectionOpacityChange = { glossyReflectionOpacity = it },
                                onOpenSidebar = {
                                    isSidebarOpen = true
                                },
                                deviceFrameWidth = deviceFrameWidth,
                                deviceFrameHeight = deviceFrameHeight,
                                buttonStyle = buttonStyle,
                                hasFoldLine = hasFoldLine,
                                onDeviceFrameWidthChange = { deviceFrameWidth = it },
                                onDeviceFrameHeightChange = { deviceFrameHeight = it },
                                onButtonStyleChange = { buttonStyle = it },
                                onHasFoldLineChange = { hasFoldLine = it }
                            )
                        }
                        TabCategory.CANVAS -> {
                            CanvasTabContent(
                                activeRatio = activeRatio,
                                tiltX = tiltX,
                                tiltY = tiltY,
                                tiltZ = tiltZ,
                                perspectiveDepth = perspectiveDepth,
                                shadowStrength = shadowStrength,
                                deviceFrameScale = deviceFrameScale,
                                shadowEnabled = shadowEnabled,
                                shadowBlurRadius = shadowBlurRadius,
                                shadowOffsetX = shadowOffsetX,
                                shadowOffsetY = shadowOffsetY,
                                shadowColor = shadowColor,
                                onRatioChange = { activeRatio = it },
                                onTiltXChange = { tiltX = it },
                                onTiltYChange = { tiltY = it },
                                onTiltZChange = { tiltZ = it },
                                onPerspectiveChange = { perspectiveDepth = it },
                                onShadowStrengthChange = { shadowStrength = it },
                                onDeviceFrameScaleChange = { deviceFrameScale = it },
                                onShadowEnabledChange = { shadowEnabled = it },
                                onShadowBlurRadiusChange = { shadowBlurRadius = it },
                                onShadowOffsetXChange = { shadowOffsetX = it },
                                onShadowOffsetYChange = { shadowOffsetY = it },
                                onShadowColorChange = { shadowColor = it }
                            )
                        }
                        TabCategory.WATERMARK -> {
                            WatermarkTabContent(
                                showWatermark = showWatermark,
                                watermarkText = watermarkText,
                                watermarkPosition = watermarkPosition,
                                watermarkColor = watermarkColor,
                                watermarkSize = watermarkSize,
                                watermarkOpacity = watermarkOpacity,
                                onShowWatermarkChange = { showWatermark = it },
                                onTextChange = { watermarkText = it },
                                onPositionChange = { watermarkPosition = it },
                                onColorChange = { watermarkColor = it },
                                onSizeChange = { watermarkSize = it },
                                onOpacityChange = { watermarkOpacity = it }
                            )
                        }
                        TabCategory.EXPORT -> {
                            ExportTabContent(
                                exportFormat = exportFormat,
                                exportQuality = exportQuality,
                                onExportFormatChange = { exportFormat = it },
                                onExportQualityChange = { exportQuality = it }
                            )
                        }
                        TabCategory.BATCH -> {
                            BatchTabContent(
                                batchQueue = batchQueue,
                                autoMatchDeviceFrameRatio = autoMatchDeviceFrameRatio,
                                onAutoMatchDeviceFrameRatioChange = { autoMatchDeviceFrameRatio = it },
                                onPickScreenshots = {
                                    batchPhotoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onRemoveScreenshot = { uri -> batchQueue.remove(uri) },
                                onClearQueue = { batchQueue.clear() },
                                onProcessBatch = { processBatchQueue() }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // SPEED-SAVE FAB DOCKED AT BOTTOM RIGHT
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Share icon
                    FloatingActionButton(
                        onClick = {
                            if (!isSaving) {
                                isSaving = true
                                shareMockupImage(
                                    context = context,
                                    aspectRatio = activeRatio,
                                    backgroundType = backgroundType,
                                    solidColor = selectedSolidColor,
                                    gradient = PremiumGradients[selectedGradientIndex],
                                    imageUri = selectedImageUri,
                                    deviceFrameAspectRatio = deviceFrameAspectRatio,
                                    ambientBlurRadius = ambientBlurRadius,
                                    screenshotScale = screenshotScale,
                                    screenshotOffsetX = screenshotOffsetX,
                                    screenshotOffsetY = screenshotOffsetY,
                                    activeTemplate = activeTemplate,
                                    bezelColor = bezelColor,
                                    bezelThickness = bezelThickness,
                                    screenCornerRadius = screenCornerRadius,
                                    showStatusBarIcons = showStatusBarIcons,
                                    tiltX = tiltX,
                                    tiltY = tiltY,
                                    tiltZ = tiltZ,
                                    perspectiveDepth = perspectiveDepth,
                                    shadowStrength = shadowStrength,
                                    showWatermark = showWatermark,
                                    watermarkText = watermarkText,
                                    watermarkPosition = watermarkPosition,
                                    watermarkColor = watermarkColor,
                                    watermarkSize = watermarkSize,
                                    watermarkOpacity = watermarkOpacity,
                                    coroutineScope = coroutineScope,
                                    liquidThemeIndex = liquidThemeIndex,
                                    liquidScale = liquidScale,
                                    showDisplayGlassBlur = showDisplayGlassBlur,
                                    displayGlassBlurColor = displayGlassBlurColor,
                                    displayGlassBlurOpacity = displayGlassBlurOpacity,
                                    deviceFrameScale = deviceFrameScale,
                                    shadowEnabled = shadowEnabled,
                                    shadowBlurRadius = shadowBlurRadius,
                                    shadowOffsetX = shadowOffsetX,
                                    shadowOffsetY = shadowOffsetY,
                                    shadowColor = shadowColor,
                                    backgroundUri = selectedBackgroundUri,
                                    backgroundBlurRadius = backgroundBlurRadius,
                                    showGlossyReflection = showGlossyReflection,
                                    glossyReflectionOpacity = glossyReflectionOpacity,
                                    screenshotGrayscale = screenshotGrayscale,
                                    screenshotSepia = screenshotSepia,
                                    screenshotBrightness = screenshotBrightness,
                                    screenshotContrast = screenshotContrast,
                                    screenshotLiquidGlass = screenshotLiquidGlass,
                                    isDesktop = selectedStyle.isDesktop,
                                    buttonStyle = buttonStyle,
                                    hasFoldLine = hasFoldLine,
                                    exportFormat = exportFormat,
                                    exportQuality = exportQuality
                                ) {
                                    isSaving = false
                                }
                            }
                        },
                        containerColor = Color(0xFF334155),
                        contentColor = Color.White,
                        modifier = Modifier.testTag("share_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share Mockup")
                    }

                    // Download high-res PNG
                    ExtendedFloatingActionButton(
                        onClick = {
                            if (!isSaving) {
                                isSaving = true
                                 saveMockupImage(
                                    context = context,
                                    aspectRatio = activeRatio,
                                    backgroundType = backgroundType,
                                    solidColor = selectedSolidColor,
                                    gradient = PremiumGradients[selectedGradientIndex],
                                    imageUri = selectedImageUri,
                                    deviceFrameAspectRatio = deviceFrameAspectRatio,
                                    ambientBlurRadius = ambientBlurRadius,
                                    screenshotScale = screenshotScale,
                                    screenshotOffsetX = screenshotOffsetX,
                                    screenshotOffsetY = screenshotOffsetY,
                                    activeTemplate = activeTemplate,
                                    bezelColor = bezelColor,
                                    bezelThickness = bezelThickness,
                                    screenCornerRadius = screenCornerRadius,
                                    showStatusBarIcons = showStatusBarIcons,
                                    tiltX = tiltX,
                                    tiltY = tiltY,
                                    tiltZ = tiltZ,
                                    perspectiveDepth = perspectiveDepth,
                                    shadowStrength = shadowStrength,
                                    showWatermark = showWatermark,
                                    watermarkText = watermarkText,
                                    watermarkPosition = watermarkPosition,
                                    watermarkColor = watermarkColor,
                                    watermarkSize = watermarkSize,
                                    watermarkOpacity = watermarkOpacity,
                                    coroutineScope = coroutineScope,
                                    liquidThemeIndex = liquidThemeIndex,
                                    liquidScale = liquidScale,
                                    showDisplayGlassBlur = showDisplayGlassBlur,
                                    displayGlassBlurColor = displayGlassBlurColor,
                                    displayGlassBlurOpacity = displayGlassBlurOpacity,
                                    deviceFrameScale = deviceFrameScale,
                                    shadowEnabled = shadowEnabled,
                                    shadowBlurRadius = shadowBlurRadius,
                                    shadowOffsetX = shadowOffsetX,
                                    shadowOffsetY = shadowOffsetY,
                                    shadowColor = shadowColor,
                                    backgroundUri = selectedBackgroundUri,
                                    showGlossyReflection = showGlossyReflection,
                                    glossyReflectionOpacity = glossyReflectionOpacity,
                                    screenshotGrayscale = screenshotGrayscale,
                                    screenshotSepia = screenshotSepia,
                                    screenshotBrightness = screenshotBrightness,
                                    screenshotContrast = screenshotContrast,
                                    screenshotLiquidGlass = screenshotLiquidGlass,
                                    isDesktop = selectedStyle.isDesktop,
                                    buttonStyle = buttonStyle,
                                    hasFoldLine = hasFoldLine,
                                    exportFormat = exportFormat,
                                    exportQuality = exportQuality
                                ) {
                                    isSaving = false
                                }
                            }
                        },
                        containerColor = Color(0xFF4CAF50), // Vibrant Green download action
                        contentColor = Color.White,
                        icon = { Icon(Icons.Default.Download, contentDescription = "Download Mockup as $exportFormat") },
                        text = { Text("Download $exportFormat", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("download_button")
                    )
                }
            }
            }
        }
    }
}

// Helper to wrap Icon sizing
@Composable
fun Icon(imageVector: ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = Modifier.size(size)
    )
}

// ==========================================
// PREVIEW RENDERING ENGINE
// ==========================================

@Composable
fun MockupCanvasContainer(
    aspectRatio: CanvasRatio,
    backgroundType: BackgroundType,
    solidColor: Color,
    gradient: CustomGradient,
    imageUri: Uri?,
    deviceFrameAspectRatio: Float = 9f / 19.5f,
    ambientBlurRadius: Float,
    screenshotScale: Float,
    screenshotOffsetX: Float,
    screenshotOffsetY: Float,
    activeTemplate: MockupTemplate,
    isDesktop: Boolean = false,
    bezelColor: Color,
    bezelThickness: Float,
    screenCornerRadius: Float,
    showStatusBarIcons: Boolean,
    tiltX: Float,
    tiltY: Float,
    tiltZ: Float,
    perspectiveDepth: Float,
    shadowStrength: Float,
    showWatermark: Boolean,
    watermarkText: String,
    watermarkPosition: WatermarkPosition,
    watermarkColor: Color,
    watermarkSize: Float,
    watermarkOpacity: Float,
    liquidThemeIndex: Int = 0,
    liquidScale: Float = 1.2f,
    showDisplayGlassBlur: Boolean = false,
    displayGlassBlurColor: Color = Color(0xFF00E5FF),
    displayGlassBlurOpacity: Float = 0.4f,
    liquidNoiseEnabled: Boolean = true,
    deviceFrameScale: Float = 0.72f,
    shadowEnabled: Boolean = true,
    shadowBlurRadius: Float = 25f,
    shadowOffsetX: Float = 10f,
    shadowOffsetY: Float = 15f,
    shadowColor: Color = Color.Black,
    backgroundUri: Uri? = null,
    backgroundBlurRadius: Float = 0f,
    showGlossyReflection: Boolean = true,
    glossyReflectionOpacity: Float = 0.18f,
    screenshotGrayscale: Float = 0f,
    screenshotSepia: Float = 0f,
    screenshotBrightness: Float = 1f,
    screenshotContrast: Float = 1f,
    screenshotLiquidGlass: Boolean = false,
    buttonStyle: String = "android",
    hasFoldLine: Boolean = false,
    onCanvasClick: () -> Unit
) {
    val context = LocalContext.current
    var blurredBackgroundBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Re-render ambient blur whenever screenshot or radius changes
    LaunchedEffect(imageUri, ambientBlurRadius, backgroundType) {
        if (imageUri != null && backgroundType == BackgroundType.AMBIENT_BLUR) {
            withContext(Dispatchers.IO) {
                try {
                    val rawBitmap = uriToBitmap(context, imageUri)
                    if (rawBitmap != null) {
                        // Scale down dramatically for super-fast blur computation
                        val scaled = Bitmap.createScaledBitmap(rawBitmap, 200, (200 * (16f/9f)).roundToInt(), true)
                        val blurred = blurBitmap(scaled, ambientBlurRadius.roundToInt().coerceIn(1, 25))
                        blurredBackgroundBitmap = blurred
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            blurredBackgroundBitmap = null
        }
    }

    // Canvas Aspect ratio container with premium Material 3 elevation shadow styling
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(aspectRatio.ratio)
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(20.dp),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.5f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(20.dp))
            .testTag("preview_canvas")
            .clickable { onCanvasClick() }
    ) {
        // 1. DRAW BACKGROUND LAYER
        when (backgroundType) {
            BackgroundType.SOLID -> {
                Box(modifier = Modifier.fillMaxSize().background(solidColor))
            }
            BackgroundType.GRADIENT -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(gradient.colors))
                )
            }
            BackgroundType.PATTERN -> {
                CustomPatternBackground(
                    patternType = PatternConfig.selectedPatternType,
                    primaryColor = PatternConfig.patternPrimaryColor,
                    secondaryColor = PatternConfig.patternSecondaryColor,
                    bgColor = PatternConfig.patternBgColor,
                    scale = PatternConfig.patternScale,
                    modifier = Modifier.fillMaxSize()
                )
            }
            BackgroundType.AMBIENT_BLUR -> {
                if (blurredBackgroundBitmap != null) {
                    AsyncImage(
                        model = blurredBackgroundBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(PremiumGradients[0].colors))
                    )
                }
            }
            BackgroundType.LIQUID_FLOW -> {
                LiquidFlowBackground(
                    themeIndex = liquidThemeIndex,
                    noiseEnabled = liquidNoiseEnabled,
                    scale = liquidScale,
                    modifier = Modifier.fillMaxSize()
                )
            }
            BackgroundType.IMAGE -> {
                if (backgroundUri != null) {
                    AsyncImage(
                        model = backgroundUri,
                        contentDescription = "Background Backdrop Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .let {
                                if (backgroundBlurRadius > 0f) {
                                    it.blur(backgroundBlurRadius.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                                } else {
                                    it
                                }
                            }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(PremiumGradients[0].colors)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Select a Backdrop Image in Background tab\nto display your scene background.",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Add a subtle diagonal lens flare highlight on background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
        )

        // 2. THE DEVICE FRAME WITH 3D ISOMETRIC GRAPHICS LAYER TILT
        val desktopOverallAspectRatio = deviceFrameAspectRatio * 0.82f
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Customizable Drop Shadow Layer behind the device frame
            if (shadowEnabled && shadowStrength > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(deviceFrameScale)
                        .aspectRatio(if (isDesktop) desktopOverallAspectRatio else deviceFrameAspectRatio)
                        .graphicsLayer {
                            rotationX = -tiltX
                            rotationY = tiltY
                            rotationZ = tiltZ
                            cameraDistance = perspectiveDepth
                            translationX = shadowOffsetX.dp.toPx()
                            translationY = shadowOffsetY.dp.toPx()
                            shadowElevation = (shadowBlurRadius * shadowStrength).dp.toPx()
                            spotShadowColor = shadowColor
                            ambientShadowColor = shadowColor
                        }
                        .background(shadowColor.copy(alpha = shadowStrength), RoundedCornerShape(screenCornerRadius.dp))
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth(deviceFrameScale)
                    .aspectRatio(if (isDesktop) desktopOverallAspectRatio else deviceFrameAspectRatio)
                    .graphicsLayer {
                        rotationX = -tiltX
                        rotationY = tiltY
                        rotationZ = tiltZ
                        cameraDistance = perspectiveDepth
                        // Fallback shadow projection if customizable shadow is disabled
                        if (!shadowEnabled && shadowStrength > 0.01f) {
                            shadowElevation = (16 * shadowStrength).dp.toPx()
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(bezelColor, RoundedCornerShape(screenCornerRadius.dp))
                        .border(
                            bezelThickness.dp / 3,
                            bezelColor.copy(alpha = 0.85f),
                            RoundedCornerShape(screenCornerRadius.dp)
                        )
                        .padding((bezelThickness / 2).dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Physical hardware buttons overlay (protruding slightly from outer bezel edge)
                    if (buttonStyle == "iphone") {
                        // Left: Silent & Volume Buttons
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = (-4).dp, y = 100.dp)
                                .size(4.dp, 30.dp)
                                .background(bezelColor.copy(alpha = 0.9f), RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp))
                                .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp))
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = (-4).dp, y = 145.dp)
                                .size(4.dp, 50.dp)
                                .background(bezelColor.copy(alpha = 0.9f), RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp))
                                .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp))
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = (-4).dp, y = 205.dp)
                                .size(4.dp, 50.dp)
                                .background(bezelColor.copy(alpha = 0.9f), RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp))
                                .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp))
                        )
                        // Right: Power Button
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = 165.dp)
                                .size(4.dp, 75.dp)
                                .background(bezelColor.copy(alpha = 0.9f), RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                                .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                        )
                    } else if (buttonStyle == "android") {
                        // Right: Volume & Power Buttons
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = 120.dp)
                                .size(4.dp, 60.dp)
                                .background(bezelColor.copy(alpha = 0.9f), RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                                .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = 195.dp)
                                .size(4.dp, 40.dp)
                                .background(bezelColor.copy(alpha = 0.9f), RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                                .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                        )
                    }

                    // PHONE SCREEN AREA
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape((screenCornerRadius - bezelThickness / 2).coerceAtLeast(0f).dp))
                        .background(Color(0xFF0D1117))
                ) {
                    // Screenshot AsyncImage
                    if (imageUri != null) {
                        val filterMatrix = remember(screenshotGrayscale, screenshotSepia, screenshotBrightness, screenshotContrast) {
                            androidx.compose.ui.graphics.ColorMatrix(
                                getCombinedColorMatrixArray(
                                    screenshotGrayscale,
                                    screenshotSepia,
                                    screenshotBrightness,
                                    screenshotContrast
                                )
                            )
                        }
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "User Screenshot",
                            contentScale = ContentScale.Crop,
                            colorFilter = if (screenshotGrayscale > 0f || screenshotSepia > 0f || screenshotBrightness != 1f || screenshotContrast != 1f) {
                                androidx.compose.ui.graphics.ColorFilter.colorMatrix(filterMatrix)
                            } else {
                                null
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = screenshotScale
                                    scaleY = screenshotScale
                                    translationX = screenshotOffsetX.dp.toPx()
                                    translationY = screenshotOffsetY.dp.toPx()
                                }
                        )

                        // Liquid Glass Filter / Overlay on the screenshot itself
                        if (screenshotLiquidGlass) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0x3A00F5D4),
                                                Color(0x107B2CBF),
                                                Color(0x409B5DE5)
                                            )
                                        )
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .drawBehind {
                                        val path = android.graphics.Path().apply {
                                            moveTo(0f, 0f)
                                            lineTo(size.width, 0f)
                                            cubicTo(size.width, size.height * 0.4f, 0f, size.height * 0.7f, 0f, size.height)
                                            close()
                                        }
                                        drawPath(
                                            path = androidx.compose.ui.graphics.Path().apply { asAndroidPath().set(path) },
                                            brush = Brush.verticalGradient(
                                                colors = listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.02f))
                                            )
                                        )
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                colors = listOf(Color(0x4000E5FF), Color.Transparent),
                                                center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.2f),
                                                radius = size.width * 0.6f
                                            )
                                        )
                                    }
                            )
                        }
                    } else {
                        // Empty/Onboarding design state placeholder
                        ScreenshotPlaceholderState()
                    }

                    // Premium Liquid Glass Blur Overlay
                    if (showDisplayGlassBlur) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            displayGlassBlurColor.copy(alpha = displayGlassBlurOpacity),
                                            displayGlassBlurColor.copy(alpha = displayGlassBlurOpacity * 0.3f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        val shiftX by rememberInfiniteTransition(label = "LiquidDisplayGlass")
                            .animateFloat(
                                initialValue = -120f,
                                targetValue = 120f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(4000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "glassShift"
                            )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .drawBehind {
                                    val progress = (shiftX + 120f) / 240f
                                    val lineX = size.width * progress
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.18f * displayGlassBlurOpacity),
                                        start = androidx.compose.ui.geometry.Offset(lineX - 100f, 0f),
                                        end = androidx.compose.ui.geometry.Offset(lineX + 100f, size.height),
                                        strokeWidth = 24.dp.toPx()
                                    )
                                }
                        )
                    }

                    // Screen fold crease (for foldables)
                    if (hasFoldLine) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .align(Alignment.Center)
                                .background(Color.Black.copy(alpha = 0.15f))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .align(Alignment.Center)
                                .offset(x = 1.dp)
                                .background(Color.White.copy(alpha = 0.08f))
                        )
                    }

                    // Real Screen Glossy Reflection Overlay
                    if (showGlossyReflection) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        0.0f to Color.White.copy(alpha = glossyReflectionOpacity * 1.5f),
                                        0.22f to Color.White.copy(alpha = glossyReflectionOpacity * 0.7f),
                                        0.35f to Color.Transparent,
                                        0.40f to Color(0xFFE0F7FA).copy(alpha = glossyReflectionOpacity * 0.25f), // subtle cyan-blue sky glare
                                        0.45f to Color.White.copy(alpha = glossyReflectionOpacity * 0.9f), // sharp light reflection
                                        0.52f to Color.Transparent,
                                        1.0f to Color.Transparent
                                    )
                                )
                        )
                    }

                    // 3. HARDWARE ACCENTS / NOTCH DRAWINGS
                    when (activeTemplate) {
                        MockupTemplate.PIXEL_MODERN -> {
                            // Small centered punch hole camera
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 8.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black)
                                    .border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                            )
                        }
                        MockupTemplate.IPHONE_NOTCH -> {
                            // Sleek notch at top
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .width(72.dp)
                                    .height(18.dp)
                                    .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
                                    .background(Color.Black)
                            )
                        }
                        MockupTemplate.DYNAMIC_ISLAND -> {
                            // Premium pill shape
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 10.dp)
                                    .width(64.dp)
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black)
                            )
                        }
                        MockupTemplate.MINIMAL_BORDER -> {
                            // Completely edge to edge minimal screen!
                        }
                    }

                    // Status Bar placeholder overlay
                    if (showStatusBarIcons) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "09:41",
                                fontSize = 8.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.SignalCellular4Bar,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(8.dp)
                                )
                                Icon(
                                    Icons.Default.Wifi,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(8.dp)
                                )
                                Icon(
                                    Icons.Default.BatteryChargingFull,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(8.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (isDesktop) {
                // Stand Stem
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(20.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(bezelColor, bezelColor.copy(alpha = 0.7f))
                            )
                        )
                )
                // Stand Base
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .height(6.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(bezelColor.copy(alpha = 0.9f), bezelColor)
                            ),
                            RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                        )
                )
            }
        }
    }

        // 4. WATERMARK TEXT DRAWING
        if (showWatermark && watermarkText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = watermarkPosition.alignment
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.45f * watermarkOpacity)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                ) {
                    Text(
                        text = watermarkText.uppercase(),
                        color = watermarkColor.copy(alpha = watermarkOpacity),
                        fontWeight = FontWeight.Bold,
                        fontSize = watermarkSize.sp,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ScreenshotPlaceholderState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                )
            )
            .drawBehind {
                // Draw a beautiful futuristic tech grid overlay
                val gridSize = 40.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawLine(
                        color = Color(0x0E00E5FF),
                        start = androidx.compose.ui.geometry.Offset(x, 0f),
                        end = androidx.compose.ui.geometry.Offset(x, size.height),
                        strokeWidth = 1f
                    )
                    x += gridSize
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(
                        color = Color(0x0E00E5FF),
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(size.width, y),
                        strokeWidth = 1f
                    )
                    y += gridSize
                }
            }
    ) {
        // Status Bar Simulation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "09:41",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(11.dp)
                )
                Icon(
                    imageVector = Icons.Default.Power,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        // Home screen content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top widget
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFF355DA), Color(0xFF7000FF))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Neon Dreams",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Sayanth & Rock Studio",
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Center Call-To-Action (Instruction) overlay
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "TAP TO LOAD",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Select screenshot to begin",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Bottom quick dock bar simulation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    Icons.Default.Phone,
                    Icons.Default.Mail,
                    Icons.Default.Home,
                    Icons.Default.Settings
                ).forEach { icon ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREENSHOT CONTROLS CONTENT
// ==========================================

@Composable
fun ScreenshotTabContent(
    selectedImageUri: Uri?,
    screenshotScale: Float,
    screenshotOffsetX: Float,
    screenshotOffsetY: Float,
    autoMatchDeviceFrameRatio: Boolean,
    onAutoMatchDeviceFrameRatioChange: (Boolean) -> Unit,
    detectedScreenshotAspectRatio: Float,
    onScaleChange: (Float) -> Unit,
    onOffsetXChange: (Float) -> Unit,
    onOffsetYChange: (Float) -> Unit,
    screenshotGrayscale: Float,
    onGrayscaleChange: (Float) -> Unit,
    screenshotSepia: Float,
    onSepiaChange: (Float) -> Unit,
    screenshotBrightness: Float,
    onBrightnessChange: (Float) -> Unit,
    screenshotContrast: Float,
    onContrastChange: (Float) -> Unit,
    screenshotLiquidGlass: Boolean,
    onLiquidGlassChange: (Boolean) -> Unit,
    onPickImage: () -> Unit,
    onPickFile: () -> Unit,
    onClearImage: () -> Unit,
    onReset: () -> Unit
) {
    if (selectedImageUri != null) {
        ControlCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Mini preview of the loaded screenshot
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected Screenshot",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Active Screenshot",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Screenshot placed inside the frame successfully.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onPickFile,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(40.dp).testTag("change_file_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Change File", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onPickImage,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(40.dp).testTag("change_gallery_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Use Gallery", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onClearImage,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(40.dp).testTag("clear_screenshot_button")
                ) {
                    Text("Clear", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onReset,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("reset_screenshot_frame_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset Frame and Screenshot",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset Screenshot & Frame", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ControlCard {
            Text(
                text = "Screenshot Style & Filters",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Grayscale
            LabelSlider(
                label = "Grayscale",
                value = screenshotGrayscale,
                valueRange = 0f..1f,
                displayValue = "${(screenshotGrayscale * 100).roundToInt()}%",
                onValueChange = onGrayscaleChange
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Sepia
            LabelSlider(
                label = "Sepia Tone",
                value = screenshotSepia,
                valueRange = 0f..1f,
                displayValue = "${(screenshotSepia * 100).roundToInt()}%",
                onValueChange = onSepiaChange
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Brightness
            LabelSlider(
                label = "Brightness",
                value = screenshotBrightness,
                valueRange = 0.5f..1.5f,
                displayValue = "${(screenshotBrightness * 100).roundToInt()}%",
                onValueChange = onBrightnessChange
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Contrast
            LabelSlider(
                label = "Contrast",
                value = screenshotContrast,
                valueRange = 0.5f..1.5f,
                displayValue = "${(screenshotContrast * 100).roundToInt()}%",
                onValueChange = onContrastChange
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Liquid Glass effect toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (screenshotLiquidGlass) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = if (screenshotLiquidGlass) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (screenshotLiquidGlass) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Filter,
                            contentDescription = null,
                            tint = if (screenshotLiquidGlass) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Liquid Glass Effect",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Applies a premium 3D glass gloss overlay",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                androidx.compose.material3.Switch(
                    checked = screenshotLiquidGlass,
                    onCheckedChange = onLiquidGlassChange,
                    modifier = Modifier.testTag("liquid_glass_switch")
                )
            }
        }
    } else {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("screenshot_upload_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Upload Screenshot File",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Select a PNG or JPG file from your device files or photo gallery to place inside the device mockup frame.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onPickFile,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(44.dp).testTag("upload_file_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Choose File", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onPickImage,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(44.dp).testTag("select_gallery_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Use Gallery", fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Quick Start",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Quick Start Guide",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                OnboardingStepRow(
                    stepNumber = "1",
                    icon = Icons.Default.AddPhotoAlternate,
                    title = "Upload Screenshot",
                    description = "Choose or pick a high-res screenshot from your library to place into the frame."
                )

                OnboardingStepRow(
                    stepNumber = "2",
                    icon = Icons.Default.Devices,
                    title = "Select Frame Style",
                    description = "Tap the menu button at the top-left to select from a variety of premium Phones, Tablets, or Desktops."
                )

                OnboardingStepRow(
                    stepNumber = "3",
                    icon = Icons.Default.ColorLens,
                    title = "Customize Aesthetics",
                    description = "Personalize colors, fluid gradients, 3D rotations, professional drop shadows, watermarks, or filters."
                )

                OnboardingStepRow(
                    stepNumber = "4",
                    icon = Icons.Default.Download,
                    title = "Export Masterpiece",
                    description = "Click the download action icon at the top-right to render and save your high-res mockup directly to your Gallery."
                )
            }
        }
    }

    if (selectedImageUri != null) {
        Spacer(modifier = Modifier.height(16.dp))
        ControlCard(title = "Auto-Match Device Frame") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Match Frame to Screenshot",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val ratioDesc = when {
                        Math.abs(detectedScreenshotAspectRatio - (9f / 16f)) < 0.02f -> "9:16 (Portrait)"
                        Math.abs(detectedScreenshotAspectRatio - (9f / 19.5f)) < 0.02f -> "9:19.5 (Tall Portrait)"
                        Math.abs(detectedScreenshotAspectRatio - 1f) < 0.02f -> "1:1 (Square)"
                        Math.abs(detectedScreenshotAspectRatio - (4f / 3f)) < 0.02f -> "4:3 (Tablet)"
                        Math.abs(detectedScreenshotAspectRatio - (16f / 9f)) < 0.02f -> "16:9 (Landscape)"
                        else -> {
                            val computedY = ((9f / detectedScreenshotAspectRatio) * 10).roundToInt() / 10f
                            "Custom (9 : $computedY)"
                        }
                    }
                    Text(
                        text = "Detected ratio: $ratioDesc",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Switch(
                    checked = autoMatchDeviceFrameRatio,
                    onCheckedChange = onAutoMatchDeviceFrameRatioChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.testTag("auto_match_ratio_switch")
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    ControlCard(title = "Scale & Positioning") {
        LabelSlider(
            label = "Screenshot Scale",
            value = screenshotScale,
            valueRange = 0.5f..1.8f,
            displayValue = "${(screenshotScale * 100).roundToInt()}%",
            onValueChange = onScaleChange
        )

        Spacer(modifier = Modifier.height(12.dp))

        LabelSlider(
            label = "Offset X (Horizontal)",
            value = screenshotOffsetX,
            valueRange = -100f..100f,
            displayValue = "${screenshotOffsetX.roundToInt()}dp",
            onValueChange = onOffsetXChange
        )

        Spacer(modifier = Modifier.height(12.dp))

        LabelSlider(
            label = "Offset Y (Vertical)",
            value = screenshotOffsetY,
            valueRange = -100f..100f,
            displayValue = "${screenshotOffsetY.roundToInt()}dp",
            onValueChange = onOffsetYChange
        )
    }
}

// ==========================================
// BATCH CONTROLS CONTENT
// ==========================================

@Composable
fun BatchTabContent(
    batchQueue: List<Uri>,
    autoMatchDeviceFrameRatio: Boolean,
    onAutoMatchDeviceFrameRatioChange: (Boolean) -> Unit,
    onPickScreenshots: () -> Unit,
    onRemoveScreenshot: (Uri) -> Unit,
    onClearQueue: () -> Unit,
    onProcessBatch: () -> Unit
) {
    ControlCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Batch Process",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Apply your current mockup styles, 3D tilt, shadow, and background styling to multiple screenshots and save them in bulk.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                lineHeight = 15.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onPickScreenshots,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("batch_select_button")
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Screenshots", fontSize = 13.sp)
                }

                if (batchQueue.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onClearQueue,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("batch_clear_button")
                    ) {
                        Text("Clear All")
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    ControlCard(title = "Batch Settings") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Dynamic Ratio Detection",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Detects & matches device frame ratio for each screenshot separately",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Switch(
                checked = autoMatchDeviceFrameRatio,
                onCheckedChange = onAutoMatchDeviceFrameRatioChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                ),
                modifier = Modifier.testTag("batch_auto_match_switch")
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (batchQueue.isEmpty()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Collections,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = "Queue is empty",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Select one or more screenshots above to start batch exporting.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }
    } else {
        ControlCard(title = "Screenshots Queue (${batchQueue.size})") {
            val chunked = batchQueue.chunked(3)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                chunked.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { uri ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(9f / 16f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                IconButton(
                                    onClick = { onRemoveScreenshot(uri) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(24.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), shape = CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                        val remaining = 3 - rowItems.size
                        for (i in 0 until remaining) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onProcessBatch,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("batch_process_button")
        ) {
            Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Process & Export Batch Queue", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 15.sp)
        }
    }
}

// ==========================================
// DEVICE FRAME SIDEBAR COMPONENT
// ==========================================

@Composable
fun DeviceFrameSidebar(
    modifier: Modifier = Modifier,
    selectedStyleId: String,
    onStyleSelect: (DeviceFrameStyle) -> Unit,
    onClose: () -> Unit,
    backgroundType: BackgroundType = BackgroundType.SOLID,
    onBackgroundTypeChange: (BackgroundType) -> Unit = {},
    selectedSolidColor: Color = Color.Black,
    onSolidColorChange: (Color) -> Unit = {},
    customHue: Float = 0f,
    onCustomHueChange: (Float) -> Unit = {},
    customSaturation: Float = 0.7f,
    onCustomSaturationChange: (Float) -> Unit = {},
    customValue: Float = 0.9f,
    onCustomValueChange: (Float) -> Unit = {},
    selectedGradientIndex: Int = 0,
    onGradientIndexChange: (Int) -> Unit = {},
    selectedPatternType: PatternType = PatternType.DOTS,
    onPatternTypeChange: (PatternType) -> Unit = {},
    patternPrimaryColorHue: Float = 240f,
    onPatternPrimaryColorHueChange: (Float) -> Unit = {},
    patternSecondaryColorHue: Float = 280f,
    onPatternSecondaryColorHueChange: (Float) -> Unit = {},
    patternBgColor: Color = Color.Black,
    onPatternBgColorChange: (Color) -> Unit = {},
    patternScale: Float = 1.0f,
    onPatternScaleChange: (Float) -> Unit = {},
    shadowEnabled: Boolean = true,
    onShadowEnabledChange: (Boolean) -> Unit = {},
    shadowStrength: Float = 0.4f,
    onShadowStrengthChange: (Float) -> Unit = {},
    shadowBlurRadius: Float = 25f,
    onShadowBlurRadiusChange: (Float) -> Unit = {},
    shadowOffsetX: Float = 10f,
    onShadowOffsetXChange: (Float) -> Unit = {},
    shadowOffsetY: Float = 15f,
    onShadowOffsetYChange: (Float) -> Unit = {},
    shadowColor: Color = Color.Black,
    onShadowColorChange: (Color) -> Unit = {}
) {
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Phones", "Tablets", "Desktops")
    
    val filteredPresets = remember(selectedCategory) {
        when (selectedCategory) {
            "Phones" -> DeviceFrameStylePresets.filter { !it.isTablet && !it.isDesktop }
            "Tablets" -> DeviceFrameStylePresets.filter { it.isTablet }
            "Desktops" -> DeviceFrameStylePresets.filter { it.isDesktop }
            else -> DeviceFrameStylePresets
        }
    }

    var activeTab by remember { mutableStateOf("Devices") }
    val tabs = listOf("Devices", "Background", "Shadow")

    Surface(
        modifier = modifier
            .width(320.dp)
            .fillMaxHeight()
            .testTag("device_frame_sidebar"),
        color = Color(0xFF0F0F12), // Deep obsidian background
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp, bottom = 16.dp)
        ) {
            // Header with Close Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DEVICE LAB",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF),
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = when (activeTab) {
                            "Devices" -> "Device Frames"
                            "Background" -> "Custom Canvas"
                            else -> "Realistic Shadow"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Sidebar",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // TAB SELECTOR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (tab in tabs) {
                    val isSelected = activeTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFF1E293B) else Color.Transparent)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.White.copy(alpha = 0.08f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { activeTab = tab }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.6f),
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (activeTab == "Devices") {
                // Category Quick Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (category in categories) {
                        val isSelected = selectedCategory == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSelected) Color(0xFF00E5FF)
                                    else Color.White.copy(alpha = 0.05f)
                                )
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = category,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Available presets (${filteredPresets.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp)
                )
                
                // Presets Lazy List
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    filteredPresets.forEach { style ->
                        item {
                            val isSelected = style.id == selectedStyleId
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.12f)
                                        else Color.White.copy(alpha = 0.02f)
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { onStyleSelect(style) }
                                    .padding(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.15f)
                                                else Color.White.copy(alpha = 0.06f)
                                            )
                                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when {
                                                style.isDesktop -> Icons.Default.Laptop
                                                style.isTablet -> Icons.Default.TabletAndroid
                                                else -> Icons.Default.Smartphone
                                            },
                                            contentDescription = null,
                                            tint = if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = style.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color(0xFF00E5FF) else Color.White
                                        )
                                        Text(
                                            text = style.description,
                                            fontSize = 9.5.sp,
                                            color = Color.White.copy(alpha = 0.5f),
                                            lineHeight = 13.sp
                                        )
                                    }
                                    
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF00E5FF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Active",
                                                tint = Color.Black,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SpecBadge(text = style.template.name.replace("_", " "))
                                    SpecBadge(text = "Corner: ${style.screenCornerRadius.roundToInt()}dp")
                                    SpecBadge(text = "Bezel: ${style.bezelThickness.roundToInt()}dp")
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Selecting presets wraps your screenshot instantly in high-fidelity hardware.",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.4f),
                        lineHeight = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else if (activeTab == "Background") {
                // Background Customization Panel!
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 1. Background Type Selection Chips
                    Column {
                        Text(
                            text = "CANVAS TYPE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF),
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.03f))
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            val canvasOptions = listOf(
                                BackgroundType.SOLID to "Solid",
                                BackgroundType.GRADIENT to "Grad",
                                BackgroundType.PATTERN to "Pattern",
                                BackgroundType.AMBIENT_BLUR to "Blur",
                                BackgroundType.LIQUID_FLOW to "Liquid"
                            )
                            for (option in canvasOptions) {
                                val type = option.first
                                val label = option.second
                                val isSelected = backgroundType == type
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFF334155) else Color.Transparent)
                                        .clickable { onBackgroundTypeChange(type) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }

                    Divider(color = Color.White.copy(alpha = 0.06f))

                    // 2. Active Mode Customizers
                    when (backgroundType) {
                        BackgroundType.SOLID -> {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(
                                    text = "SOLID COLOR PRESETS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.5f),
                                    letterSpacing = 1.sp
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    for (color in PresetColors) {
                                        val isCurrent = selectedSolidColor == color
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .border(
                                                    width = if (isCurrent) 2.5.dp else 1.dp,
                                                    color = if (isCurrent) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.15f),
                                                    shape = CircleShape
                                                )
                                                .clickable { onSolidColorChange(color) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "INFINITE PALETTE SLIDERS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.5f),
                                    letterSpacing = 1.sp
                                )

                                // Custom Hue Slider
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Hue Accent", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                                        Text("${customHue.roundToInt()}°", fontSize = 11.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = customHue,
                                        onValueChange = onCustomHueChange,
                                        valueRange = 0f..360f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.hsv(customHue, 1f, 1f),
                                            activeTrackColor = Color.hsv(customHue, 0.7f, 0.8f),
                                            inactiveTrackColor = Color.White.copy(alpha = 0.08f)
                                        )
                                    )
                                }

                                // Custom Saturation Slider
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Saturation", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                                        Text("${(customSaturation * 100).roundToInt()}%", fontSize = 11.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = customSaturation,
                                        onValueChange = onCustomSaturationChange,
                                        valueRange = 0f..1f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = Color.White.copy(alpha = 0.4f),
                                            inactiveTrackColor = Color.White.copy(alpha = 0.08f)
                                        )
                                    )
                                }

                                // Custom Value (Brightness) Slider
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Brightness", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                                        Text("${(customValue * 100).roundToInt()}%", fontSize = 11.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = customValue,
                                        onValueChange = onCustomValueChange,
                                        valueRange = 0f..1f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = Color.White.copy(alpha = 0.4f),
                                            inactiveTrackColor = Color.White.copy(alpha = 0.08f)
                                        )
                                    )
                                }
                            }
                        }
                        BackgroundType.GRADIENT -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "PREMIUM GRADIENTS (${PremiumGradients.size})",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.5f),
                                    letterSpacing = 1.sp
                                )
                                for (idx in PremiumGradients.indices) {
                                    val grad = PremiumGradients[idx]
                                    val isCurrent = selectedGradientIndex == idx
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isCurrent) Color(0xFF1E293B) else Color.White.copy(alpha = 0.02f)
                                            )
                                            .border(
                                                width = if (isCurrent) 1.5.dp else 1.dp,
                                                color = if (isCurrent) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.05f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { onGradientIndexChange(idx) }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 50.dp, height = 28.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Brush.linearGradient(grad.colors))
                                        )
                                        Text(
                                            text = grad.name,
                                            fontSize = 12.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCurrent) Color(0xFF00E5FF) else Color.White
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        if (isCurrent) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Selected",
                                                tint = Color(0xFF00E5FF),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        BackgroundType.PATTERN -> {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(
                                    text = "PATTERN TYPE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.5f),
                                    letterSpacing = 1.sp
                                )
                                val patternOptions = listOf(
                                    PatternType.DOTS to "Dot Matrix",
                                    PatternType.GRID to "Sleek Grid",
                                    PatternType.STRIPES to "Carbon Fiber",
                                    PatternType.WAVES to "Topo Wave",
                                    PatternType.HEXAGON to "Honeycomb",
                                    PatternType.STARS to "Stellar Space"
                                )
                                
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    for (pair in patternOptions.chunked(2)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            for (option in pair) {
                                                val type = option.first
                                                val label = option.second
                                                val isCurrent = selectedPatternType == type
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(
                                                            if (isCurrent) Color(0xFF1E293B) else Color.White.copy(alpha = 0.02f)
                                                        )
                                                        .border(
                                                            width = if (isCurrent) 1.5.dp else 1.dp,
                                                            color = if (isCurrent) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.05f),
                                                            shape = RoundedCornerShape(10.dp)
                                                        )
                                                        .clickable { onPatternTypeChange(type) }
                                                        .padding(vertical = 10.dp, horizontal = 12.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = label,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isCurrent) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.8f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Divider(color = Color.White.copy(alpha = 0.05f))

                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Pattern Scale", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                                        Text("${(patternScale * 100).roundToInt()}%", fontSize = 11.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = patternScale,
                                        onValueChange = onPatternScaleChange,
                                        valueRange = 0.4f..2.5f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = Color(0xFF00E5FF),
                                            inactiveTrackColor = Color.White.copy(alpha = 0.08f)
                                        )
                                    )
                                }

                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Primary Color Hue", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                                        Text("${patternPrimaryColorHue.roundToInt()}°", fontSize = 11.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = patternPrimaryColorHue,
                                        onValueChange = onPatternPrimaryColorHueChange,
                                        valueRange = 0f..360f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.hsv(patternPrimaryColorHue, 0.7f, 0.9f),
                                            activeTrackColor = Color.hsv(patternPrimaryColorHue, 0.7f, 0.9f),
                                            inactiveTrackColor = Color.White.copy(alpha = 0.08f)
                                        )
                                    )
                                }

                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Secondary Color Hue", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                                        Text("${patternSecondaryColorHue.roundToInt()}°", fontSize = 11.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = patternSecondaryColorHue,
                                        onValueChange = onPatternSecondaryColorHueChange,
                                        valueRange = 0f..360f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.hsv(patternSecondaryColorHue, 0.7f, 0.9f),
                                            activeTrackColor = Color.hsv(patternSecondaryColorHue, 0.7f, 0.9f),
                                            inactiveTrackColor = Color.White.copy(alpha = 0.08f)
                                        )
                                    )
                                }

                                Column {
                                    Text(
                                        text = "PATTERN BASE BACKGROUND",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.5f),
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        for (color in PresetColors) {
                                            val isCurrent = patternBgColor == color
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                                    .border(
                                                        width = if (isCurrent) 2.5.dp else 1.dp,
                                                        color = if (isCurrent) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.15f),
                                                        shape = CircleShape
                                                    )
                                                    .clickable { onPatternBgColorChange(color) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        BackgroundType.AMBIENT_BLUR -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "AMBIENT BLUR",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.5f),
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Ambient Blur uses dynamic blurred color hues extracted from your screenshot.",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.6f),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                        BackgroundType.LIQUID_FLOW -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "LIQUID FLOW DESIGN",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.5f),
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Liquid flow displays fully customized, beautiful procedural glass and liquid themes.",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.6f),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                        else -> {}
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Every background is fully rendered at ultra high resolution upon export.",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.4f),
                        lineHeight = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                // Shadow Customization Panel!
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 1. Enable Toggle Card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable Drop Shadow",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Apply realistic depth shadows",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                        Switch(
                            checked = shadowEnabled,
                            onCheckedChange = onShadowEnabledChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF00E5FF),
                                checkedTrackColor = Color(0xFF00E5FF).copy(alpha = 0.3f)
                            )
                        )
                    }

                    if (shadowEnabled) {
                        // 2. Shadow Intensity (Opacity) Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Shadow Intensity", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                                Text("${(shadowStrength * 100).roundToInt()}%", fontSize = 11.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = shadowStrength,
                                onValueChange = onShadowStrengthChange,
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color(0xFF00E5FF),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.08f)
                                )
                            )
                        }

                        // 3. Shadow Softness / Blur Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Shadow Softness / Blur", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                                Text("${shadowBlurRadius.roundToInt()}dp", fontSize = 11.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = shadowBlurRadius,
                                onValueChange = onShadowBlurRadiusChange,
                                valueRange = 0f..80f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color(0xFF00E5FF),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.08f)
                                )
                            )
                        }

                        // 4. Shadow Hue (Color Spectrum Slider)
                        val hsv = FloatArray(3)
                        android.graphics.Color.colorToHSV(
                            android.graphics.Color.argb(
                                (shadowColor.alpha * 255f).roundToInt(),
                                (shadowColor.red * 255f).roundToInt(),
                                (shadowColor.green * 255f).roundToInt(),
                                (shadowColor.blue * 255f).roundToInt()
                            ),
                            hsv
                        )
                        val shadowHue = hsv[0]

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Shadow Color Hue", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                                val isBlack = shadowColor == Color.Black || (shadowColor.red == 0f && shadowColor.green == 0f && shadowColor.blue == 0f)
                                Text(
                                    text = if (isBlack) "Monochrome" else "${shadowHue.roundToInt()}°",
                                    fontSize = 11.sp,
                                    color = Color(0xFF00E5FF),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = shadowHue,
                                onValueChange = { hue ->
                                    onShadowColorChange(Color.hsv(hue, 0.9f, 0.9f))
                                },
                                valueRange = 0f..360f,
                                colors = SliderDefaults.colors(
                                    thumbColor = if (shadowColor == Color.Black) Color.White else Color.hsv(shadowHue, 1f, 1f),
                                    activeTrackColor = if (shadowColor == Color.Black) Color.White.copy(alpha = 0.4f) else Color.hsv(shadowHue, 0.7f, 0.8f),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.08f)
                                )
                            )
                        }

                        // 5. Light Position X (Offset)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Offset X (Horizontal)", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                                Text("${shadowOffsetX.roundToInt()}dp", fontSize = 11.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = shadowOffsetX,
                                onValueChange = onShadowOffsetXChange,
                                valueRange = -50f..50f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color(0xFF00E5FF),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.08f)
                                )
                            )
                        }

                        // 6. Light Position Y (Offset)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Offset Y (Vertical)", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                                Text("${shadowOffsetY.roundToInt()}dp", fontSize = 11.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = shadowOffsetY,
                                onValueChange = onShadowOffsetYChange,
                                valueRange = -50f..50f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color(0xFF00E5FF),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.08f)
                                )
                            )
                        }

                        // 7. Preset Color Chips
                        Column {
                            Text(
                                text = "QUICK SHADOW PRESETS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.5f),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val shadowPresetColors = listOf(
                                Pair("Classic Black", Color.Black),
                                Pair("Slate Grey", Color(0xFF475569)),
                                Pair("Neon Cyan", Color(0xFF00E5FF)),
                                Pair("Cosmic Violet", Color(0xFF7000FF)),
                                Pair("Neon Pink", Color(0xFFF355DA)),
                                Pair("Amber Glow", Color(0xFFFF9100))
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                shadowPresetColors.forEach { (name, color) ->
                                    val isSelected = shadowColor == color
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                color = if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.15f),
                                                shape = CircleShape
                                            )
                                            .clickable { onShadowColorChange(color) }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Customize realistic drop shadow values to enhance visual layout depth.",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.4f),
                        lineHeight = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun SpecBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.5f),
            letterSpacing = 0.2.sp
        )
    }
}

// ==========================================
// BACKGROUND CONTROLS CONTENT
// ==========================================

@Composable
fun VisualGradientSlider(
    modifier: Modifier = Modifier,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    gradientBrush: Brush,
    title: String,
    displayValue: String
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = displayValue,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(gradientBrush)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                        val mapped = valueRange.start + ratio * (valueRange.endInclusive - valueRange.start)
                        onValueChange(mapped)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val ratio = (change.position.x / size.width).coerceIn(0f, 1f)
                        val mapped = valueRange.start + ratio * (valueRange.endInclusive - valueRange.start)
                        onValueChange(mapped)
                    }
                }
        ) {
            val ratio = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
            val thumbX = maxWidth * ratio
            Box(
                modifier = Modifier
                    .absoluteOffset(x = thumbX - 14.dp, y = 0.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .shadow(elevation = 6.dp, shape = CircleShape)
                    .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}

@Composable
fun InteractiveColorPicker(
    selectedColor: Color,
    onColorSelect: (Color) -> Unit,
    hue: Float,
    saturation: Float,
    value: Float,
    onHueChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onValueChange: (Float) -> Unit
) {
    var hexInputText by remember { mutableStateOf("") }
    
    // Sync hex input from selectedColor changes
    LaunchedEffect(selectedColor) {
        val hex = String.format("#%06X", (selectedColor.toArgb() and 0xFFFFFF))
        if (hexInputText.uppercase() != hex) {
            hexInputText = hex
        }
    }

    val obsidianColors = remember {
        listOf(
            Color(0xFF0F172A), // Deep Slate
            Color(0xFF1E293B), // Medium Slate
            Color(0xFF121212), // Dark Charcoal
            Color(0xFF0D1E2D), // Deep Cyan Blue
            Color(0xFF1A0B2E), // Deep Purple
            Color(0xFF022C22), // Dark Emerald
            Color(0xFF450A0A)  // Dark Maroon
        )
    }

    val vibrantColors = remember {
        listOf(
            Color(0xFF4F46E5), // Indigo
            Color(0xFF06B6D4), // Cyan
            Color(0xFF10B981), // Emerald
            Color(0xFFF43F5E), // Rose
            Color(0xFF8B5CF6), // Violet
            Color(0xFFF59E0B), // Amber
            Color(0xFFEC4899)  // Pink
        )
    }

    val pastelColors = remember {
        listOf(
            Color(0xFFEEF2F6), // Soft Slate
            Color(0xFFE0E7FF), // Lavender Mist
            Color(0xFFE0F2FE), // Ice Sky
            Color(0xFFD1FAE5), // Mint Green
            Color(0xFFFCE7F3), // Pale Rose
            Color(0xFFFEF3C7), // Warm Sand
            Color(0xFFF3E8FF)  // Pale Lilac
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Live Color Preview & Hex Details
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            // Preview Swatch
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(selectedColor)
                    .border(2.dp, Color.White, RoundedCornerShape(16.dp))
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(16.dp))
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Current Color",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Real-time Hex Color Field
                OutlinedTextField(
                    value = hexInputText,
                    onValueChange = { input ->
                        hexInputText = input
                        val cleaned = input.trim().removePrefix("#")
                        if (cleaned.length == 6) {
                            try {
                                val parsedInt = cleaned.toLong(16)
                                val r = ((parsedInt shr 16) and 0xFF).toInt()
                                val g = ((parsedInt shr 8) and 0xFF).toInt()
                                val b = (parsedInt and 0xFF).toInt()
                                val parsedColor = Color(r, g, b, 255)
                                onColorSelect(parsedColor)
                            } catch (e: Exception) {
                                // Ignore invalid input format
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("custom_hex_input_picker"),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    singleLine = true,
                    placeholder = { Text("#FFFFFF") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }
        }

        // Hue Slider
        val hueBrush = remember {
            Brush.horizontalGradient(
                colors = listOf(
                    Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                )
            )
        }
        VisualGradientSlider(
            value = hue,
            onValueChange = onHueChange,
            valueRange = 0f..360f,
            gradientBrush = hueBrush,
            title = "Hue Spectrum",
            displayValue = "${hue.roundToInt()}°"
        )

        // Saturation Slider
        val saturationBrush = remember(hue, value) {
            Brush.horizontalGradient(
                colors = listOf(
                    Color.hsv(hue, 0f, value),
                    Color.hsv(hue, 1f, value)
                )
            )
        }
        VisualGradientSlider(
            value = saturation,
            onValueChange = onSaturationChange,
            valueRange = 0f..1f,
            gradientBrush = saturationBrush,
            title = "Saturation Intensity",
            displayValue = "${(saturation * 100).roundToInt()}%"
        )

        // Value (Brightness) Slider
        val valueBrush = remember(hue, saturation) {
            Brush.horizontalGradient(
                colors = listOf(
                    Color.Black,
                    Color.hsv(hue, saturation, 1f)
                )
            )
        }
        VisualGradientSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0.05f..1f,
            gradientBrush = valueBrush,
            title = "Brightness Value",
            displayValue = "${(value * 100).roundToInt()}%"
        )

        Spacer(modifier = Modifier.height(4.dp))
        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(4.dp))

        // Curated Palette Color Swatches Categories
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Premium Swatch Palettes",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Category 1: Deep / Obsidian Tones
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Obsidian Dark Tones",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    obsidianColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selectedColor == color) 2.5.dp else 1.dp,
                                    color = if (selectedColor == color) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f),
                                    shape = CircleShape
                                )
                                .clickable { onColorSelect(color) }
                        )
                    }
                }
            }

            // Category 2: Cyber / Vibrant Tones
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Cyber Vibrant Tones",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    vibrantColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selectedColor == color) 2.5.dp else 1.dp,
                                    color = if (selectedColor == color) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f),
                                    shape = CircleShape
                                )
                                .clickable { onColorSelect(color) }
                        )
                    }
                }
            }

            // Category 3: Pastel Light Tones
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Soft Pastel Tones",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pastelColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selectedColor == color) 2.5.dp else 1.dp,
                                    color = if (selectedColor == color) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.15f),
                                    shape = CircleShape
                                )
                                .clickable { onColorSelect(color) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BackgroundTabContent(
    backgroundType: BackgroundType,
    selectedSolidColor: Color,
    selectedGradientIndex: Int,
    ambientBlurRadius: Float,
    backgroundBlurRadius: Float,
    customHue: Float,
    customSaturation: Float,
    customValue: Float,
    hasScreenshot: Boolean,
    selectedBackgroundUri: Uri? = null,
    onPickBackground: () -> Unit = {},
    onClearBackground: () -> Unit = {},
    onBackgroundTypeChange: (BackgroundType) -> Unit,
    onSolidColorSelect: (Color) -> Unit,
    onGradientSelect: (Int) -> Unit,
    onBlurRadiusChange: (Float) -> Unit,
    onBackgroundBlurRadiusChange: (Float) -> Unit,
    onHueChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onValueChange: (Float) -> Unit,
    liquidThemeIndex: Int = 0,
    liquidScale: Float = 1.2f,
    showDisplayGlassBlur: Boolean = false,
    displayGlassBlurColor: Color = Color(0xFF00E5FF),
    displayGlassBlurOpacity: Float = 0.4f,
    liquidNoiseEnabled: Boolean = true,
    onLiquidThemeChange: (Int) -> Unit = {},
    onLiquidScaleChange: (Float) -> Unit = {},
    onShowDisplayGlassBlurChange: (Boolean) -> Unit = {},
    onDisplayGlassBlurColorChange: (Color) -> Unit = {},
    onDisplayGlassBlurOpacityChange: (Float) -> Unit = {},
    onLiquidNoiseChange: (Boolean) -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var hexInputText by remember { mutableStateOf("") }
    
    LaunchedEffect(selectedSolidColor) {
        val hex = String.format("#%06X", (selectedSolidColor.toArgb() and 0xFFFFFF))
        if (hexInputText.uppercase() != hex) {
            hexInputText = hex
        }
    }
    
    val quickPresets = listOf(
        // Gradients
        Triple("Midnight Glow Gradient", BackgroundType.GRADIENT, 0),
        Triple("Sunset Breeze Gradient", BackgroundType.GRADIENT, 1),
        Triple("Ocean Surge Gradient", BackgroundType.GRADIENT, 2),
        Triple("Neon Matrix Gradient", BackgroundType.GRADIENT, 3),
        Triple("Cyberpunk Sky Gradient", BackgroundType.GRADIENT, 4),
        Triple("Cosmic Abyss Gradient", BackgroundType.GRADIENT, 5),
        Triple("Emerald Jade Gradient", BackgroundType.GRADIENT, 6),
        // Solids
        Triple("Deep Slate Solid", BackgroundType.SOLID, 0),
        Triple("Medium Slate Solid", BackgroundType.SOLID, 1),
        Triple("Dark Charcoal Solid", BackgroundType.SOLID, 2),
        Triple("Deep Cyan Solid", BackgroundType.SOLID, 3),
        Triple("Dark Violet Solid", BackgroundType.SOLID, 4),
        Triple("Dark Emerald Solid", BackgroundType.SOLID, 5),
        Triple("Dark Maroon Solid", BackgroundType.SOLID, 6)
    )

    val activePresetIndex = quickPresets.indexOfFirst { preset ->
        preset.second == backgroundType && (
            (backgroundType == BackgroundType.GRADIENT && selectedGradientIndex == preset.third) ||
            (backgroundType == BackgroundType.SOLID && PresetColors.getOrNull(preset.third) == selectedSolidColor)
        )
    }
    
    val currentPresetName = if (activePresetIndex != -1) {
        quickPresets[activePresetIndex].first
    } else {
        when (backgroundType) {
            BackgroundType.AMBIENT_BLUR -> "Ambient Blur Backdrop"
            BackgroundType.LIQUID_FLOW -> "Liquid Blur Backdrop"
            else -> "Custom / Fine-Tuned Background"
        }
    }

    ControlCard(title = "Custom Backdrop Image") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onPickBackground,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).testTag("select_background_button_header"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (backgroundType == BackgroundType.IMAGE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pick Background Image")
            }

            if (selectedBackgroundUri != null) {
                OutlinedButton(
                    onClick = onClearBackground,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear")
                }
            }
        }

        if (selectedBackgroundUri != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    .clickable { onBackgroundTypeChange(BackgroundType.IMAGE) }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AsyncImage(
                    model = selectedBackgroundUri,
                    contentDescription = "Selected Background",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Custom Image Loaded",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Tap here to use this image as background",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (backgroundType == BackgroundType.IMAGE) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    ControlCard(title = "Preset Background Gallery") {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .clickable { menuExpanded = true }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("background_preset_selector"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .then(
                                when (backgroundType) {
                                    BackgroundType.SOLID -> Modifier.background(selectedSolidColor)
                                    BackgroundType.GRADIENT -> Modifier.background(Brush.linearGradient(PremiumGradients[selectedGradientIndex].colors))
                                    BackgroundType.PATTERN -> Modifier.background(PatternConfig.patternBgColor)
                                    BackgroundType.AMBIENT_BLUR -> Modifier.background(Color.Gray)
                                    BackgroundType.LIQUID_FLOW -> Modifier.background(Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFFF355DA))))
                                    BackgroundType.IMAGE -> Modifier.background(Color.DarkGray)
                                }
                            )
                    )
                    Text(
                        text = currentPresetName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Expand presets menu",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                quickPresets.forEach { preset ->
                    val isSelected = preset.second == backgroundType && (
                        (backgroundType == BackgroundType.GRADIENT && selectedGradientIndex == preset.third) ||
                        (backgroundType == BackgroundType.SOLID && PresetColors.getOrNull(preset.third) == selectedSolidColor)
                    )
                    
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .then(
                                            if (preset.second == BackgroundType.SOLID) {
                                                Modifier.background(PresetColors[preset.third])
                                            } else {
                                                Modifier.background(Brush.linearGradient(PremiumGradients[preset.third].colors))
                                            }
                                        )
                                )
                                Text(
                                    text = preset.first,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        },
                        trailingIcon = {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onBackgroundTypeChange(preset.second)
                            if (preset.second == BackgroundType.SOLID) {
                                onSolidColorSelect(PresetColors[preset.third])
                            } else {
                                onGradientSelect(preset.third)
                            }
                        }
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    ControlCard(title = "Background Type") {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BackgroundType.values().forEach { type ->
                val active = backgroundType == type
                Button(
                    onClick = {
                        if (type == BackgroundType.AMBIENT_BLUR && !hasScreenshot) {
                            // Fallback if no screenshot
                        } else {
                            onBackgroundTypeChange(type)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    enabled = type != BackgroundType.AMBIENT_BLUR || hasScreenshot,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(type.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    when (backgroundType) {
        BackgroundType.SOLID -> {
            ControlCard(title = "Custom Solid Color") {
                InteractiveColorPicker(
                    selectedColor = selectedSolidColor,
                    onColorSelect = onSolidColorSelect,
                    hue = customHue,
                    saturation = customSaturation,
                    value = customValue,
                    onHueChange = onHueChange,
                    onSaturationChange = onSaturationChange,
                    onValueChange = onValueChange
                )
            }
        }
        BackgroundType.GRADIENT -> {
            ControlCard(title = "Premium Vector Gradients") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Choose a professionally curated gradient theme for your scene mockup background.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    // 2-Column Responsive Grid of Gradient Presets
                    val chunked = PremiumGradients.chunked(2)
                    chunked.forEach { rowGradients ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowGradients.forEach { gradient ->
                                val index = PremiumGradients.indexOf(gradient)
                                val active = selectedGradientIndex == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(72.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Brush.linearGradient(gradient.colors))
                                        .border(
                                            width = if (active) 3.dp else 1.dp,
                                            color = if (active) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .shadow(
                                            elevation = if (active) 8.dp else 2.dp,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable { onGradientSelect(index) }
                                        .padding(12.dp),
                                    contentAlignment = Alignment.BottomStart
                                ) {
                                    // Soft overlay to ensure text is always readable
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                                                )
                                            )
                                    )
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = gradient.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            modifier = Modifier.shadow(1.dp)
                                        )
                                        if (active) {
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // If the row has only 1 item (odd count total), add a blank spacer to maintain alignment
                            if (rowGradients.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        BackgroundType.AMBIENT_BLUR -> {
            ControlCard(title = "Ambient Blur Calibration") {
                LabelSlider(
                    label = "Blur Strength",
                    value = ambientBlurRadius,
                    valueRange = 5f..25f,
                    displayValue = "${ambientBlurRadius.roundToInt()} px",
                    onValueChange = onBlurRadiusChange
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Ambient Blur replicates your imported screenshot, stretches it to fill the background layout, and applies a highly sophisticated blur contour.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
        BackgroundType.LIQUID_FLOW -> {
            ControlCard(title = "Liquid Fluid Aesthetics") {
                Text("Liquid Aura Theme", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
                val themes = listOf(
                    "Neon Aura" to listOf(Color(0xFF00E5FF), Color(0xFFF355DA)),
                    "Sunset" to listOf(Color(0xFFFF5E62), Color(0xFFFF9966)),
                    "Cyber Ocean" to listOf(Color(0xFF00F0FF), Color(0xFF0072FF)),
                    "Orchid" to listOf(Color(0xFF8A2387), Color(0xFFE94057)),
                    "Emerald" to listOf(Color(0xFF11998E), Color(0xFF38EF7D))
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    themes.forEachIndexed { idx, pair ->
                        val active = liquidThemeIndex == idx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.horizontalGradient(pair.second))
                                .border(
                                    if (active) 2.dp else 0.dp,
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onLiquidThemeChange(idx) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                pair.first,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.shadow(1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LabelSlider(
                    label = "Fluid Zoom Scale",
                    value = liquidScale,
                    valueRange = 0.5f..2.0f,
                    displayValue = "${(liquidScale * 100).roundToInt()}%",
                    onValueChange = onLiquidScaleChange
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Noise Granularity", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = liquidNoiseEnabled,
                        onCheckedChange = onLiquidNoiseChange
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ControlCard(title = "Premium Display Glass Blur") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Active Glass Blur Over Screen", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Applies a shimmering, color-matched physical glass light reflection over the device's screen area.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            lineHeight = 13.sp
                        )
                    }
                    Switch(
                        checked = showDisplayGlassBlur,
                        onCheckedChange = onShowDisplayGlassBlurChange
                    )
                }

                if (showDisplayGlassBlur) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Glass Tint Palette", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    val glassColors = listOf(
                        Color(0xFF00E5FF), // Aqua
                        Color(0xFFE94057), // Rose Pink
                        Color(0xFF7000FF), // Violet
                        Color(0xFFFF9966), // Gold Orange
                        Color(0xFF38EF7D), // Mint Green
                        Color(0xFFFFFFFF)  // Ice White
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        glassColors.forEach { color ->
                            val active = displayGlassBlurColor == color
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        if (active) 2.5.dp else 0.dp,
                                        MaterialTheme.colorScheme.primary,
                                        CircleShape
                                    )
                                    .clickable { onDisplayGlassBlurColorChange(color) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LabelSlider(
                        label = "Glass Matte Opacity",
                        value = displayGlassBlurOpacity,
                        valueRange = 0.1f..0.8f,
                        displayValue = "${(displayGlassBlurOpacity * 100).roundToInt()}%",
                        onValueChange = onDisplayGlassBlurOpacityChange
                    )
                }
            }
        }
        BackgroundType.PATTERN -> {
            ControlCard(title = "Pattern Settings") {
                Text(
                    text = "Custom patterns can be selected and fully configured inside the dedicated Device Lab sidebar! Tap the sidebar trigger on the main header, switch to 'Background' tab, and choose your pattern, colors, and scale.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
        BackgroundType.IMAGE -> {
            ControlCard(title = "Backdrop Image Settings") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Your custom backdrop image is active and will be used as the scene background. You can change or clear it in the 'Custom Backdrop Image' card at the top of this tab.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    LabelSlider(
                        label = "Image Blur Radius",
                        value = backgroundBlurRadius,
                        valueRange = 0f..25f,
                        displayValue = if (backgroundBlurRadius == 0f) "Disabled" else "${backgroundBlurRadius.roundToInt()} px",
                        onValueChange = onBackgroundBlurRadiusChange
                    )
                    
                    Text(
                        text = "Apply real-time Gaussian blur to soften and focus your custom background backdrop.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

// ==========================================
// TEMPLATE CONTROLS CONTENT
// ==========================================

@Composable
fun TemplateTabContent(
    selectedDeviceFrameStyleId: String,
    onDeviceFrameStyleChange: (String) -> Unit,
    activeTemplate: MockupTemplate,
    bezelColor: Color,
    bezelThickness: Float,
    screenCornerRadius: Float,
    showStatusBarIcons: Boolean,
    showGlossyReflection: Boolean,
    glossyReflectionOpacity: Float,
    deviceFrameScale: Float,
    onDeviceFrameScaleChange: (Float) -> Unit,
    onTemplateChange: (MockupTemplate) -> Unit,
    onBezelColorChange: (Color) -> Unit,
    onThicknessChange: (Float) -> Unit,
    onCornerRadiusChange: (Float) -> Unit,
    onShowStatusBarChange: (Boolean) -> Unit,
    onShowGlossyReflectionChange: (Boolean) -> Unit,
    onGlossyReflectionOpacityChange: (Float) -> Unit,
    onOpenSidebar: () -> Unit,
    deviceFrameWidth: Float,
    deviceFrameHeight: Float,
    buttonStyle: String,
    hasFoldLine: Boolean,
    onDeviceFrameWidthChange: (Float) -> Unit,
    onDeviceFrameHeightChange: (Float) -> Unit,
    onButtonStyleChange: (String) -> Unit,
    onHasFoldLineChange: (Boolean) -> Unit
) {
    var styleMenuExpanded by remember { mutableStateOf(false) }
    val currentStyle = DeviceFrameStylePresets.find { it.id == selectedDeviceFrameStyleId } ?: DeviceFrameStylePresets[0]

    ControlCard(title = "Material 3 Device Style") {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .clickable { styleMenuExpanded = true }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (currentStyle.isTablet) Icons.Default.TabletAndroid else Icons.Default.Smartphone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = currentStyle.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = currentStyle.description,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            lineHeight = 14.sp
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Expand device styles menu",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            DropdownMenu(
                expanded = styleMenuExpanded,
                onDismissRequest = { styleMenuExpanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                DeviceFrameStylePresets.forEach { style ->
                    val isSelected = style.id == selectedDeviceFrameStyleId
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (style.isTablet) Icons.Default.TabletAndroid else Icons.Default.Smartphone,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = style.name,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = style.description,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        lineHeight = 14.sp
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        onClick = {
                            onDeviceFrameStyleChange(style.id)
                            styleMenuExpanded = false
                        }
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    ControlCard(title = "Hardware Model Presets") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .clickable { onOpenSidebar() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00E5FF).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Open Sidebar Presets",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White
                )
                Text(
                    text = "Select high-fidelity Google Pixel, Apple iPhone, or Samsung Galaxy templates.",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    lineHeight = 14.sp
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(18.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    ControlCard(title = "Device Frame Size (Scale)", icon = Icons.Default.AspectRatio) {
        LabelSlider(
            label = "Device Frame Scale",
            value = deviceFrameScale,
            valueRange = 0.3f..0.95f,
            displayValue = "${(deviceFrameScale * 100).roundToInt()}%",
            onValueChange = onDeviceFrameScaleChange,
            modifier = Modifier.testTag("device_frame_scale_slider")
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    ControlCard(title = "Device Template Frame") {
        MockupTemplate.values().forEach { template ->
            val active = activeTemplate == template
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .clickable { onTemplateChange(template) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = when (template) {
                            MockupTemplate.PIXEL_MODERN -> Icons.Default.Circle
                            MockupTemplate.IPHONE_NOTCH -> Icons.Default.Inbox
                            MockupTemplate.DYNAMIC_ISLAND -> Icons.Default.HorizontalRule
                            MockupTemplate.MINIMAL_BORDER -> Icons.Default.CropFree
                        },
                        contentDescription = null,
                        tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        template.label,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                }
                if (active) {
                    Icon(Icons.Default.RadioButtonChecked, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
                } else {
                    Icon(Icons.Default.RadioButtonUnchecked, contentDescription = "Inactive", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    ControlCard(title = "Bezel Frame Geometry") {
        LabelSlider(
            label = "Bezel Outer Thickness",
            value = bezelThickness,
            valueRange = 4f..24f,
            displayValue = "${bezelThickness.roundToInt()}dp",
            onValueChange = onThicknessChange
        )

        Spacer(modifier = Modifier.height(12.dp))

        LabelSlider(
            label = "Screen Corner Rounding",
            value = screenCornerRadius,
            valueRange = 0f..40f,
            displayValue = "${screenCornerRadius.roundToInt()}dp",
            onValueChange = onCornerRadiusChange
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    ControlCard(title = "Custom Device Dimensions", icon = Icons.Default.AspectRatio) {
        LabelSlider(
            label = "Device Frame Width",
            value = deviceFrameWidth,
            valueRange = 150f..800f,
            displayValue = "${deviceFrameWidth.roundToInt()}dp",
            onValueChange = onDeviceFrameWidthChange
        )

        Spacer(modifier = Modifier.height(12.dp))

        LabelSlider(
            label = "Device Frame Height",
            value = deviceFrameHeight,
            valueRange = 300f..1000f,
            displayValue = "${deviceFrameHeight.roundToInt()}dp",
            onValueChange = onDeviceFrameHeightChange
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    ControlCard(title = "Physical Hardware Features", icon = Icons.Default.Settings) {
        Text("Side Accent Buttons", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("android" to "Android", "iphone" to "iPhone", "none" to "None").forEach { (styleKey, styleLabel) ->
                val active = buttonStyle == styleKey
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .border(1.dp, if (active) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp))
                        .clickable { onButtonStyleChange(styleKey) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = styleLabel,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Center Screen Fold Crease", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("Draws a vertical folding crease line in the center (ideal for open foldable tablet profiles).", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            Switch(
                checked = hasFoldLine,
                onCheckedChange = onHasFoldLineChange
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    ControlCard(title = "Glossy Screen Reflection") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Enable Screen Gloss & Reflection", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("Adds a realistic angled physical light reflection sheen on top of the screenshot.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            Switch(
                checked = showGlossyReflection,
                onCheckedChange = onShowGlossyReflectionChange,
                modifier = Modifier.testTag("glossy_reflection_toggle")
            )
        }

        if (showGlossyReflection) {
            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            LabelSlider(
                label = "Gloss Intensity",
                value = glossyReflectionOpacity,
                valueRange = 0.05f..0.60f,
                displayValue = "${(glossyReflectionOpacity * 100).roundToInt()}%",
                onValueChange = onGlossyReflectionOpacityChange
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    ControlCard(title = "Frame Details") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Show Status Bar Indicators", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("Time, Signal, Wifi, and Battery details", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            Switch(
                checked = showStatusBarIcons,
                onCheckedChange = onShowStatusBarChange
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Divider()
        Spacer(modifier = Modifier.height(12.dp))

        Text("Bezel Color Tone", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val bezelColors = listOf(
                Color(0xFF1E293B), // Classic Slate
                Color(0xFF0F172A), // Dark Midnight
                Color(0xFFF1F5F9), // Pure White
                Color(0xFFE2E8F0), // Platinum
                Color(0xFFFFB300), // Rich Gold
                Color(0xFF00E5FF)  // Glowing Cyan
            )
            bezelColors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            if (bezelColor == color) 2.dp else 0.dp,
                            if (color.toArgb() == Color.White.toArgb()) Color.Black else Color.White,
                            CircleShape
                        )
                        .clickable { onBezelColorChange(color) }
                )
            }
        }
    }
}

// ==========================================
// CANVAS & 3D PERSPECTIVE CONTROLS CONTENT
// ==========================================

@Composable
fun CanvasTabContent(
    activeRatio: CanvasRatio,
    tiltX: Float,
    tiltY: Float,
    tiltZ: Float,
    perspectiveDepth: Float,
    shadowStrength: Float,
    deviceFrameScale: Float,
    shadowEnabled: Boolean,
    shadowBlurRadius: Float,
    shadowOffsetX: Float,
    shadowOffsetY: Float,
    shadowColor: Color,
    onRatioChange: (CanvasRatio) -> Unit,
    onTiltXChange: (Float) -> Unit,
    onTiltYChange: (Float) -> Unit,
    onTiltZChange: (Float) -> Unit,
    onPerspectiveChange: (Float) -> Unit,
    onShadowStrengthChange: (Float) -> Unit,
    onDeviceFrameScaleChange: (Float) -> Unit,
    onShadowEnabledChange: (Boolean) -> Unit,
    onShadowBlurRadiusChange: (Float) -> Unit,
    onShadowOffsetXChange: (Float) -> Unit,
    onShadowOffsetYChange: (Float) -> Unit,
    onShadowColorChange: (Color) -> Unit
) {
    ControlCard(title = "Composition Zoom (Device Size)") {
        LabelSlider(
            label = "Device Frame Scale",
            value = deviceFrameScale,
            valueRange = 0.3f..0.95f,
            displayValue = "${(deviceFrameScale * 100).roundToInt()}%",
            onValueChange = onDeviceFrameScaleChange
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    ControlCard(title = "Canvas Aspect Ratio") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CanvasRatio.values().forEach { ratio ->
                val active = activeRatio == ratio
                Button(
                    onClick = { onRatioChange(ratio) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(ratio.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    ControlCard(title = "3D Graphics Lens Rotation") {
        LabelSlider(
            label = "Tilt X (Pitch Up/Down)",
            value = tiltX,
            valueRange = -45f..45f,
            displayValue = "${tiltX.roundToInt()}°",
            onValueChange = onTiltXChange
        )

        Spacer(modifier = Modifier.height(12.dp))

        LabelSlider(
            label = "Tilt Y (Yaw Left/Right)",
            value = tiltY,
            valueRange = -45f..45f,
            displayValue = "${tiltY.roundToInt()}°",
            onValueChange = onTiltYChange
        )

        Spacer(modifier = Modifier.height(12.dp))

        LabelSlider(
            label = "Roll Z (Spin Angle)",
            value = tiltZ,
            valueRange = -180f..180f,
            displayValue = "${tiltZ.roundToInt()}°",
            onValueChange = onTiltZChange
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quick Mockup Rotation",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            val rotationType = when (tiltZ.roundToInt()) {
                0 -> "Portrait"
                90 -> "Landscape"
                180, -180 -> "Upside Portrait"
                90, -90 -> "Landscape Rev"
                else -> "Custom Rotation"
            }
            Text(
                text = rotationType,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val rotationAngles = listOf(
                0f to "Portrait (0°)",
                90f to "Landscape (90°)",
                180f to "Upside Portrait (180°)",
                -90f to "Landscape Rev (270°)"
            )
            rotationAngles.forEach { (angle, desc) ->
                val active = tiltZ.roundToInt() == angle.roundToInt()
                Button(
                    onClick = { onTiltZChange(angle) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        contentColor = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("rotate_btn_${if (angle < 0) 270 else angle.toInt()}"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
                    border = if (active) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Smartphone,
                            contentDescription = desc,
                            modifier = Modifier
                                .size(18.dp)
                                .graphicsLayer { rotationZ = angle },
                            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (angle) {
                                0f -> "0°"
                                90f -> "90°"
                                180f -> "180°"
                                else -> "270°"
                            },
                            fontSize = 10.sp,
                            fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LabelSlider(
            label = "Camera Perspective Depth",
            value = perspectiveDepth,
            valueRange = 4f..20f,
            displayValue = "${24 - perspectiveDepth.roundToInt()}/24",
            onValueChange = onPerspectiveChange
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    ControlCard(title = "Device Drop Shadow") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Enable Drop Shadow",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Add realistic depth & colorful glow",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Switch(
                checked = shadowEnabled,
                onCheckedChange = onShadowEnabledChange,
                modifier = Modifier.testTag("shadow_enabled_toggle")
            )
        }

        if (shadowEnabled) {
            Spacer(modifier = Modifier.height(16.dp))

            LabelSlider(
                label = "Shadow Opacity",
                value = shadowStrength,
                valueRange = 0f..1f,
                displayValue = "${(shadowStrength * 100).roundToInt()}%",
                onValueChange = onShadowStrengthChange
            )

            Spacer(modifier = Modifier.height(12.dp))

            LabelSlider(
                label = "Shadow Softness / Blur",
                value = shadowBlurRadius,
                valueRange = 0f..80f,
                displayValue = "${shadowBlurRadius.roundToInt()}dp",
                onValueChange = onShadowBlurRadiusChange
            )

            Spacer(modifier = Modifier.height(12.dp))

            LabelSlider(
                label = "Light Position X (Offset)",
                value = shadowOffsetX,
                valueRange = -50f..50f,
                displayValue = "${shadowOffsetX.roundToInt()}dp",
                onValueChange = onShadowOffsetXChange
            )

            Spacer(modifier = Modifier.height(12.dp))

            LabelSlider(
                label = "Light Position Y (Offset)",
                value = shadowOffsetY,
                valueRange = -50f..50f,
                displayValue = "${shadowOffsetY.roundToInt()}dp",
                onValueChange = onShadowOffsetYChange
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Shadow Tint & Glow Tone",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            val shadowPresetColors = listOf(
                Pair("Classic Black", Color.Black),
                Pair("Slate Grey", Color(0xFF475569)),
                Pair("Neon Cyan", Color(0xFF00E5FF)),
                Pair("Cosmic Violet", Color(0xFF7000FF)),
                Pair("Neon Pink", Color(0xFFF355DA)),
                Pair("Amber Glow", Color(0xFFFF9100))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                shadowPresetColors.forEach { (name, color) ->
                    val isSelected = shadowColor == color
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .clickable { onShadowColorChange(color) }
                            .testTag("shadow_color_${name.lowercase().replace(" ", "_")}")
                    )
                }
            }
        }
    }
}

// ==========================================
// WATERMARK CONTROLS CONTENT
// ==========================================

@Composable
fun ExportTabContent(
    exportFormat: String,
    exportQuality: Float,
    onExportFormatChange: (String) -> Unit,
    onExportQualityChange: (Float) -> Unit
) {
    ControlCard(title = "Output Image Format", icon = Icons.Default.Save) {
        Text(
            text = "Choose the format for your rendered mockup image. PNG is lossless and has the highest details, while JPEG and WebP offer great compression control.",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val formats = listOf("PNG", "JPEG", "WebP")
            formats.forEach { format ->
                val isSelected = exportFormat == format
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_format_${format.lowercase()}")
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.15f)
                            else Color.White.copy(alpha = 0.04f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onExportFormatChange(format) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = format,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color(0xFF00E5FF) else Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Small description of current format benefits
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.02f))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = when (exportFormat) {
                    "PNG" -> "PNG: Lossless, crispest graphics. Best for high-contrast presentation. (Quality is fixed at 100%)"
                    "JPEG" -> "JPEG: High compatibility. Adjustable quality. Great for reducing file sizes."
                    "WebP" -> "WebP: Next-gen image format. Superior compression. Highly recommended."
                    else -> ""
                },
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Quality slider - enabled only for lossy format (JPEG/WebP) or we can show 100% fixed for PNG
    val isQualityAdjustable = exportFormat != "PNG"

    ControlCard(title = "Compression Quality", icon = Icons.Default.Tune) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Export Quality",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = if (isQualityAdjustable) Color.White else Color.White.copy(alpha = 0.4f)
            )
            Text(
                text = if (isQualityAdjustable) "${exportQuality.toInt()}%" else "100% (Lossless)",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isQualityAdjustable) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.4f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = if (isQualityAdjustable) exportQuality else 100f,
            onValueChange = { if (isQualityAdjustable) onExportQualityChange(it) },
            valueRange = 10f..100f,
            steps = 17, // 5% steps (10, 15, 20, ..., 100)
            enabled = isQualityAdjustable,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("export_quality_slider"),
            colors = SliderDefaults.colors(
                activeTrackColor = if (isQualityAdjustable) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.1f),
                thumbColor = if (isQualityAdjustable) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.2f)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isQualityAdjustable) {
                if (exportQuality >= 90f) "Excellent visual quality, minimal compression artifacts."
                else if (exportQuality >= 70f) "Balanced quality and file size. Perfect for web sharing."
                else "High compression, small file size. May show slight artifacting."
            } else {
                "PNG automatically saves with full details. No compression loss."
            },
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun WatermarkTabContent(
    showWatermark: Boolean,
    watermarkText: String,
    watermarkPosition: WatermarkPosition,
    watermarkColor: Color,
    watermarkSize: Float,
    watermarkOpacity: Float,
    onShowWatermarkChange: (Boolean) -> Unit,
    onTextChange: (String) -> Unit,
    onPositionChange: (WatermarkPosition) -> Unit,
    onColorChange: (Color) -> Unit,
    onSizeChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit
) {
    ControlCard(title = "Watermark Visibility") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable Decorative Watermark", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Switch(
                checked = showWatermark,
                onCheckedChange = onShowWatermarkChange
            )
        }
    }

    if (showWatermark) {
        Spacer(modifier = Modifier.height(16.dp))

        ControlCard(title = "Watermark Text Settings") {
            OutlinedTextField(
                value = watermarkText,
                onValueChange = onTextChange,
                label = { Text("Watermark Label") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Quick Watermark Presets:",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val suggestions = listOf("Remove All", "Sayanth Rock", "Rock Screen", "HiShoot Pro")
                suggestions.forEach { text ->
                    val isSelected = if (text == "Remove All") {
                        !showWatermark || watermarkText.isEmpty()
                    } else {
                        showWatermark && watermarkText == text
                    }
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.error else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                if (text == "Remove All") {
                                    onShowWatermarkChange(false)
                                    onTextChange("")
                                } else {
                                    onShowWatermarkChange(true)
                                    onTextChange(text)
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = text,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LabelSlider(
                label = "Font Size",
                value = watermarkSize,
                valueRange = 10f..24f,
                displayValue = "${watermarkSize.roundToInt()}sp",
                onValueChange = onSizeChange
            )

            Spacer(modifier = Modifier.height(12.dp))

            LabelSlider(
                label = "Watermark Opacity",
                value = watermarkOpacity,
                valueRange = 0.1f..1.0f,
                displayValue = "${(watermarkOpacity * 100).roundToInt()}%",
                onValueChange = onOpacityChange
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        ControlCard(title = "Watermark Alignment") {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WatermarkPosition.values().forEach { pos ->
                    val active = watermarkPosition == pos
                    Button(
                        onClick = { onPositionChange(pos) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(pos.label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ControlCard(title = "Watermark Color Tone") {
            val colors = listOf(
                Color(0xFF00E5FF), // Electric Cyan
                Color(0xFFE040FB), // Vivid Orchid
                Color(0xFFFFFFFF), // Pure White
                Color(0xFF90A4AE), // Cool Slate
                Color(0xFFFFEE58), // Electric Yellow
                Color(0xFF66BB6A)  // Light Green
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                colors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                if (watermarkColor.toArgb() == color.toArgb()) 2.dp else 0.dp,
                                if (color.toArgb() == Color.White.toArgb()) Color.Black else Color.White,
                                CircleShape
                              )
                            .clickable { onColorChange(color) }
                    )
                }
            }
        }
    }
}

// ==========================================
// REUSABLE PRESENTATIONAL UI COMPONENTS
// ==========================================

@Composable
fun ControlCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)), // Deep solid card surface
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            if (title != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    if (icon != null) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF818CF8).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = Color(0xFF818CF8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = title.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color(0xFF818CF8), // Periwinkle Indigo accent
                        letterSpacing = 1.sp
                    )
                }
            }
            content()
        }
    }
}

@Composable
fun LabelSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.7f)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = displayValue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF818CF8) // Periwinkle Indigo accent
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF818CF8),
                activeTrackColor = Color(0xFF818CF8),
                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
            ),
            modifier = Modifier.height(24.dp)
        )
    }
}

fun Modifier.studioGridBackground(
    gridSize: Float = 48f,
    dotRadius: Float = 1.5f,
    color: Color = Color.White.copy(alpha = 0.05f)
): Modifier = this.drawBehind {
    val width = size.width
    val height = size.height
    // Draw dot grid
    var x = 0f
    while (x < width) {
        var y = 0f
        while (y < height) {
            drawCircle(
                color = color,
                radius = dotRadius,
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
            y += gridSize
        }
        x += gridSize
    }
}

// ==========================================
// PROCEDURAL IMAGE UTILITIES
// ==========================================

fun getScreenshotAspectRatio(context: Context, uri: Uri): Float {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            if (options.outWidth > 0 && options.outHeight > 0) {
                options.outWidth.toFloat() / options.outHeight.toFloat()
            } else {
                9f / 19.5f
            }
        } ?: (9f / 19.5f)
    } catch (e: Exception) {
        e.printStackTrace()
        9f / 19.5f
    }
}

fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Super-fast optimized pure-Kotlin Box-Blur algorithm.
 * Operates efficiently on scaled-down bitmaps.
 */
fun blurBitmap(sentBitmap: Bitmap, radius: Int): Bitmap {
    val bitmap = sentBitmap.copy(sentBitmap.config ?: Bitmap.Config.ARGB_8888, true)
    if (radius < 1) return bitmap

    val w = bitmap.width
    val h = bitmap.height
    val pix = IntArray(w * h)
    bitmap.getPixels(pix, 0, w, 0, 0, w, h)

    val wm = w - 1
    val hm = h - 1
    val wh = w * h
    val div = radius + radius + 1

    val r = IntArray(wh)
    val g = IntArray(wh)
    val b = IntArray(wh)
    var rsum: Int
    var gsum: Int
    var bsum: Int
    var x: Int
    var y: Int
    var i: Int
    var p: Int
    var yp: Int
    var yi: Int
    var yw: Int

    val vmin = IntArray(Math.max(w, h))
    val dv = IntArray(256 * div)
    for (idx in 0 until 256 * div) {
        dv[idx] = idx / div
    }

    yw = 0
    yi = 0

    for (row in 0 until h) {
        rsum = 0
        gsum = 0
        bsum = 0
        for (col in -radius..radius) {
            p = pix[yi + Math.min(wm, Math.max(col, 0))]
            rsum += p shr 16 and 0xff
            gsum += p shr 8 and 0xff
            bsum += p and 0xff
        }
        for (col in 0 until w) {
            r[yi] = dv[rsum]
            g[yi] = dv[gsum]
            b[yi] = dv[bsum]

            if (row == 0) {
                vmin[col] = Math.min(col + radius + 1, wm)
            }
            val p1 = pix[yw + vmin[col]]
            val p2 = pix[yw + Math.max(col - radius, 0)]

            rsum += (p1 shr 16 and 0xff) - (p2 shr 16 and 0xff)
            gsum += (p1 shr 8 and 0xff) - (p2 shr 8 and 0xff)
            bsum += (p1 and 0xff) - (p2 and 0xff)
            yi++
        }
        yw += w
    }

    for (col in 0 until w) {
        rsum = 0
        gsum = 0
        bsum = 0
        yp = -radius * w
        for (row in -radius..radius) {
            yi = Math.max(0, yp) + col
            rsum += r[yi]
            gsum += g[yi]
            bsum += b[yi]
            yp += w
        }
        yi = col
        for (row in 0 until h) {
            pix[yi] = (-0x1000000 or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum])
            if (col == 0) {
                vmin[row] = Math.min(row + radius + 1, hm) * w
            }
            val p1 = col + vmin[row]
            val p2 = col + Math.max(row - radius, 0) * w

            rsum += r[p1] - r[p2]
            gsum += g[p1] - g[p2]
            bsum += b[p1] - b[p2]
            yi += w
        }
    }

    bitmap.setPixels(pix, 0, w, 0, 0, w, h)
    return bitmap

}

/**
 * Calculates a combined color matrix array for Grayscale, Sepia, Brightness, and Contrast.
 */
fun getCombinedColorMatrixArray(
    grayscale: Float,
    sepia: Float,
    brightness: Float,
    contrast: Float
): FloatArray {
    val m = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )
    
    // 1. Grayscale / Saturation
    val sat = 1f - grayscale
    if (sat != 1f) {
        val invSat = 1f - sat
        val r = 0.213f * invSat
        val g = 0.715f * invSat
        val b = 0.072f * invSat
        
        m[0] = r + sat; m[1] = g; m[2] = b
        m[5] = r; m[6] = g + sat; m[7] = b
        m[10] = r; m[11] = g; m[12] = b + sat
    }
    
    // 2. Sepia
    if (sepia > 0f) {
        val s = sepia
        val invS = 1f - s
        
        val s00 = 0.393f * s + invS; val s01 = 0.769f * s; val s02 = 0.189f * s
        val s10 = 0.349f * s; val s11 = 0.686f * s + invS; val s12 = 0.168f * s
        val s20 = 0.272f * s; val s21 = 0.534f * s; val s22 = 0.131f * s + invS
        
        val r00 = m[0]*s00 + m[5]*s01 + m[10]*s02
        val r01 = m[1]*s00 + m[6]*s01 + m[11]*s02
        val r02 = m[2]*s00 + m[7]*s01 + m[12]*s02
        
        val r10 = m[0]*s10 + m[5]*s11 + m[10]*s12
        val r11 = m[1]*s10 + m[6]*s11 + m[11]*s12
        val r12 = m[2]*s10 + m[7]*s11 + m[12]*s12
        
        val r20 = m[0]*s20 + m[5]*s21 + m[10]*s22
        val r21 = m[1]*s20 + m[6]*s21 + m[11]*s22
        val r22 = m[2]*s20 + m[7]*s21 + m[12]*s22
        
        m[0] = r00; m[1] = r01; m[2] = r02
        m[5] = r10; m[6] = r11; m[7] = r12
        m[10] = r20; m[11] = r21; m[12] = r22
    }
    
    // 3. Contrast
    if (contrast != 1f) {
        val c = contrast
        val off = 128f * (1f - c)
        
        m[0] *= c; m[1] *= c; m[2] *= c; m[4] = m[4] * c + off
        m[5] *= c; m[6] *= c; m[7] *= c; m[9] = m[9] * c + off
        m[10] *= c; m[11] *= c; m[12] *= c; m[14] = m[14] * c + off
    }
    
    // 4. Brightness
    val bOffset = (brightness - 1f) * 255f
    if (bOffset != 0f) {
        m[4] += bOffset
        m[9] += bOffset
        m[14] += bOffset
    }
    
    return m
}

// ==========================================
// HIGH-RESOLUTION MOCKUP EXPORT ENGINE
// ==========================================

/**
 * Creates a high-definition Bitmap drawing matching the user configurations.
 */
suspend fun renderHighResMockup(
    context: Context,
    aspectRatio: CanvasRatio,
    backgroundType: BackgroundType,
    solidColor: Color,
    gradient: CustomGradient,
    imageUri: Uri?,
    deviceFrameAspectRatio: Float = 9f / 19.5f,
    ambientBlurRadius: Float,
    screenshotScale: Float,
    screenshotOffsetX: Float,
    screenshotOffsetY: Float,
    activeTemplate: MockupTemplate,
    bezelColor: Color,
    bezelThickness: Float,
    screenCornerRadius: Float,
    showStatusBarIcons: Boolean,
    tiltX: Float,
    tiltY: Float,
    tiltZ: Float,
    perspectiveDepth: Float,
    shadowStrength: Float,
    showWatermark: Boolean,
    watermarkText: String,
    watermarkPosition: WatermarkPosition,
    watermarkColor: Color,
    watermarkSize: Float,
    watermarkOpacity: Float,
    liquidThemeIndex: Int = 0,
    liquidScale: Float = 1.2f,
    showDisplayGlassBlur: Boolean = false,
    displayGlassBlurColor: Color = Color(0xFF00E5FF),
    displayGlassBlurOpacity: Float = 0.4f,
    patternType: PatternType = PatternConfig.selectedPatternType,
    patternPrimaryColor: Color = PatternConfig.patternPrimaryColor,
    patternSecondaryColor: Color = PatternConfig.patternSecondaryColor,
    patternBgColor: Color = PatternConfig.patternBgColor,
    patternScale: Float = PatternConfig.patternScale,
    deviceFrameScale: Float = 0.72f,
    shadowEnabled: Boolean = true,
    shadowBlurRadius: Float = 25f,
    shadowOffsetX: Float = 10f,
    shadowOffsetY: Float = 15f,
    shadowColor: Color = Color.Black,
    backgroundUri: Uri? = null,
    backgroundBlurRadius: Float = 0f,
    showGlossyReflection: Boolean = true,
    glossyReflectionOpacity: Float = 0.18f,
    screenshotGrayscale: Float = 0f,
    screenshotSepia: Float = 0f,
    screenshotBrightness: Float = 1f,
    screenshotContrast: Float = 1f,
    screenshotLiquidGlass: Boolean = false,
    isDesktop: Boolean = false,
    buttonStyle: String = "android",
    hasFoldLine: Boolean = false
): Bitmap = withContext(Dispatchers.IO) {
    // 1. CHOOSE HIGH RESOLUTION EXPORT SIZE (Standard 1080p width base)
    val width = 1440
    val height = (width / aspectRatio.ratio).roundToInt()

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // 2. DRAW BACKGROUND PAINT
    val bgPaint = Paint().apply { isAntiAlias = true }

    when (backgroundType) {
        BackgroundType.SOLID -> {
            canvas.drawColor(solidColor.toArgb())
        }
        BackgroundType.GRADIENT -> {
            val colors = gradient.colors.map { it.toArgb() }.toIntArray()
            val shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                colors, null, Shader.TileMode.CLAMP
            )
            bgPaint.shader = shader
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        }
        BackgroundType.PATTERN -> {
            canvas.drawColor(patternBgColor.toArgb())
            val pPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 1.5f * patternScale * (width / 400f)
            }
            
            when (patternType) {
                PatternType.DOTS -> {
                    pPaint.style = Paint.Style.FILL
                    pPaint.color = patternPrimaryColor.toArgb()
                    val step = 32f * patternScale * (width / 400f)
                    val radius = 2f * patternScale * (width / 400f)
                    var x = step / 2f
                    while (x < width) {
                        var y = step / 2f
                        while (y < height) {
                            canvas.drawCircle(x, y, radius, pPaint)
                            y += step
                        }
                        x += step
                    }
                }
                PatternType.GRID -> {
                    pPaint.color = patternPrimaryColor.copy(alpha = 0.2f).toArgb()
                    pPaint.strokeWidth = 1f * (width / 400f)
                    val step = 40f * patternScale * (width / 400f)
                    var x = 0f
                    while (x < width) {
                        canvas.drawLine(x, 0f, x, height.toFloat(), pPaint)
                        x += step
                    }
                    var y = 0f
                    while (y < height) {
                        canvas.drawLine(0f, y, width.toFloat(), y, pPaint)
                        y += step
                    }
                }
                PatternType.STRIPES -> {
                    pPaint.color = patternPrimaryColor.copy(alpha = 0.15f).toArgb()
                    pPaint.strokeWidth = 4f * patternScale * (width / 400f)
                    val step = 24f * patternScale * (width / 400f)
                    var x = -height.toFloat()
                    while (x < width) {
                        canvas.drawLine(x, 0f, x + height, height.toFloat(), pPaint)
                        x += step
                    }
                }
                PatternType.WAVES -> {
                    pPaint.color = patternPrimaryColor.copy(alpha = 0.25f).toArgb()
                    pPaint.strokeWidth = 1.5f * patternScale * (width / 400f)
                    
                    val lineSpacing = 36f * patternScale * (width / 400f)
                    val amplitude = 12f * patternScale * (width / 400f)
                    val frequency = (0.015f / patternScale) * (400f / width)
                    
                    var yOffset = -amplitude
                    while (yOffset < height + amplitude) {
                        val path = Path()
                        path.moveTo(0f, yOffset)
                        var x = 0f
                        while (x <= width) {
                            val y = yOffset + amplitude * Math.sin((x * frequency).toDouble()).toFloat()
                            path.lineTo(x, y)
                            x += 10f * (width / 400f)
                        }
                        canvas.drawPath(path, pPaint)
                        yOffset += lineSpacing
                    }
                }
                PatternType.HEXAGON -> {
                    pPaint.color = patternPrimaryColor.copy(alpha = 0.12f).toArgb()
                    pPaint.strokeWidth = 1f * (width / 400f)
                    
                    val r = 24f * patternScale * (width / 400f)
                    val h = r * Math.sin(Math.toRadians(30.0)).toFloat()
                    val w = r * Math.cos(Math.toRadians(30.0)).toFloat()
                    
                    var row = 0
                    var y = -r
                    while (y < height + r) {
                        val xOffset = if (row % 2 == 1) w else 0f
                        var x = -w
                        while (x < width + w) {
                            val path = Path()
                            val cx = x + xOffset
                            val cy = y
                            path.moveTo(cx, cy - r)
                            path.lineTo(cx + w, cy - h)
                            path.lineTo(cx + w, cy + h)
                            path.lineTo(cx, cy + r)
                            path.lineTo(cx - w, cy + h)
                            path.lineTo(cx - w, cy - h)
                            path.close()
                            
                            canvas.drawPath(path, pPaint)
                            x += 2 * w
                        }
                        y += r + h
                        row++
                    }
                }
                PatternType.STARS -> {
                    pPaint.color = patternPrimaryColor.copy(alpha = 0.15f).toArgb()
                    pPaint.strokeWidth = 0.8f * (width / 400f)
                    
                    val fPaint = Paint().apply {
                        isAntiAlias = true
                        style = Paint.Style.FILL
                        color = AndroidColor.argb((0.75f * 255).toInt(), 255, 255, 255)
                    }
                    
                    val starCount = 60
                    for (i in 0 until starCount) {
                        val xPct = Math.abs(Math.sin((i * 1234.56).toDouble())).toFloat()
                        val yPct = Math.abs(Math.cos((i * 6543.21).toDouble())).toFloat()
                        val starSize = (1.5f + Math.abs(Math.sin((i * 999.9).toDouble())).toFloat() * 2.5f) * patternScale * (width / 400f)
                        
                        val px = xPct * width
                        val py = yPct * height
                        
                        canvas.drawLine(px - starSize * 2f, py, px + starSize * 2f, py, pPaint)
                        canvas.drawLine(px, py - starSize * 2f, px, py + starSize * 2f, pPaint)
                        canvas.drawCircle(px, py, starSize * 0.5f, fPaint)
                    }
                }
            }
        }
        BackgroundType.AMBIENT_BLUR -> {
            var loadedBg: Bitmap? = null
            if (imageUri != null) {
                val sBmp = uriToBitmap(context, imageUri)
                if (sBmp != null) {
                    val scaled = Bitmap.createScaledBitmap(sBmp, 200, (200 * (16f/9f)).roundToInt(), true)
                    loadedBg = blurBitmap(scaled, ambientBlurRadius.roundToInt().coerceIn(1, 25))
                }
            }
            if (loadedBg != null) {
                val dstRect = android.graphics.Rect(0, 0, width, height)
                canvas.drawBitmap(loadedBg, null, dstRect, bgPaint)
            } else {
                // Fallback Solid slate
                canvas.drawColor(PresetColors[0].toArgb())
            }
        }
        BackgroundType.LIQUID_FLOW -> {
            // Draw deep space base
            canvas.drawColor(AndroidColor.parseColor("#070B19"))

            val themeColors = listOf(
                listOf(0xFF00E5FF.toInt(), 0xFFF355DA.toInt(), 0xFF7000FF.toInt(), 0xFFFF007F.toInt()),
                listOf(0xFFFF5E62.toInt(), 0xFFFF9966.toInt(), 0xFFFFD200.toInt(), 0xFF8A2387.toInt()),
                listOf(0xFF00F0FF.toInt(), 0xFF0072FF.toInt(), 0xFF00FF87.toInt(), 0xFF11998E.toInt()),
                listOf(0xFF8A2387.toInt(), 0xFFE94057.toInt(), 0xFFF27121.toInt(), 0xFF3F2B96.toInt()),
                listOf(0xFF11998E.toInt(), 0xFF38EF7D.toInt(), 0xFF00F2FE.toInt(), 0xFF4FACFE.toInt())
            )
            val colors = themeColors.getOrElse(liquidThemeIndex) { themeColors[0] }
            val radiusBase = Math.min(width, height) * 0.75f * liquidScale

            val paint = Paint().apply { isAntiAlias = true }

            // Draw Blob 1
            val shader1 = RadialGradient(
                width * 0.7f, height * 0.4f, radiusBase * 1.1f,
                intArrayOf((colors[0] and 0x00FFFFFF) or 0x90000000.toInt(), (colors[0] and 0x00FFFFFF) or 0x24000000.toInt(), AndroidColor.TRANSPARENT),
                floatArrayOf(0f, 0.4f, 1f), Shader.TileMode.CLAMP
            )
            paint.shader = shader1
            canvas.drawCircle(width * 0.7f, height * 0.4f, radiusBase * 1.1f, paint)

            // Draw Blob 2
            val shader2 = RadialGradient(
                width * 0.3f, height * 0.62f, radiusBase * 0.9f,
                intArrayOf((colors[1] and 0x00FFFFFF) or 0x80000000.toInt(), (colors[1] and 0x00FFFFFF) or 0x1A000000.toInt(), AndroidColor.TRANSPARENT),
                floatArrayOf(0f, 0.4f, 1f), Shader.TileMode.CLAMP
            )
            paint.shader = shader2
            canvas.drawCircle(width * 0.3f, height * 0.62f, radiusBase * 0.9f, paint)

            // Draw Blob 3
            val shader3 = RadialGradient(
                width * 0.45f, height * 0.32f, radiusBase * 1.2f,
                intArrayOf((colors[2] and 0x00FFFFFF) or 0x75000000.toInt(), (colors[2] and 0x00FFFFFF) or 0x14000000.toInt(), AndroidColor.TRANSPARENT),
                floatArrayOf(0f, 0.4f, 1f), Shader.TileMode.CLAMP
            )
            paint.shader = shader3
            canvas.drawCircle(width * 0.45f, height * 0.32f, radiusBase * 1.2f, paint)

            // Draw Blob 4
            val shader4 = RadialGradient(
                width * 0.58f, height * 0.7f, radiusBase * 0.85f,
                intArrayOf((colors[3] and 0x00FFFFFF) or 0x65000000.toInt(), (colors[3] and 0x00FFFFFF) or 0x0D000000.toInt(), AndroidColor.TRANSPARENT),
                floatArrayOf(0f, 0.4f, 1f), Shader.TileMode.CLAMP
            )
            paint.shader = shader4
            canvas.drawCircle(width * 0.58f, height * 0.7f, radiusBase * 0.85f, paint)
        }
        BackgroundType.IMAGE -> {
            var loadedBg: Bitmap? = null
            if (backgroundUri != null) {
                val sBmp = uriToBitmap(context, backgroundUri)
                if (sBmp != null) {
                    if (backgroundBlurRadius > 0f) {
                        // Scale down for fast and smooth blur computation
                        val scaleFactor = 400
                        val scaledWidth = scaleFactor
                        val scaledHeight = (scaleFactor * (sBmp.height.toFloat() / sBmp.width)).roundToInt().coerceAtLeast(1)
                        val scaled = Bitmap.createScaledBitmap(sBmp, scaledWidth, scaledHeight, true)
                        loadedBg = blurBitmap(scaled, backgroundBlurRadius.roundToInt().coerceIn(1, 25))
                    } else {
                        loadedBg = sBmp
                    }
                }
            }
            if (loadedBg != null) {
                val dstRect = android.graphics.Rect(0, 0, width, height)
                canvas.drawBitmap(loadedBg, null, dstRect, bgPaint)
            } else {
                canvas.drawColor(PresetColors[0].toArgb())
            }
        }
    }

    // Subtle Radial spotlight vignette
    val spotlight = RadialGradient(
        width / 2f, height / 2f, Math.max(width, height) * 0.8f,
        intArrayOf(AndroidColor.argb((255 * 0.08).toInt(), 255, 255, 255), AndroidColor.TRANSPARENT),
        null, Shader.TileMode.CLAMP
    )
    val spotlightPaint = Paint().apply {
        isAntiAlias = true
        shader = spotlight
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), spotlightPaint)

    // 3. GRAPHICS LAYER MATRIX 3D TRANSFORMATION USING CAMERA
    val deviceWidth = width * deviceFrameScale
    val deviceHeight = deviceWidth / deviceFrameAspectRatio

    val deviceCenterX = width / 2f
    val deviceCenterY = height / 2f

    // We instantiate a real android.graphics.Camera to perform accurate projection!
    val androidCamera = android.graphics.Camera()
    val matrix = Matrix()

    androidCamera.save()
    // Align with Jetpack Compose tilt signs
    androidCamera.rotateX(-tiltX)
    androidCamera.rotateY(tiltY)
    androidCamera.rotateZ(tiltZ)
    
    // Scale camera distance to avoid standard clip limits
    val distance = perspectiveDepth * 72f
    androidCamera.setLocation(0f, 0f, -distance)
    
    androidCamera.getMatrix(matrix)
    androidCamera.restore()

    // Matrix translation to center, rotate, and scale
    matrix.preTranslate(-deviceCenterX, -deviceCenterY)
    matrix.postTranslate(deviceCenterX, deviceCenterY)

    // Apply 3D Matrix to entire device frame rendering pass!
    canvas.save()
    canvas.concat(matrix)

    // Draw frame shadow
    if (shadowStrength > 0.01f) {
        val shadowPaint = Paint().apply {
            isAntiAlias = true
            if (shadowEnabled) {
                color = shadowColor.toArgb()
                alpha = (255 * shadowStrength).toInt()
                
                // Scale blur to match high-res canvas scale
                val scaleFactor = deviceWidth / 260f
                val blurRad = shadowBlurRadius * scaleFactor
                if (blurRad > 0.1f) {
                    maskFilter = android.graphics.BlurMaskFilter(blurRad, android.graphics.BlurMaskFilter.Blur.NORMAL)
                }
            } else {
                color = AndroidColor.BLACK
                alpha = (255 * shadowStrength * 0.5f).toInt()
            }
        }
        
        val scaleFactor = deviceWidth / 260f
        val offsetX = if (shadowEnabled) shadowOffsetX * scaleFactor else deviceWidth * 0.025f
        val offsetY = if (shadowEnabled) shadowOffsetY * scaleFactor else deviceWidth * 0.025f
        val rx = screenCornerRadius * (deviceWidth / 1000f) * 20f
        val shadowRect = RectF(
            deviceCenterX - deviceWidth / 2f + offsetX,
            deviceCenterY - deviceHeight / 2f + offsetY,
            deviceCenterX + deviceWidth / 2f + offsetX,
            deviceCenterY + deviceHeight / 2f + offsetY
        )
        canvas.drawRoundRect(shadowRect, rx, rx, shadowPaint)
    }

    // Draw bezel outer shell
    val bezelPaint = Paint().apply {
        isAntiAlias = true
        color = bezelColor.toArgb()
        style = Paint.Style.FILL
    }
    val rxOuter = screenCornerRadius * (deviceWidth / 280f)
    val outerRect = if (isDesktop) {
        val standHeight = deviceHeight * 0.18f
        RectF(
            deviceCenterX - deviceWidth / 2f,
            deviceCenterY - deviceHeight / 2f,
            deviceCenterX + deviceWidth / 2f,
            deviceCenterY + deviceHeight / 2f - standHeight
        )
    } else {
        RectF(
            deviceCenterX - deviceWidth / 2f,
            deviceCenterY - deviceHeight / 2f,
            deviceCenterX + deviceWidth / 2f,
            deviceCenterY + deviceHeight / 2f
        )
    }
    canvas.drawRoundRect(outerRect, rxOuter, rxOuter, bezelPaint)

    // Draw Physical side buttons
    if (buttonStyle == "iphone") {
        val buttonPaint = Paint().apply {
            isAntiAlias = true
            color = bezelColor.toArgb()
            style = Paint.Style.FILL
        }
        val buttonThickness = (deviceWidth / 280f) * 4f
        val rxBtn = (deviceWidth / 280f) * 1.5f
        
        val scaleYFactor = outerRect.height() / 620f
        
        // Left: silent, vol up, vol down
        val silentRect = RectF(
            outerRect.left - buttonThickness,
            outerRect.top + 100f * scaleYFactor,
            outerRect.left,
            outerRect.top + (100f + 30f) * scaleYFactor
        )
        canvas.drawRoundRect(silentRect, rxBtn, rxBtn, buttonPaint)
        
        val volUpRect = RectF(
            outerRect.left - buttonThickness,
            outerRect.top + 145f * scaleYFactor,
            outerRect.left,
            outerRect.top + (145f + 50f) * scaleYFactor
        )
        canvas.drawRoundRect(volUpRect, rxBtn, rxBtn, buttonPaint)
        
        val volDownRect = RectF(
            outerRect.left - buttonThickness,
            outerRect.top + 205f * scaleYFactor,
            outerRect.left,
            outerRect.top + (205f + 50f) * scaleYFactor
        )
        canvas.drawRoundRect(volDownRect, rxBtn, rxBtn, buttonPaint)
        
        // Right: power
        val powerRect = RectF(
            outerRect.right,
            outerRect.top + 165f * scaleYFactor,
            outerRect.right + buttonThickness,
            outerRect.top + (165f + 75f) * scaleYFactor
        )
        canvas.drawRoundRect(powerRect, rxBtn, rxBtn, buttonPaint)
    } else if (buttonStyle == "android") {
        val buttonPaint = Paint().apply {
            isAntiAlias = true
            color = bezelColor.toArgb()
            style = Paint.Style.FILL
        }
        val buttonThickness = (deviceWidth / 280f) * 4f
        val rxBtn = (deviceWidth / 280f) * 1.5f
        
        val scaleYFactor = outerRect.height() / 620f
        
        // Right: Vol, Power
        val volRect = RectF(
            outerRect.right,
            outerRect.top + 120f * scaleYFactor,
            outerRect.right + buttonThickness,
            outerRect.top + (120f + 60f) * scaleYFactor
        )
        canvas.drawRoundRect(volRect, rxBtn, rxBtn, buttonPaint)
        
        val powerRect = RectF(
            outerRect.right,
            outerRect.top + 195f * scaleYFactor,
            outerRect.right + buttonThickness,
            outerRect.top + (195f + 40f) * scaleYFactor
        )
        canvas.drawRoundRect(powerRect, rxBtn, rxBtn, buttonPaint)
    }

    // Draw Desktop Stand if isDesktop is true
    if (isDesktop) {
        val standHeight = deviceHeight * 0.18f
        val stemWidth = deviceWidth * 0.08f
        val stemHeight = standHeight * 0.75f
        
        // Draw Stem
        val stemPaint = Paint().apply {
            isAntiAlias = true
            color = bezelColor.toArgb()
            style = Paint.Style.FILL
        }
        val stemLeft = deviceCenterX - stemWidth / 2f
        val stemTop = outerRect.bottom
        val stemRight = deviceCenterX + stemWidth / 2f
        val stemBottom = stemTop + stemHeight
        canvas.drawRect(stemLeft, stemTop, stemRight, stemBottom, stemPaint)

        // Draw Base
        val basePaint = Paint().apply {
            isAntiAlias = true
            color = bezelColor.copy(alpha = 0.9f).toArgb()
            style = Paint.Style.FILL
        }
        val baseWidth = deviceWidth * 0.35f
        val baseHeight = standHeight * 0.25f
        val baseLeft = deviceCenterX - baseWidth / 2f
        val baseTop = stemBottom
        val baseRight = deviceCenterX + baseWidth / 2f
        val baseBottom = baseTop + baseHeight
        val rxBase = rxOuter * 0.3f
        canvas.drawRoundRect(RectF(baseLeft, baseTop, baseRight, baseBottom), rxBase, rxBase, basePaint)
    }

    // Draw Screen area inside Bezel
    val bThicknessPx = bezelThickness * (deviceWidth / 280f)
    val innerRect = RectF(
        outerRect.left + bThicknessPx,
        outerRect.top + bThicknessPx,
        outerRect.right - bThicknessPx,
        outerRect.bottom - bThicknessPx
    )
    val rxInner = Math.max(0f, rxOuter - bThicknessPx)

    // Clip screen area path
    val screenPath = Path().apply {
        addRoundRect(innerRect, rxInner, rxInner, Path.Direction.CW)
    }
    
    canvas.save()
    canvas.clipPath(screenPath)

    // Screen fill
    val screenBgPaint = Paint().apply {
        isAntiAlias = true
        color = AndroidColor.parseColor("#0D1117")
    }
    canvas.drawRect(innerRect, screenBgPaint)

    // Screenshot image
    var screenshotBmp: Bitmap? = null
    if (imageUri != null) {
        screenshotBmp = uriToBitmap(context, imageUri)
    }

    if (screenshotBmp != null) {
        // Draw user screenshot with custom offset and scale parameters
        val scMatrix = Matrix()
        val scWidth = screenshotBmp.width
        val scHeight = screenshotBmp.height

        val aspectWidth = innerRect.width()
        val aspectHeight = innerRect.height()

        // Center Crop calculations
        val scaleX = aspectWidth / scWidth.toFloat()
        val scaleY = aspectHeight / scHeight.toFloat()
        val scale = Math.max(scaleX, scaleY) * screenshotScale

        scMatrix.postTranslate(-scWidth / 2f, -scHeight / 2f)
        scMatrix.postScale(scale, scale)
        
        // Translate to match
        val transX = innerRect.centerX() + (screenshotOffsetX * (deviceWidth / 280f))
        val transY = innerRect.centerY() + (screenshotOffsetY * (deviceWidth / 280f))
        scMatrix.postTranslate(transX, transY)

        val paint = Paint().apply { isAntiAlias = true }
        if (screenshotGrayscale > 0f || screenshotSepia > 0f || screenshotBrightness != 1f || screenshotContrast != 1f) {
            val filterMatrix = android.graphics.ColorMatrix(
                getCombinedColorMatrixArray(
                    screenshotGrayscale,
                    screenshotSepia,
                    screenshotBrightness,
                    screenshotContrast
                )
            )
            paint.colorFilter = android.graphics.ColorMatrixColorFilter(filterMatrix)
        }
        canvas.drawBitmap(screenshotBmp, scMatrix, paint)

        // Draw Liquid Glass overlay on the screenshot
        if (screenshotLiquidGlass) {
            // 1. Draw Liquid Glass Background tint
            val glassBgPaint = Paint().apply {
                isAntiAlias = true
                shader = LinearGradient(
                    innerRect.left, innerRect.top, innerRect.left, innerRect.bottom,
                    intArrayOf(
                        AndroidColor.parseColor("#3A00F5D4"),
                        AndroidColor.parseColor("#107B2CBF"),
                        AndroidColor.parseColor("#409B5DE5")
                    ),
                    null, Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(innerRect, glassBgPaint)
            
            // 2. Draw glossy reflection arc path
            val path = Path().apply {
                moveTo(innerRect.left, innerRect.top)
                lineTo(innerRect.right, innerRect.top)
                cubicTo(innerRect.right, innerRect.top + innerRect.height() * 0.4f, innerRect.left, innerRect.top + innerRect.height() * 0.7f, innerRect.left, innerRect.bottom)
                close()
            }
            val arcPaint = Paint().apply {
                isAntiAlias = true
                shader = LinearGradient(
                    innerRect.left, innerRect.top, innerRect.left, innerRect.bottom,
                    intArrayOf(
                        AndroidColor.argb((255 * 0.25f).toInt(), 255, 255, 255),
                        AndroidColor.argb((255 * 0.02f).toInt(), 255, 255, 255)
                    ),
                    null, Shader.TileMode.CLAMP
                )
            }
            canvas.drawPath(path, arcPaint)
            
            // 3. Draw secondary glowing glass frost vignette
            val glowPaint = Paint().apply {
                isAntiAlias = true
                shader = RadialGradient(
                    innerRect.left + innerRect.width() * 0.8f, innerRect.top + innerRect.height() * 0.2f, innerRect.width() * 0.6f,
                    intArrayOf(
                        AndroidColor.parseColor("#4000E5FF"),
                        AndroidColor.TRANSPARENT
                    ),
                    null, Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(innerRect, glowPaint)
        }
    } else {
        // Empty state gradient placeholder
        val placeholderColors = intArrayOf(
            AndroidColor.parseColor("#00B0FF"),
            AndroidColor.parseColor("#D500F9"),
            AndroidColor.parseColor("#3D5AFE")
        )
        val sh = LinearGradient(
            innerRect.left, innerRect.top, innerRect.right, innerRect.bottom,
            placeholderColors, null, Shader.TileMode.CLAMP
        )
        val placeholderPaint = Paint().apply {
            isAntiAlias = true
            shader = sh
        }
        canvas.drawRect(innerRect, placeholderPaint)

        // Draw basic centered "Select" icon representation
        val promptPaint = Paint().apply {
            isAntiAlias = true
            color = AndroidColor.WHITE
            textSize = deviceWidth * 0.05f
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        canvas.drawText("ADD SCREENSHOT", innerRect.centerX(), innerRect.centerY(), promptPaint)
    }

    if (showDisplayGlassBlur) {
        val glassPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            val colorVal = displayGlassBlurColor.toArgb()
            val colors = intArrayOf(
                (colorVal and 0x00FFFFFF) or ((displayGlassBlurOpacity * 255).toInt() shl 24),
                (colorVal and 0x00FFFFFF) or ((displayGlassBlurOpacity * 255 * 0.3f).toInt() shl 24),
                AndroidColor.TRANSPARENT
            )
            val shader = RadialGradient(
                innerRect.centerX(), innerRect.centerY(), innerRect.width() * 0.8f,
                colors, null, Shader.TileMode.CLAMP
            )
            this.shader = shader
        }
        canvas.drawRect(innerRect, glassPaint)

        // Shimmering reflection line
        val linePaint = Paint().apply {
            isAntiAlias = true
            color = AndroidColor.WHITE
            alpha = (255 * 0.15f * displayGlassBlurOpacity).toInt()
            strokeWidth = innerRect.width() * 0.12f
        }
        canvas.drawLine(
            innerRect.left, innerRect.top,
            innerRect.right, innerRect.bottom,
            linePaint
        )
    }

    // Screen fold crease (for foldables)
    if (hasFoldLine) {
        val linePaintDark = Paint().apply {
            isAntiAlias = true
            color = AndroidColor.argb((255 * 0.15f).toInt(), 0, 0, 0)
            strokeWidth = (deviceWidth / 280f) * 1.5f
        }
        val linePaintLight = Paint().apply {
            isAntiAlias = true
            color = AndroidColor.argb((255 * 0.08f).toInt(), 255, 255, 255)
            strokeWidth = (deviceWidth / 280f) * 1.5f
        }
        canvas.drawLine(innerRect.centerX(), innerRect.top, innerRect.centerX(), innerRect.bottom, linePaintDark)
        canvas.drawLine(innerRect.centerX() + 1f, innerRect.top, innerRect.centerX() + 1f, innerRect.bottom, linePaintLight)
    }

    if (showGlossyReflection) {
        val glossPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            val glossColors = intArrayOf(
                AndroidColor.argb((255 * glossyReflectionOpacity * 1.5f).toInt().coerceIn(0, 255), 255, 255, 255),
                AndroidColor.argb((255 * glossyReflectionOpacity * 0.7f).toInt().coerceIn(0, 255), 255, 255, 255),
                AndroidColor.TRANSPARENT,
                AndroidColor.argb((255 * glossyReflectionOpacity * 0.25f).toInt().coerceIn(0, 255), 128, 230, 250), // subtle cyan-blue sky glare
                AndroidColor.argb((255 * glossyReflectionOpacity * 0.9f).toInt().coerceIn(0, 255), 255, 255, 255),
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT
            )
            val positions = floatArrayOf(0.0f, 0.22f, 0.35f, 0.40f, 0.45f, 0.52f, 1.0f)
            shader = LinearGradient(
                innerRect.left, innerRect.top,
                innerRect.right, innerRect.bottom,
                glossColors, positions, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(innerRect, glossPaint)
    }

    // Render Hardware Accent features (Punch Hole, Notch)
    canvas.restore() // Restore Clip screen area

    val hardwarePaint = Paint().apply {
        isAntiAlias = true
        color = AndroidColor.BLACK
        style = Paint.Style.FILL
    }

    when (activeTemplate) {
        MockupTemplate.PIXEL_MODERN -> {
            // Small Punch hole
            canvas.drawCircle(deviceCenterX, innerRect.top + bThicknessPx * 1.5f, deviceWidth * 0.025f, hardwarePaint)
        }
        MockupTemplate.IPHONE_NOTCH -> {
            // Rounded Notch
            val notchWidth = deviceWidth * 0.42f
            val notchHeight = deviceWidth * 0.07f
            val notchRect = RectF(
                deviceCenterX - notchWidth / 2f,
                innerRect.top,
                deviceCenterX + notchWidth / 2f,
                innerRect.top + notchHeight
            )
            val rxNotch = deviceWidth * 0.025f
            canvas.drawRoundRect(notchRect, rxNotch, rxNotch, hardwarePaint)
        }
        MockupTemplate.DYNAMIC_ISLAND -> {
            // Pill Shape Island
            val pillWidth = deviceWidth * 0.35f
            val pillHeight = deviceWidth * 0.06f
            val pillTop = innerRect.top + bThicknessPx * 1.2f
            val pillRect = RectF(
                deviceCenterX - pillWidth / 2f,
                pillTop,
                deviceCenterX + pillWidth / 2f,
                pillTop + pillHeight
            )
            val rxPill = pillHeight / 2f
            canvas.drawRoundRect(pillRect, rxPill, rxPill, hardwarePaint)
        }
        MockupTemplate.MINIMAL_BORDER -> {
            // No hardware overlay
        }
    }

    // Status bar placeholders drawing
    if (showStatusBarIcons) {
        val statusTextPaint = Paint().apply {
            isAntiAlias = true
            color = AndroidColor.argb(220, 255, 255, 255)
            textSize = deviceWidth * 0.035f
        }
        canvas.drawText("09:41", innerRect.left + bThicknessPx * 1.5f, innerRect.top + bThicknessPx * 2.5f, statusTextPaint)
    }

    canvas.restore() // Restore Cameraconcat Matrix state

    // 4. WATERMARK TEXT EXPORT RENDER
    if (showWatermark && watermarkText.isNotBlank()) {
        val wmPaint = Paint().apply {
            isAntiAlias = true
            color = watermarkColor.toArgb()
            alpha = (255 * watermarkOpacity).toInt()
            textSize = watermarkSize * (width / 400f) // Scale size appropriately to canvas size
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }

        val padding = width * 0.05f
        val bounds = android.graphics.Rect()
        wmPaint.getTextBounds(watermarkText, 0, watermarkText.length, bounds)
        
        val wx: Float
        val wy: Float

        when (watermarkPosition) {
            WatermarkPosition.TOP_LEFT -> {
                wmPaint.textAlign = Paint.Align.LEFT
                wx = padding
                wy = padding + bounds.height()
            }
            WatermarkPosition.TOP_RIGHT -> {
                wmPaint.textAlign = Paint.Align.RIGHT
                wx = width - padding
                wy = padding + bounds.height()
            }
            WatermarkPosition.BOTTOM_LEFT -> {
                wmPaint.textAlign = Paint.Align.LEFT
                wx = padding
                wy = height - padding
            }
            WatermarkPosition.BOTTOM_CENTER -> {
                wmPaint.textAlign = Paint.Align.CENTER
                wx = width / 2f
                wy = height - padding
            }
            WatermarkPosition.BOTTOM_RIGHT -> {
                wmPaint.textAlign = Paint.Align.RIGHT
                wx = width - padding
                wy = height - padding
            }
        }

        // Draw small glass background behind watermark
        val wmBgPaint = Paint().apply {
            isAntiAlias = true
            color = AndroidColor.BLACK
            alpha = (255 * 0.4f * watermarkOpacity).toInt()
        }
        val bgRectWidth = bounds.width() * 1.25f
        val bgRectHeight = bounds.height() * 1.6f
        val bx: Float = when (wmPaint.textAlign) {
            Paint.Align.LEFT -> wx - bounds.width() * 0.12f
            Paint.Align.RIGHT -> wx - bgRectWidth + bounds.width() * 0.12f
            else -> wx - bgRectWidth / 2f
        }
        val by = wy - bounds.height() * 1.3f
        canvas.drawRoundRect(bx, by, bx + bgRectWidth, by + bgRectHeight, 12f, 12f, wmBgPaint)

        canvas.drawText(watermarkText.uppercase(), wx, wy, wmPaint)
    }

    bitmap
}

// ==========================================
// EXPORT DISPATCHERS & MEDIASTORE FLOW
// ==========================================

fun saveMockupImage(
    context: Context,
    aspectRatio: CanvasRatio,
    backgroundType: BackgroundType,
    solidColor: Color,
    gradient: CustomGradient,
    imageUri: Uri?,
    deviceFrameAspectRatio: Float = 9f / 19.5f,
    ambientBlurRadius: Float,
    screenshotScale: Float,
    screenshotOffsetX: Float,
    screenshotOffsetY: Float,
    activeTemplate: MockupTemplate,
    bezelColor: Color,
    bezelThickness: Float,
    screenCornerRadius: Float,
    showStatusBarIcons: Boolean,
    tiltX: Float,
    tiltY: Float,
    tiltZ: Float,
    perspectiveDepth: Float,
    shadowStrength: Float,
    showWatermark: Boolean,
    watermarkText: String,
    watermarkPosition: WatermarkPosition,
    watermarkColor: Color,
    watermarkSize: Float,
    watermarkOpacity: Float,
    coroutineScope: CoroutineScope,
    liquidThemeIndex: Int = 0,
    liquidScale: Float = 1.2f,
    showDisplayGlassBlur: Boolean = false,
    displayGlassBlurColor: Color = Color(0xFF00E5FF),
    displayGlassBlurOpacity: Float = 0.4f,
    patternType: PatternType = PatternConfig.selectedPatternType,
    patternPrimaryColor: Color = PatternConfig.patternPrimaryColor,
    patternSecondaryColor: Color = PatternConfig.patternSecondaryColor,
    patternBgColor: Color = PatternConfig.patternBgColor,
    patternScale: Float = PatternConfig.patternScale,
    deviceFrameScale: Float = 0.72f,
    shadowEnabled: Boolean = true,
    shadowBlurRadius: Float = 25f,
    shadowOffsetX: Float = 10f,
    shadowOffsetY: Float = 15f,
    shadowColor: Color = Color.Black,
    backgroundUri: Uri? = null,
    backgroundBlurRadius: Float = 0f,
    showGlossyReflection: Boolean = true,
    glossyReflectionOpacity: Float = 0.18f,
    screenshotGrayscale: Float = 0f,
    screenshotSepia: Float = 0f,
    screenshotBrightness: Float = 1f,
    screenshotContrast: Float = 1f,
    screenshotLiquidGlass: Boolean = false,
    isDesktop: Boolean = false,
    buttonStyle: String = "android",
    hasFoldLine: Boolean = false,
    exportFormat: String = "PNG",
    exportQuality: Float = 90f,
    onComplete: () -> Unit
) {
    coroutineScope.launch {
        try {
            val highResBitmap = renderHighResMockup(
                context, aspectRatio, backgroundType, solidColor, gradient, imageUri,
                deviceFrameAspectRatio,
                ambientBlurRadius, screenshotScale, screenshotOffsetX, screenshotOffsetY,
                activeTemplate, bezelColor, bezelThickness, screenCornerRadius, showStatusBarIcons,
                tiltX, tiltY, tiltZ, perspectiveDepth, shadowStrength, showWatermark,
                watermarkText, watermarkPosition, watermarkColor, watermarkSize, watermarkOpacity,
                liquidThemeIndex, liquidScale, showDisplayGlassBlur, displayGlassBlurColor, displayGlassBlurOpacity,
                deviceFrameScale = deviceFrameScale,
                shadowEnabled = shadowEnabled,
                shadowBlurRadius = shadowBlurRadius,
                shadowOffsetX = shadowOffsetX,
                shadowOffsetY = shadowOffsetY,
                shadowColor = shadowColor,
                backgroundUri = backgroundUri,
                backgroundBlurRadius = backgroundBlurRadius,
                showGlossyReflection = showGlossyReflection,
                glossyReflectionOpacity = glossyReflectionOpacity,
                screenshotGrayscale = screenshotGrayscale,
                screenshotSepia = screenshotSepia,
                screenshotBrightness = screenshotBrightness,
                screenshotContrast = screenshotContrast,
                screenshotLiquidGlass = screenshotLiquidGlass,
                isDesktop = isDesktop,
                buttonStyle = buttonStyle,
                hasFoldLine = hasFoldLine
            )

            val extension = when (exportFormat.uppercase()) {
                "JPEG" -> "jpg"
                "WEBP" -> "webp"
                else -> "png"
            }
            val mimeType = when (exportFormat.uppercase()) {
                "JPEG" -> "image/jpeg"
                "WEBP" -> "image/webp"
                else -> "image/png"
            }
            val compressFormat = when (exportFormat.uppercase()) {
                "JPEG" -> Bitmap.CompressFormat.JPEG
                "WEBP" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (exportQuality < 100f) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP_LOSSLESS
                    } else {
                        @Suppress("DEPRECATION")
                        Bitmap.CompressFormat.WEBP
                    }
                }
                else -> Bitmap.CompressFormat.PNG
            }

            // Save to Public MediaStore
            val filename = "HiShoot_${System.currentTimeMillis()}.$extension"
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/HiShootStudio")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val imageUriResult = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (imageUriResult != null) {
                resolver.openOutputStream(imageUriResult)?.use { outputStream ->
                    highResBitmap.compress(compressFormat, exportQuality.toInt(), outputStream)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(imageUriResult, contentValues, null, null)
                }

                Toast.makeText(context, "Mockup saved successfully to Gallery!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Failed to save image metadata.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            onComplete()
        }
    }
}

fun shareMockupImage(
    context: Context,
    aspectRatio: CanvasRatio,
    backgroundType: BackgroundType,
    solidColor: Color,
    gradient: CustomGradient,
    imageUri: Uri?,
    deviceFrameAspectRatio: Float = 9f / 19.5f,
    ambientBlurRadius: Float,
    screenshotScale: Float,
    screenshotOffsetX: Float,
    screenshotOffsetY: Float,
    activeTemplate: MockupTemplate,
    bezelColor: Color,
    bezelThickness: Float,
    screenCornerRadius: Float,
    showStatusBarIcons: Boolean,
    tiltX: Float,
    tiltY: Float,
    tiltZ: Float,
    perspectiveDepth: Float,
    shadowStrength: Float,
    showWatermark: Boolean,
    watermarkText: String,
    watermarkPosition: WatermarkPosition,
    watermarkColor: Color,
    watermarkSize: Float,
    watermarkOpacity: Float,
    coroutineScope: CoroutineScope,
    liquidThemeIndex: Int = 0,
    liquidScale: Float = 1.2f,
    showDisplayGlassBlur: Boolean = false,
    displayGlassBlurColor: Color = Color(0xFF00E5FF),
    displayGlassBlurOpacity: Float = 0.4f,
    patternType: PatternType = PatternConfig.selectedPatternType,
    patternPrimaryColor: Color = PatternConfig.patternPrimaryColor,
    patternSecondaryColor: Color = PatternConfig.patternSecondaryColor,
    patternBgColor: Color = PatternConfig.patternBgColor,
    patternScale: Float = PatternConfig.patternScale,
    deviceFrameScale: Float = 0.72f,
    shadowEnabled: Boolean = true,
    shadowBlurRadius: Float = 25f,
    shadowOffsetX: Float = 10f,
    shadowOffsetY: Float = 15f,
    shadowColor: Color = Color.Black,
    backgroundUri: Uri? = null,
    backgroundBlurRadius: Float = 0f,
    showGlossyReflection: Boolean = true,
    glossyReflectionOpacity: Float = 0.18f,
    screenshotGrayscale: Float = 0f,
    screenshotSepia: Float = 0f,
    screenshotBrightness: Float = 1f,
    screenshotContrast: Float = 1f,
    screenshotLiquidGlass: Boolean = false,
    isDesktop: Boolean = false,
    buttonStyle: String = "android",
    hasFoldLine: Boolean = false,
    exportFormat: String = "PNG",
    exportQuality: Float = 90f,
    onComplete: () -> Unit
) {
    coroutineScope.launch {
        try {
            val highResBitmap = renderHighResMockup(
                context, aspectRatio, backgroundType, solidColor, gradient, imageUri,
                deviceFrameAspectRatio,
                ambientBlurRadius, screenshotScale, screenshotOffsetX, screenshotOffsetY,
                activeTemplate, bezelColor, bezelThickness, screenCornerRadius, showStatusBarIcons,
                tiltX, tiltY, tiltZ, perspectiveDepth, shadowStrength, showWatermark,
                watermarkText, watermarkPosition, watermarkColor, watermarkSize, watermarkOpacity,
                liquidThemeIndex, liquidScale, showDisplayGlassBlur, displayGlassBlurColor, displayGlassBlurOpacity,
                patternType = patternType,
                patternPrimaryColor = patternPrimaryColor,
                patternSecondaryColor = patternSecondaryColor,
                patternBgColor = patternBgColor,
                patternScale = patternScale,
                deviceFrameScale = deviceFrameScale,
                shadowEnabled = shadowEnabled,
                shadowBlurRadius = shadowBlurRadius,
                shadowOffsetX = shadowOffsetX,
                shadowOffsetY = shadowOffsetY,
                shadowColor = shadowColor,
                backgroundUri = backgroundUri,
                backgroundBlurRadius = backgroundBlurRadius,
                showGlossyReflection = showGlossyReflection,
                glossyReflectionOpacity = glossyReflectionOpacity,
                screenshotGrayscale = screenshotGrayscale,
                screenshotSepia = screenshotSepia,
                screenshotBrightness = screenshotBrightness,
                screenshotContrast = screenshotContrast,
                screenshotLiquidGlass = screenshotLiquidGlass,
                isDesktop = isDesktop,
                buttonStyle = buttonStyle,
                hasFoldLine = hasFoldLine
            )

            val extension = when (exportFormat.uppercase()) {
                "JPEG" -> "jpg"
                "WEBP" -> "webp"
                else -> "png"
            }
            val compressFormat = when (exportFormat.uppercase()) {
                "JPEG" -> Bitmap.CompressFormat.JPEG
                "WEBP" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (exportQuality < 100f) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP_LOSSLESS
                    } else {
                        @Suppress("DEPRECATION")
                        Bitmap.CompressFormat.WEBP
                    }
                }
                else -> Bitmap.CompressFormat.PNG
            }

            // Cache image locally inside app directory and share via FileProvider
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val shareFile = File(cachePath, "hishoot_share.$extension")
            
            withContext(Dispatchers.IO) {
                FileOutputStream(shareFile).use { out ->
                    highResBitmap.compress(compressFormat, exportQuality.toInt(), out)
                    out.flush()
                }
            }

            val fileUri = FileProvider.getUriForFile(
                context,
                "com.aistudio.hishootstudio.pwyksd.fileprovider",
                shareFile
            )

            if (fileUri != null) {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    setDataAndType(fileUri, context.contentResolver.getType(fileUri))
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share HiShoot Mockup"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error preparing share: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            onComplete()
        }
    }
}

@Composable
fun LiquidFlowBackground(
    themeIndex: Int,
    noiseEnabled: Boolean,
    scale: Float,
    modifier: Modifier = Modifier
) {
    val liquidThemes = listOf(
        // Theme 0: Neon Aura (Cyan, Pink, Purple, Magenta)
        listOf(Color(0xFF00E5FF), Color(0xFFF355DA), Color(0xFF7000FF), Color(0xFFFF007F)),
        // Theme 1: Sunset Fusion (Orange, Coral, Yellow, Sunset Purple)
        listOf(Color(0xFFFF5E62), Color(0xFFFF9966), Color(0xFFFFD200), Color(0xFF8A2387)),
        // Theme 2: Cyber Ocean (Aqua Teal, Electric Blue, Green, Deep Blue)
        listOf(Color(0xFF00F0FF), Color(0xFF0072FF), Color(0xFF00FF87), Color(0xFF11998E)),
        // Theme 3: Cosmic Orchid (Plum Red, Sunset Coral, Deep Gold, Indigo)
        listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121), Color(0xFF3F2B96)),
        // Theme 4: Emerald Aurora (Deep Mint, Bright Green, Soft Turquoise, Rich Teal)
        listOf(Color(0xFF11998E), Color(0xFF38EF7D), Color(0xFF00F2FE), Color(0xFF4FACFE))
    )
    val colors = liquidThemes.getOrElse(themeIndex) { liquidThemes[0] }

    val infiniteTransition = rememberInfiniteTransition(label = "LiquidFlow")
    
    val angle1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle1"
    )
    val angle2 by infiniteTransition.animateFloat(
        initialValue = 180f,
        targetValue = 540f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle2"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(Color(0xFF070B19))
            .drawBehind {
                val width = size.width
                val height = size.height
                val radiusBase = Math.min(width, height) * 0.75f * scale
                
                val rad1 = Math.toRadians(angle1.toDouble())
                val rad2 = Math.toRadians(angle2.toDouble())
                
                val x1 = (width / 2f) + (width * 0.22f * Math.cos(rad1)).toFloat()
                val y1 = (height / 2f) + (height * 0.18f * Math.sin(rad1)).toFloat()
                
                val x2 = (width / 2f) + (width * 0.2f * Math.cos(rad2)).toFloat()
                val y2 = (height / 2f) + (height * 0.24f * Math.sin(rad2)).toFloat()
                
                val x3 = (width / 2f) - (width * 0.18f * Math.sin(rad1)).toFloat()
                val y3 = (height / 2f) + (height * 0.2f * Math.cos(rad1)).toFloat()

                val x4 = (width / 2f) + (width * 0.12f * Math.sin(rad2)).toFloat()
                val y4 = (height / 2f) - (height * 0.22f * Math.cos(rad2)).toFloat()

                // Blob 1
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(colors[0].copy(alpha = 0.55f), colors[0].copy(alpha = 0.15f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(x1, y1),
                        radius = radiusBase * 1.1f
                    ),
                    center = androidx.compose.ui.geometry.Offset(x1, y1),
                    radius = radiusBase * 1.1f
                )

                // Draw Blob 2
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(colors[1].copy(alpha = 0.5f), colors[1].copy(alpha = 0.1f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(x2, y2),
                        radius = radiusBase * 0.9f
                    ),
                    center = androidx.compose.ui.geometry.Offset(x2, y2),
                    radius = radiusBase * 0.9f
                )

                // Draw Blob 3
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(colors[2].copy(alpha = 0.45f), colors[2].copy(alpha = 0.08f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(x3, y3),
                        radius = radiusBase * 1.2f
                    ),
                    center = androidx.compose.ui.geometry.Offset(x3, y3),
                    radius = radiusBase * 1.2f
                )

                // Draw Blob 4
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(colors[3].copy(alpha = 0.4f), colors[3].copy(alpha = 0.05f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(x4, y4),
                        radius = radiusBase * 0.85f
                    ),
                    center = androidx.compose.ui.geometry.Offset(x4, y4),
                    radius = radiusBase * 0.85f
                )
            }
    ) {
        if (noiseEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.03f),
                                Color.Black.copy(alpha = 0.12f)
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun CustomPatternBackground(
    patternType: PatternType,
    primaryColor: Color,
    secondaryColor: Color,
    bgColor: Color,
    scale: Float,
    modifier: Modifier = Modifier
) {
    ComposeCanvas(modifier = modifier.background(bgColor)) {
        val width = size.width
        val height = size.height
        
        when (patternType) {
            PatternType.DOTS -> {
                val step = 32f * scale * density
                val radius = 2f * scale * density
                var x = step / 2f
                while (x < width) {
                    var y = step / 2f
                    while (y < height) {
                        drawCircle(
                            color = primaryColor,
                            radius = radius,
                            center = androidx.compose.ui.geometry.Offset(x, y)
                        )
                        y += step
                    }
                    x += step
                }
            }
            PatternType.GRID -> {
                val step = 40f * scale * density
                var x = 0f
                while (x < width) {
                    drawLine(
                        color = primaryColor.copy(alpha = 0.2f),
                        start = androidx.compose.ui.geometry.Offset(x, 0f),
                        end = androidx.compose.ui.geometry.Offset(x, height),
                        strokeWidth = 1f * density
                    )
                    x += step
                }
                var y = 0f
                while (y < height) {
                    drawLine(
                        color = primaryColor.copy(alpha = 0.2f),
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(width, y),
                        strokeWidth = 1f * density
                    )
                    y += step
                }
            }
            PatternType.STRIPES -> {
                val step = 24f * scale * density
                var x = -height
                while (x < width) {
                    drawLine(
                        color = primaryColor.copy(alpha = 0.15f),
                        start = androidx.compose.ui.geometry.Offset(x, 0f),
                        end = androidx.compose.ui.geometry.Offset(x + height, height),
                        strokeWidth = 4f * scale * density
                    )
                    x += step
                }
            }
            PatternType.WAVES -> {
                val lineSpacing = 36f * scale * density
                val amplitude = 12f * scale * density
                val frequency = (0.015f / scale) / density
                
                var yOffset = -amplitude
                while (yOffset < height + amplitude) {
                    val path = androidx.compose.ui.graphics.Path()
                    path.moveTo(0f, yOffset)
                    var x = 0f
                    while (x <= width) {
                        val y = yOffset + amplitude * Math.sin((x * frequency).toDouble()).toFloat()
                        path.lineTo(x, y)
                        x += 10f * density
                    }
                    drawPath(
                        path = path,
                        color = primaryColor.copy(alpha = 0.25f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f * scale * density)
                    )
                    yOffset += lineSpacing
                }
            }
            PatternType.HEXAGON -> {
                val r = 24f * scale * density
                val h = r * Math.sin(Math.toRadians(30.0)).toFloat()
                val w = r * Math.cos(Math.toRadians(30.0)).toFloat()
                
                var row = 0
                var y = -r
                while (y < height + r) {
                    val xOffset = if (row % 2 == 1) w else 0f
                    var x = -w
                    while (x < width + w) {
                        val path = androidx.compose.ui.graphics.Path()
                        val cx = x + xOffset
                        val cy = y
                        path.moveTo(cx, cy - r)
                        path.lineTo(cx + w, cy - h)
                        path.lineTo(cx + w, cy + h)
                        path.lineTo(cx, cy + r)
                        path.lineTo(cx - w, cy + h)
                        path.lineTo(cx - w, cy - h)
                        path.close()
                        
                        drawPath(
                            path = path,
                            color = primaryColor.copy(alpha = 0.12f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f * density)
                        )
                        x += 2 * w
                    }
                    y += r + h
                    row++
                }
            }
            PatternType.STARS -> {
                val starCount = 60
                for (i in 0 until starCount) {
                    val xPct = Math.abs(Math.sin((i * 1234.56).toDouble())).toFloat()
                    val yPct = Math.abs(Math.cos((i * 6543.21).toDouble())).toFloat()
                    val starSize = (1.5f + Math.abs(Math.sin((i * 999.9).toDouble())).toFloat() * 2.5f) * scale * density
                    
                    val px = xPct * width
                    val py = yPct * height
                    
                    drawLine(
                        color = primaryColor.copy(alpha = 0.15f),
                        start = androidx.compose.ui.geometry.Offset(px - starSize * 2f, py),
                        end = androidx.compose.ui.geometry.Offset(px + starSize * 2f, py),
                        strokeWidth = 0.8f * density
                    )
                    drawLine(
                        color = primaryColor.copy(alpha = 0.15f),
                        start = androidx.compose.ui.geometry.Offset(px, py - starSize * 2f),
                        end = androidx.compose.ui.geometry.Offset(px, py + starSize * 2f),
                        strokeWidth = 0.8f * density
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.75f),
                        radius = starSize * 0.5f,
                        center = androidx.compose.ui.geometry.Offset(px, py)
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingStepRow(
    stepNumber: String,
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 14.sp
            )
        }
    }
}

