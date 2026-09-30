package net.drvpn.app.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.drvpn.app.R
import net.drvpn.app.handler.GeoIpLookup
import net.drvpn.app.ui.compose.IosOrange
import net.drvpn.app.ui.compose.colorPing
import net.drvpn.app.ui.compose.colorPingRed
import net.drvpn.app.ui.compose.focusHighlight

/** Title of a server row: the country name, or DRVPN when the location is unknown. */
@Composable
internal fun serverRowTitle(country: String): String {
    val locale = LocalConfiguration.current.locales[0]
    return when {
        country == GeoIpLookup.CDN -> "DRVPN · CDN"
        country.length == 2 -> java.util.Locale("", country).getDisplayCountry(locale).ifBlank { "DRVPN" }
        else -> "DRVPN"
    }
}

/**
 * Round flag: the flag emoji drawn large and clipped to a circle, so it fills the disc like a
 * round flag icon. Unknown / CDN locations show their symbol on a grey disc. [fast] adds a small
 * lightning badge.
 */
@Composable
internal fun FlagCircle(badge: String, isCountry: Boolean, size: Dp = 44.dp, fast: Boolean = false) {
    Box(Modifier.size(size + 4.dp)) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (isCountry) {
                Text(
                    badge,
                    fontSize = (size.value * 1.25f).sp,
                    modifier = Modifier.wrapContentSize(unbounded = true)
                )
            } else {
                Text(badge.ifEmpty { "🌐" }, fontSize = (size.value * 0.5f).sp)
            }
        }
        if (fast) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text("⚡", fontSize = 8.sp, color = Color.White)
            }
        }
    }
}

/** Four signal bars from the ping: green / orange / red, hollow bars for the missing strength. */
@Composable
internal fun SignalBars(delayMillis: Long, modifier: Modifier = Modifier) {
    val color = when {
        delayMillis <= 0L -> colorPingRed
        delayMillis < 400L -> colorPing
        delayMillis < 1000L -> IosOrange
        else -> colorPingRed
    }
    val strength = when {
        delayMillis <= 0L -> 0
        delayMillis < 150L -> 4
        delayMillis < 300L -> 3
        delayMillis < 700L -> 2
        else -> 1
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            for (i in 1..4) {
                Box(
                    Modifier
                        .width(3.dp)
                        .height((3 + i * 3).dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(if (i <= strength) color else MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
    }
}

@Composable
internal fun ServerSectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 6.dp)
    )
}

/**
 * One server: round flag, country name, protocol underneath, signal bars (and ping) at the end.
 * Selecting and the optional "more" menu are separate focus targets for TV remotes.
 */
@Composable
internal fun ServerLocationRow(
    row: ServerRowUiModel,
    isSelected: Boolean,
    fast: Boolean,
    onClick: () -> Unit,
    onMore: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val selectedLabel = if (isSelected) stringResource(R.string.acc_selected_server) else null
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(shape)
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else Color.Transparent)
            .semantics { if (selectedLabel != null) stateDescription = selectedLabel },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .focusHighlight(shape)
                .clickable(onClick = onClick)
                .padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isCountry = row.country.length == 2
            FlagCircle(row.flag, isCountry, fast = fast)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    serverRowTitle(row.country),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    row.typeDescription.replace(" / ", " · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val pingText = when {
                row.testDelayMillis > 0L -> "${row.testDelayMillis}ms"
                row.testDelayMillis < 0L -> "✕"
                else -> ""
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clearAndSetSemantics {
                    if (pingText.isNotEmpty()) contentDescription = pingText
                }
            ) {
                SignalBars(row.testDelayMillis)
                if (pingText.isNotEmpty()) {
                    Text(
                        pingText,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.offset(y = 2.dp)
                    )
                }
            }
        }
        if (onMore != null) {
            IconButton(onClick = onMore, modifier = Modifier.size(36.dp).focusHighlight(CircleShape)) {
                Icon(
                    painterResource(R.drawable.ic_more_vert_24dp),
                    stringResource(R.string.acc_more),
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
