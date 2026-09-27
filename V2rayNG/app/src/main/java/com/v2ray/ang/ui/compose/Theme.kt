package com.v2ray.ang.ui.compose

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.MmkvManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private val LightColor = lightColorScheme(
    primary = Color(0xFF166CD4), // Dr VPN blue
    onPrimary = Color(0xFFFFFFFF), // White
    primaryContainer = Color(0xFFD6E4FA), // Pale blue
    onPrimaryContainer = Color(0xFF001B3D), // Navy
    secondary = Color(0xFFf97910), // Orange
    onSecondary = Color(0xFFFFFFFF), // White
    secondaryContainer = Color(0xFFFFE8D6), // Pale Orange
    onSecondaryContainer = Color(0xFF2B1700), // Dark Brown
    tertiary = Color(0xFF009966), // Green
    onTertiary = Color(0xFFFFFFFF), // White
    tertiaryContainer = Color(0xFFA0F2D0), // Light Green
    onTertiaryContainer = Color(0xFF00201A), // Dark Teal
    error = Color(0xFFBA1A1A), // Red
    errorContainer = Color(0xFFFFDAD6), // Light Red
    onError = Color(0xFFFFFFFF), // White
    onErrorContainer = Color(0xFF410002), // Dark Red
    background = Color(0xFFFFFFFF), // White
    onBackground = Color(0xFF1C1B1F), // Near Black
    surface = Color(0xFFFFFFFF), // White
    onSurface = Color(0xFF1C1B1F), // Near Black
    surfaceVariant = Color(0xFFE7E0EC), // Light Purple Gray
    onSurfaceVariant = Color(0xFF49454F), // Dark Gray
    outline = Color(0xFF79747E), // Medium Gray
    outlineVariant = Color(0xFFCAC4D0), // Light Gray
    inverseSurface = Color(0xFF313033), // Dark Gray
    inverseOnSurface = Color(0xFFF4EFF4), // Very Light Gray
    inversePrimary = Color(0xFFC0C0C0), // Silver Gray
    scrim = Color(0xFF000000), // Black
    surfaceTint = Color(0xFF166CD4), // Dr VPN blue
    surfaceContainerLowest = Color(0xFFFFFFFF), // White
    surfaceContainerLow = Color(0xFFF7F7F7), // Very Light Gray
    surfaceContainer = Color(0xFFF1F1F1), // Light Gray
    surfaceContainerHigh = Color(0xFFEBEBEB), // Light Gray
    surfaceContainerHighest = Color(0xFFE5E5E5), // Light Gray
)

private val DarkColor = darkColorScheme(
    primary = Color(0xFF9EC3FF), // Light blue
    onPrimary = Color(0xFF002F65), // Navy
    primaryContainer = Color(0xFF0B4A9A), // Deep blue
    onPrimaryContainer = Color(0xFFD6E4FA), // Pale blue
    secondary = Color(0xFFf97910), // Orange
    onSecondary = Color(0xFF4E2600), // Dark Brown
    secondaryContainer = Color(0xFF6F3800), // Brown
    onSecondaryContainer = Color(0xFFFFE8D6), // Pale Orange
    tertiary = Color(0xFF83D6B5), // Mint Green
    onTertiary = Color(0xFF00382E), // Dark Teal
    tertiaryContainer = Color(0xFF005143), // Teal
    onTertiaryContainer = Color(0xFFA0F2D0), // Light Green
    error = Color(0xFFFFB4AB), // Light Red
    errorContainer = Color(0xFF93000A), // Dark Red
    onError = Color(0xFF690005), // Deep Red
    onErrorContainer = Color(0xFFFFDAD6), // Light Red
    background = Color(0xFF1C1B1F), // Near Black
    onBackground = Color(0xFFE6E1E5), // Light Gray
    surface = Color(0xFF1C1B1F), // Near Black
    onSurface = Color(0xFFE6E1E5), // Light Gray
    surfaceVariant = Color(0xFF49454F), // Dark Gray
    onSurfaceVariant = Color(0xFFCAC4D0), // Light Gray
    outline = Color(0xFF938F99), // Grayish Purple
    outlineVariant = Color(0xFF49454F), // Dark Gray
    inverseSurface = Color(0xFFE6E1E5), // Light Gray
    inverseOnSurface = Color(0xFF1C1B1F), // Near Black
    inversePrimary = Color(0xFF000000), // Black
    scrim = Color(0xFF000000), // Black
    surfaceTint = Color(0xFF9EC3FF), // Light blue
    surfaceContainerLowest = Color(0xFF0F0F12), // Near Black
    surfaceContainerLow = Color(0xFF1A191D), // Dark Gray
    surfaceContainer = Color(0xFF1E1D21), // Dark Gray
    surfaceContainerHigh = Color(0xFF282729), // Dark Gray
    surfaceContainerHighest = Color(0xFF333234), // Dark Gray
)

