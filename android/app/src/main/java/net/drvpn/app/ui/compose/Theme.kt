package net.drvpn.app.ui.compose

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import net.drvpn.app.AppConfig
import net.drvpn.app.handler.MmkvManager
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


// Dr VPN theme styles. "Neomorph" uses layered surfaces with clear text contrast, while "glass"
// uses cool translucent-looking colors (full soft-shadow / blur
// effects are approximated through the color scheme).
private val NeoLight = LightColor.copy(
    primary = Color(0xFF5B6B89),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9E0EC),
    onPrimaryContainer = Color(0xFF1B2536),
    background = Color(0xFFE4EAF2),
    onBackground = Color(0xFF1D2736),
    surface = Color(0xFFF8FAFD),
    onSurface = Color(0xFF1D2736),
    onSurfaceVariant = Color(0xFF4A586D),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8FAFD),
    surfaceContainer = Color(0xFFEEF2F7),
    surfaceContainerHigh = Color(0xFFE8EDF4),
    surfaceContainerHighest = Color(0xFFDDE4EE),
    surfaceTint = Color(0xFF5B6B89),
)
private val NeoDark = DarkColor.copy(
    primary = Color(0xFF9DB2D4),
    onPrimary = Color(0xFF1B2536),
    primaryContainer = Color(0xFF33405A),
    onPrimaryContainer = Color(0xFFD9E0EC),
    background = Color(0xFF171D26),
    onBackground = Color(0xFFE8EDF5),
    surface = Color(0xFF202833),
    onSurface = Color(0xFFE8EDF5),
    onSurfaceVariant = Color(0xFFB5C0D0),
    surfaceContainerLowest = Color(0xFF242D39),
    surfaceContainerLow = Color(0xFF293340),
    surfaceContainer = Color(0xFF2E3947),
    surfaceContainerHigh = Color(0xFF364251),
    surfaceContainerHighest = Color(0xFF414F60),
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

private val OceanLight = IosLight.copy(
    primary = Color(0xFF087E8B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC9F0F1),
    onPrimaryContainer = Color(0xFF063E45),
    secondary = Color(0xFF2864B4),
    onSecondary = Color.White,
    tertiary = Color(0xFF6857B8),
    onTertiary = Color.White,
    background = Color(0xFFF1F8FA),
    onBackground = Color(0xFF142B32),
    surface = Color(0xFFFAFEFF),
    onSurface = Color(0xFF142B32),
    onSurfaceVariant = Color(0xFF4E6870),
    outline = Color(0xFF9AB8BE),
    outlineVariant = Color(0xFFD4E5E8),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7FCFD),
    surfaceContainer = Color(0xFFECF5F7),
    surfaceContainerHigh = Color(0xFFE2EFF1),
    surfaceContainerHighest = Color(0xFFD7E9EC),
    surfaceTint = Color(0xFF087E8B),
)
private val OceanDark = IosDark.copy(
    primary = Color(0xFF67D5DC),
    onPrimary = Color(0xFF00363C),
    primaryContainer = Color(0xFF07545D),
    onPrimaryContainer = Color(0xFFC9F0F1),
    secondary = Color(0xFF9BC3FF),
    onSecondary = Color(0xFF12345F),
    tertiary = Color(0xFFC3B5FF),
    onTertiary = Color(0xFF302366),
    background = Color(0xFF0C1C22),
    onBackground = Color(0xFFE0F1F3),
    surface = Color(0xFF10252C),
    onSurface = Color(0xFFE0F1F3),
    onSurfaceVariant = Color(0xFFB1C9CD),
    outline = Color(0xFF65848A),
    outlineVariant = Color(0xFF2C454B),
    surfaceContainerLowest = Color(0xFF132B32),
    surfaceContainerLow = Color(0xFF172F37),
    surfaceContainer = Color(0xFF1D3840),
    surfaceContainerHigh = Color(0xFF27434B),
    surfaceContainerHighest = Color(0xFF324F57),
    surfaceTint = Color(0xFF67D5DC),
)
private val ForestLight = IosLight.copy(
    primary = Color(0xFF34764D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1F0D8),
    onPrimaryContainer = Color(0xFF123D23),
    secondary = Color(0xFF80602E),
    onSecondary = Color.White,
    tertiary = Color(0xFF397E7A),
    onTertiary = Color.White,
    background = Color(0xFFF3F7F1),
    onBackground = Color(0xFF202D22),
    surface = Color(0xFFFCFEFA),
    onSurface = Color(0xFF202D22),
    onSurfaceVariant = Color(0xFF536657),
    outline = Color(0xFFA9B9A8),
    outlineVariant = Color(0xFFDCE6D9),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF9FCF6),
    surfaceContainer = Color(0xFFEEF4E9),
    surfaceContainerHigh = Color(0xFFE5EDDF),
    surfaceContainerHighest = Color(0xFFD9E5D2),
    surfaceTint = Color(0xFF34764D),
)
private val ForestDark = IosDark.copy(
    primary = Color(0xFF91D5A0),
    onPrimary = Color(0xFF123B22),
    primaryContainer = Color(0xFF285D3A),
    onPrimaryContainer = Color(0xFFD1F0D8),
    secondary = Color(0xFFE2C18D),
    onSecondary = Color(0xFF442E0B),
    tertiary = Color(0xFF8FD4CC),
    onTertiary = Color(0xFF003B37),
    background = Color(0xFF151E17),
    onBackground = Color(0xFFE3EDE1),
    surface = Color(0xFF1B271D),
    onSurface = Color(0xFFE3EDE1),
    onSurfaceVariant = Color(0xFFB7C7B5),
    outline = Color(0xFF708270),
    outlineVariant = Color(0xFF354537),
    surfaceContainerLowest = Color(0xFF202E22),
    surfaceContainerLow = Color(0xFF243327),
    surfaceContainer = Color(0xFF2A3A2D),
    surfaceContainerHigh = Color(0xFF344638),
    surfaceContainerHighest = Color(0xFF405344),
    surfaceTint = Color(0xFF91D5A0),
)
private val SunsetLight = IosLight.copy(
    primary = Color(0xFFB64F35),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCE),
    onPrimaryContainer = Color(0xFF42190E),
    secondary = Color(0xFF9A5A00),
    onSecondary = Color.White,
    tertiary = Color(0xFF7652A5),
    onTertiary = Color.White,
    background = Color(0xFFFFF6F0),
    onBackground = Color(0xFF35231E),
    surface = Color(0xFFFFFCF9),
    onSurface = Color(0xFF35231E),
    onSurfaceVariant = Color(0xFF765D54),
    outline = Color(0xFFC6A99D),
    outlineVariant = Color(0xFFF0DDD4),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFFAF6),
    surfaceContainer = Color(0xFFFFF0E7),
    surfaceContainerHigh = Color(0xFFF8E5DA),
    surfaceContainerHighest = Color(0xFFF0D8CA),
    surfaceTint = Color(0xFFB64F35),
)
private val SunsetDark = IosDark.copy(
    primary = Color(0xFFFFB59F),
    onPrimary = Color(0xFF5C1708),
    primaryContainer = Color(0xFF8F3521),
    onPrimaryContainer = Color(0xFFFFDBCE),
    secondary = Color(0xFFFFC16C),
    onSecondary = Color(0xFF4F2D00),
    tertiary = Color(0xFFD2B8FF),
    onTertiary = Color(0xFF40266A),
    background = Color(0xFF241714),
    onBackground = Color(0xFFF5E5DE),
    surface = Color(0xFF30201B),
    onSurface = Color(0xFFF5E5DE),
    onSurfaceVariant = Color(0xFFD4BDB3),
    outline = Color(0xFF92766B),
    outlineVariant = Color(0xFF4B342D),
    surfaceContainerLowest = Color(0xFF38251F),
    surfaceContainerLow = Color(0xFF3D2923),
    surfaceContainer = Color(0xFF47312A),
    surfaceContainerHigh = Color(0xFF543B33),
    surfaceContainerHighest = Color(0xFF62473E),
    surfaceTint = Color(0xFFFFB59F),
)
private val MidnightLight = LightColor.copy(
    primary = Color(0xFF334D72),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E5F8),
    onPrimaryContainer = Color(0xFF172B49),
    secondary = Color(0xFF4F647D),
    onSecondary = Color.White,
    tertiary = Color(0xFF3C7775),
    onTertiary = Color.White,
    background = Color(0xFFF1F4F8),
    onBackground = Color(0xFF202B38),
    surface = Color.White,
    onSurface = Color(0xFF202B38),
    onSurfaceVariant = Color(0xFF526274),
    outline = Color(0xFFAAB6C4),
    outlineVariant = Color(0xFFDCE3EB),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = Color(0xFFEEF2F6),
    surfaceContainerHigh = Color(0xFFE4EAF0),
    surfaceContainerHighest = Color(0xFFD8E1EA),
    surfaceTint = Color(0xFF334D72),
)
private val MidnightDark = DarkColor.copy(
    primary = Color(0xFFA9C7F4),
    onPrimary = Color(0xFF172D4C),
    primaryContainer = Color(0xFF344D70),
    onPrimaryContainer = Color(0xFFD9E5F8),
    secondary = Color(0xFFBECDE0),
    onSecondary = Color(0xFF2A3C53),
    tertiary = Color(0xFF9DD4D0),
    onTertiary = Color(0xFF153B39),
    background = Color(0xFF10151D),
    onBackground = Color(0xFFE3EAF3),
    surface = Color(0xFF171E28),
    onSurface = Color(0xFFE3EAF3),
    onSurfaceVariant = Color(0xFFBBC7D6),
    outline = Color(0xFF748194),
    outlineVariant = Color(0xFF323D4B),
    surfaceContainerLowest = Color(0xFF1B2430),
    surfaceContainerLow = Color(0xFF202A37),
    surfaceContainer = Color(0xFF263241),
    surfaceContainerHigh = Color(0xFF303D4D),
    surfaceContainerHighest = Color(0xFF3B495A),
    surfaceTint = Color(0xFFA9C7F4),
)

