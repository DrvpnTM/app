package net.drvpn.app.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.composed
import androidx.compose.foundation.border
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.drvpn.app.R
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font

/*
 * Dr VPN "Cupertino" design language: the look of iOS / SwiftUI (grouped inset lists,
 * system colors, disclosure chevrons, green toggles) implemented with Compose.
 */

// iOS system colors
val IosBlue = Color(0xFF007AFF)
val IosBlueDark = Color(0xFF0A84FF)
val IosGreen = Color(0xFF34C759)
val IosRed = Color(0xFFFF3B30)
val IosOrange = Color(0xFFFF9500)
val IosIndigo = Color(0xFF5856D6)
val IosTeal = Color(0xFF30B0C7)
val IosPink = Color(0xFFFF2D55)
val IosGray = Color(0xFF8E8E93)

/** Dr VPN light palette ("B"): white pages, soft slate cards, one confident blue. */
internal val IosLight = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = Color(0xFF2563EB),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF1E3A8A),
    tertiary = Color(0xFF22C55E),
    onTertiary = Color.White,
    error = Color(0xFFEF4444),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    background = Color.White,
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFEEF2F7),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    inverseSurface = Color(0xFF0F172A),
    inverseOnSurface = Color.White,
    inversePrimary = Color(0xFF93C5FD),
    scrim = Color.Black,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Color(0xFFF5F7FB),
    surfaceContainerLow = Color(0xFFF5F7FB),
    surfaceContainer = Color(0xFFF5F7FB),
    surfaceContainerHigh = Color(0xFFF5F7FB),
    surfaceContainerHighest = Color(0xFFEEF2F7),
)

internal val IosDark = darkColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0A3A70),
    onPrimaryContainer = Color(0xFFD6E8FF),
    secondary = IosBlueDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF0A3A70),
    onSecondaryContainer = Color(0xFFD6E8FF),
    tertiary = Color(0xFF30D158),
    onTertiary = Color.Black,
    error = Color(0xFFFF453A),
    onError = Color.White,
    errorContainer = Color(0xFF5C0A06),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFF98989F),
    outline = Color(0xFF48484A),
    outlineVariant = Color(0xFF38383A),
    inverseSurface = Color(0xFFF2F2F7),
    inverseOnSurface = Color.Black,
    inversePrimary = IosBlue,
    scrim = Color.Black,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Color(0xFF1C1C1E),
    surfaceContainerLow = Color(0xFF1C1C1E),
    surfaceContainer = Color(0xFF1C1C1E),
    surfaceContainerHigh = Color(0xFF1C1C1E),
    surfaceContainerHighest = Color(0xFF2C2C2E),
)

/** Background of a grouped-list cell (white on light, #1C1C1E on dark). */
val ColorScheme.cell: Color get() = surfaceContainerLowest

/** Vazirmatn (SIL OFL) for Persian and Latin alike, bundled so every phone and TV looks the same. */
val DrVpnFont = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
    Font(R.font.vazirmatn_extrabold, FontWeight.ExtraBold),
)

/** Type ramp on Vazirmatn (sizes kept from the iOS ramp). */
internal val IosTypography = Typography().let { base ->
    val t = base.copy(
        displaySmall = base.displaySmall.copy(fontSize = 34.sp, lineHeight = 44.sp, fontWeight = FontWeight.ExtraBold),
        headlineMedium = base.headlineMedium.copy(fontSize = 28.sp, lineHeight = 38.sp, fontWeight = FontWeight.ExtraBold),
        headlineSmall = base.headlineSmall.copy(fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold),
        titleSmall = base.titleSmall.copy(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold),
        bodyLarge = base.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.sp),
        bodyMedium = base.bodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp, letterSpacing = 0.sp),
        bodySmall = base.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.sp, letterSpacing = 0.sp),
        labelLarge = base.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp),
        labelMedium = base.labelMedium.copy(fontSize = 13.sp, letterSpacing = 0.sp),
        labelSmall = base.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp),
    )
    fun TextStyle.v() = copy(fontFamily = DrVpnFont)
    t.copy(
        displayLarge = t.displayLarge.v(), displayMedium = t.displayMedium.v(), displaySmall = t.displaySmall.v(),
        headlineLarge = t.headlineLarge.v(), headlineMedium = t.headlineMedium.v(), headlineSmall = t.headlineSmall.v(),
        titleLarge = t.titleLarge.v(), titleMedium = t.titleMedium.v(), titleSmall = t.titleSmall.v(),
        bodyLarge = t.bodyLarge.v(), bodyMedium = t.bodyMedium.v(), bodySmall = t.bodySmall.v(),
        labelLarge = t.labelLarge.v(), labelMedium = t.labelMedium.v(), labelSmall = t.labelSmall.v(),
    )
}