// Semantic Colors
val colorPing = IosGreen
val colorPingRed = IosRed
val colorConfigType = IosOrange
val colorFabActive = IosGreen
val colorFabInactiveLight = Color(0xFF9C9C9C) // Gray
val colorFabInactiveDark = Color(0xFF646464) // Dark Gray
val dividerColorLight = Color(0xFFE0E0E0) // Light Gray
val dividerColorDark = Color(0xFF424242) // Dark Gray

// Toast Colors 85%
val toastNormalBgLight = Color(0xD9353A3E) // Dark Gray
val toastNormalBgDark = Color(0xD94A4F54) // Darker Gray
val toastSuccessBg = Color(0xD9388E3C) // Green
val toastErrorBg = Color(0xD9D50000) // Red
val toastInfoBg = Color(0xD93F51B5) // Indigo Blue
val toastIconCircleBg = Color(0x33FFFFFF) // Semi-transparent White
val toastTextColor = Color.White // White

object ThemeManager {
    private val _themeMode = MutableStateFlow(
        MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "0") ?: "0"
    )
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _dynamicColorEnabled = MutableStateFlow(
        MmkvManager.decodeSettingsBool(AppConfig.PREF_DYNAMIC_COLOR, false)
    )
    val dynamicColorEnabled: StateFlow<Boolean> = _dynamicColorEnabled.asStateFlow()

    private val _themeStyle = MutableStateFlow(
        MmkvManager.decodeSettingsString(AppConfig.PREF_DRVPN_THEME, "default") ?: "default"
    )
    val themeStyle: StateFlow<String> = _themeStyle.asStateFlow()

    fun setThemeStyle(style: String) {
        MmkvManager.encodeSettings(AppConfig.PREF_DRVPN_THEME, style)
        _themeStyle.value = style
    }

    fun setThemeMode(mode: String) {
        MmkvManager.encodeSettings(AppConfig.PREF_UI_MODE_NIGHT, mode)
        _themeMode.value = mode
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        MmkvManager.encodeSettings(AppConfig.PREF_DYNAMIC_COLOR, enabled)
        _dynamicColorEnabled.value = enabled
    }

    fun refresh() {
        _themeMode.value =
            MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "0") ?: "0"
        _dynamicColorEnabled.value =
            MmkvManager.decodeSettingsBool(AppConfig.PREF_DYNAMIC_COLOR, false)
    }
}

@Composable
fun resolveDarkTheme(): Boolean {
    val mode by ThemeManager.themeMode.collectAsState()
    return when (mode) {
        "1" -> false
        "2" -> true
        else -> isSystemInDarkTheme()
    }
}