fun themeColorScheme(style: String, dark: Boolean) = when (style) {
    "neomorph" -> if (dark) NeoDark else NeoLight
    "glass" -> if (dark) GlassDark else GlassLight
    "ocean" -> if (dark) OceanDark else OceanLight
    "forest" -> if (dark) ForestDark else ForestLight
    "sunset" -> if (dark) SunsetDark else SunsetLight
    "midnight" -> if (dark) MidnightDark else MidnightLight
    "material" -> if (dark) DarkColor else LightColor
    else -> if (dark) IosDark else IosLight
}

fun themeShapes(style: String) = when (style) {
    "glass", "sunset" -> Shapes(
        extraSmall = RoundedCornerShape(10.dp),
        small = RoundedCornerShape(14.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(26.dp),
        extraLarge = RoundedCornerShape(32.dp),
    )
    "midnight" -> Shapes(
        extraSmall = RoundedCornerShape(2.dp),
        small = RoundedCornerShape(4.dp),
        medium = RoundedCornerShape(8.dp),
        large = RoundedCornerShape(12.dp),
        extraLarge = RoundedCornerShape(16.dp),
    )
    "ocean" -> Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(14.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(28.dp),
        extraLarge = RoundedCornerShape(32.dp),
    )
    "forest" -> Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(14.dp),
        large = RoundedCornerShape(18.dp),
        extraLarge = RoundedCornerShape(24.dp),
    )
    "neomorph" -> Shapes(
        extraSmall = RoundedCornerShape(6.dp),
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(20.dp),
        extraLarge = RoundedCornerShape(26.dp),
    )
    else -> IosShapes
}