internal val IosShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

val IosGroupShape = RoundedCornerShape(12.dp)

/** Section footnote / header caption style (13sp secondary label). */
val iosCaption: TextStyle
    @Composable get() = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)

/** Green iOS toggle. */
@Composable
fun iosSwitchColors(): SwitchColors {
    val dark = LocalDarkTheme.current
    return SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = if (dark) Color(0xFF30D158) else IosGreen,
        checkedBorderColor = Color.Transparent,
        uncheckedThumbColor = Color.White,
        uncheckedTrackColor = if (dark) Color(0xFF39393D) else Color(0xFFE9E9EA),
        uncheckedBorderColor = Color.Transparent,
        disabledCheckedTrackColor = IosGreen.copy(alpha = 0.4f),
        disabledUncheckedTrackColor = if (dark) Color(0xFF39393D) else Color(0xFFE9E9EA),
        disabledUncheckedThumbColor = Color.White.copy(alpha = 0.6f),
        disabledCheckedThumbColor = Color.White.copy(alpha = 0.6f),
        disabledUncheckedBorderColor = Color.Transparent,
        disabledCheckedBorderColor = Color.Transparent,
    )
}

/** True while composing rows inside a [SettingsGroup]; rows then skip their own card background. */
val LocalInSettingsGroup = staticCompositionLocalOf { false }

private class DividerOffsets {
    var values: IntArray = IntArray(0)
}

/**
 * iOS inset-grouped section: a rounded card holding rows, with hairline separators between them
 * (inset from the leading edge, like UITableView). Children are laid out top to bottom.
 */
@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    dividerInset: Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    val separator = MaterialTheme.colorScheme.outlineVariant
    val offsets = remember { DividerOffsets() }
    CompositionLocalProvider(LocalInSettingsGroup provides true) {
        Layout(
            content = content,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(IosGroupShape)
                .background(MaterialTheme.colorScheme.cell)
                .drawWithContent {
                    drawContent()
                    val inset = dividerInset.toPx()
                    val stroke = 0.5.dp.toPx().coerceAtLeast(1f)
                    offsets.values.forEach { y ->
                        drawLine(separator, Offset(inset, y.toFloat()), Offset(size.width, y.toFloat()), stroke)
                    }
                },
        ) { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints.copy(minHeight = 0)) }
            val height = placeables.sumOf { it.height }
            layout(constraints.maxWidth, height) {
                var y = 0
                val dividers = ArrayList<Int>()
                placeables.forEachIndexed { index, placeable ->
                    placeable.placeRelative(0, y)
                    y += placeable.height
                    if (index < placeables.lastIndex && placeable.height > 0) dividers += y
                }
                offsets.values = dividers.toIntArray()
            }
        }
    }
}

/** Standalone row card used when a settings row is not inside a [SettingsGroup]. */
fun Modifier.iosStandaloneCell(inGroup: Boolean, cell: Color): Modifier =
    if (inGroup) this else this
        .padding(horizontal = 16.dp, vertical = 3.dp)
        .clip(IosGroupShape)
        .background(cell)

/** iOS-Settings style leading icon: white glyph on a rounded colored square. */
@Composable
fun IosIconBadge(icon: Painter, tint: Color = MaterialTheme.colorScheme.primary, enabled: Boolean = true) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (enabled) tint else tint.copy(alpha = 0.4f)),
    ) {
        Icon(painter = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

/**
 * TV / remote-control support: draws a clear primary-colored ring around the element while it has
 * D-pad focus. Put it before the clickable/selectable modifier so it observes that element's focus.
 */
fun Modifier.focusHighlight(shape: androidx.compose.ui.graphics.Shape = IosGroupShape): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    val ring = MaterialTheme.colorScheme.primary
    this
        .onFocusChanged { focused = it.hasFocus }
        .then(if (focused) Modifier.border(3.dp, ring, shape) else Modifier)
}