// Dr VPN theme styles. Beyond the default blue, "neomorph" is a soft low-contrast palette and
// "glass" a cool translucent-looking one; they change colors app-wide (full soft-shadow / blur
// effects are approximated through the color scheme).
private val NeoLight = LightColor.copy(
    primary = Color(0xFF5B6B89),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9E0EC),
    onPrimaryContainer = Color(0xFF1B2536),
    background = Color(0xFFE6EBF2),
    onBackground = Color(0xFF2C3444),
    surface = Color(0xFFE6EBF2),
    onSurface = Color(0xFF2C3444),
    surfaceContainerLowest = Color(0xFFDDE3EC),
    surfaceContainerLow = Color(0xFFE2E8F0),
    surfaceContainer = Color(0xFFEAEFF6),
    surfaceContainerHigh = Color(0xFFF0F4FA),
    surfaceContainerHighest = Color(0xFFF6F9FD),
    surfaceTint = Color(0xFF5B6B89),
)
private val NeoDark = DarkColor.copy(
    primary = Color(0xFF9DB2D4),
    onPrimary = Color(0xFF1B2536),
    primaryContainer = Color(0xFF33405A),
    onPrimaryContainer = Color(0xFFD9E0EC),
    background = Color(0xFF262A31),
    onBackground = Color(0xFFDCE1EA),
    surface = Color(0xFF262A31),
    onSurface = Color(0xFFDCE1EA),
    surfaceContainerLowest = Color(0xFF20242A),
    surfaceContainerLow = Color(0xFF2A2F37),
    surfaceContainer = Color(0xFF2F343D),
    surfaceContainerHigh = Color(0xFF373D47),
    surfaceContainerHighest = Color(0xFF414852),
    surfaceTint = Color(0xFF9DB2D4),
)
private val GlassLight = LightColor.copy(
    primary = Color(0xFF2E7BE0),
    primaryContainer = Color(0xFFCFE3FB),
    onPrimaryContainer = Color(0xFF07284D),
    background = Color(0xFFEAF2FC),
    onBackground = Color(0xFF14202E),
    surface = Color(0xFFF1F7FE),
    onSurface = Color(0xFF14202E),
    surfaceContainerLowest = Color(0xFFE4EEFA),
    surfaceContainerLow = Color(0xFFEAF2FC),
    surfaceContainer = Color(0xFFF0F6FE),
    surfaceContainerHigh = Color(0xFFF6FAFF),
    surfaceContainerHighest = Color(0xFFFBFDFF),
    surfaceTint = Color(0xFF2E7BE0),
)
private val GlassDark = DarkColor.copy(
    primary = Color(0xFF74B2FF),
    onPrimary = Color(0xFF00274F),
    primaryContainer = Color(0xFF0C3E75),
    onPrimaryContainer = Color(0xFFD3E6FF),
    background = Color(0xFF0D1826),
    onBackground = Color(0xFFDCE7F5),
    surface = Color(0xFF122238),
    onSurface = Color(0xFFDCE7F5),
    surfaceContainerLowest = Color(0xFF0B1523),
    surfaceContainerLow = Color(0xFF13233A),
    surfaceContainer = Color(0xFF182B45),
    surfaceContainerHigh = Color(0xFF1F3552),
    surfaceContainerHighest = Color(0xFF274060),
    surfaceTint = Color(0xFF74B2FF),
)

fun themeColorScheme(style: String, dark: Boolean) = when (style) {
    "neomorph" -> if (dark) NeoDark else NeoLight
    "glass" -> if (dark) GlassDark else GlassLight
    "material" -> if (dark) DarkColor else LightColor
    else -> if (dark) IosDark else IosLight
}

val LocalDarkTheme = compositionLocalOf { false }

@Composable
fun AppTheme(
    darkTheme: Boolean = resolveDarkTheme(),
    content: @Composable () -> Unit
) {
    val dynamicColor by ThemeManager.dynamicColorEnabled.collectAsState()
    val themeStyle by ThemeManager.themeStyle.collectAsState()
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        else -> themeColorScheme(themeStyle, darkTheme)
    }
    val snackbarController = rememberAppSnackbarController()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalAppSnackbar provides snackbarController
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = IosTypography,
            shapes = IosShapes
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AppSnackbarBridge(controller = snackbarController)
                content()
                AppSnackbarHost(hostState = snackbarController.hostState)
            }
        }
    }
}