fun themeTypography(style: String) = when (style) {
    "ocean" -> IosTypography.copy(
        headlineSmall = IosTypography.headlineSmall.copy(fontSize = 23.sp, lineHeight = 32.sp),
        titleLarge = IosTypography.titleLarge.copy(fontSize = 21.sp, lineHeight = 29.sp),
        titleMedium = IosTypography.titleMedium.copy(fontSize = 18.sp, lineHeight = 26.sp),
        bodyLarge = IosTypography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 25.sp),
    )
    "forest" -> IosTypography.copy(
        headlineSmall = IosTypography.headlineSmall.copy(fontSize = 21.sp, lineHeight = 28.sp),
        titleLarge = IosTypography.titleLarge.copy(fontSize = 19.sp, lineHeight = 26.sp),
        titleMedium = IosTypography.titleMedium.copy(fontSize = 16.sp, lineHeight = 23.sp),
        bodyMedium = IosTypography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 21.sp),
        labelLarge = IosTypography.labelLarge.copy(fontSize = 14.sp),
    )
    "sunset" -> IosTypography.copy(
        headlineMedium = IosTypography.headlineMedium.copy(fontSize = 30.sp, lineHeight = 40.sp),
        headlineSmall = IosTypography.headlineSmall.copy(fontSize = 23.sp, lineHeight = 32.sp),
        titleLarge = IosTypography.titleLarge.copy(fontSize = 22.sp, lineHeight = 30.sp),
        titleMedium = IosTypography.titleMedium.copy(fontSize = 19.sp, lineHeight = 27.sp),
        bodyLarge = IosTypography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 26.sp),
        bodyMedium = IosTypography.bodyMedium.copy(fontSize = 16.sp, lineHeight = 24.sp),
    )
    "midnight" -> IosTypography.copy(
        headlineSmall = IosTypography.headlineSmall.copy(fontSize = 20.sp, lineHeight = 27.sp),
        titleLarge = IosTypography.titleLarge.copy(fontSize = 18.sp, lineHeight = 24.sp),
        titleMedium = IosTypography.titleMedium.copy(fontSize = 15.sp, lineHeight = 21.sp),
        bodyLarge = IosTypography.bodyLarge.copy(fontSize = 14.sp, lineHeight = 20.sp),
        bodyMedium = IosTypography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 19.sp),
        labelLarge = IosTypography.labelLarge.copy(fontSize = 13.sp),
        labelSmall = IosTypography.labelSmall.copy(fontSize = 10.sp),
    )
    else -> IosTypography
}

val LocalDarkTheme = compositionLocalOf { false }
val LocalThemeStyle = compositionLocalOf { "default" }

@Composable
fun AppTheme(
    darkTheme: Boolean = resolveDarkTheme(),
    content: @Composable () -> Unit
) {
    val dynamicColor by ThemeManager.dynamicColorEnabled.collectAsState()
    val themeStyle by ThemeManager.themeStyle.collectAsState()
    val context = LocalContext.current
    val colorScheme = when {
        themeStyle == "default" && dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
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
        LocalThemeStyle provides themeStyle,
        LocalAppSnackbar provides snackbarController
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = themeTypography(themeStyle),
            shapes = themeShapes(themeStyle)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AppSnackbarBridge(controller = snackbarController)
                content()
                AppSnackbarHost(hostState = snackbarController.hostState)
            }
        }
    }
}
