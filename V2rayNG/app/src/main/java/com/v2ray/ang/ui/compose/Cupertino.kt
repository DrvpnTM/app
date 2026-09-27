package com.v2ray.ang.ui.compose

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

internal val IosLight = lightColorScheme(
    primary = IosBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEBFF),
    onPrimaryContainer = Color(0xFF002A5C),
    secondary = IosBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCEBFF),
    onSecondaryContainer = Color(0xFF002A5C),
    tertiary = IosGreen,
    onTertiary = Color.White,
    error = IosRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFE1DF),
    onErrorContainer = Color(0xFF5C0600),
    background = Color(0xFFF2F2F7),
    onBackground = Color.Black,
    surface = Color(0xFFF2F2F7),
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFE5E5EA),
    onSurfaceVariant = Color(0xFF8A8A8E),
    outline = Color(0xFFC6C6C8),
    outlineVariant = Color(0xFFD1D1D6),
    inverseSurface = Color(0xFF1C1C1E),
    inverseOnSurface = Color.White,
    inversePrimary = IosBlueDark,
    scrim = Color.Black,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFE5E5EA),
)

internal val IosDark = darkColorScheme(
    primary = IosBlueDark,
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

/** iOS type ramp (SF metrics) mapped onto Material roles. */
internal val IosTypography = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontSize = 34.sp, lineHeight = 41.sp, fontWeight = FontWeight.Bold),
        headlineMedium = base.headlineMedium.copy(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontSize = 20.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.copy(fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.2).sp),
        bodyMedium = base.bodyMedium.copy(fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = (-0.1).sp),
        bodySmall = base.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
        labelLarge = base.labelLarge.copy(fontSize = 17.sp, fontWeight = FontWeight.Normal),
        labelMedium = base.labelMedium.copy(fontSize = 13.sp),
        labelSmall = base.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
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
