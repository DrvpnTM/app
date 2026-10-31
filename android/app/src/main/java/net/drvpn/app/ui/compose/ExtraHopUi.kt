package net.drvpn.app.ui.compose

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.drvpn.app.AppConfig
import net.drvpn.app.R
import net.drvpn.app.core.ExtraHopKind
import net.drvpn.app.core.ExtraHopMode
import net.drvpn.app.handler.MmkvManager
import net.drvpn.app.handler.SpeedtestManager

/** How one extra hop looks: its own name (never the provider's), icon and color. */
data class HopStyle(
    @StringRes val name: Int,
    @StringRes val description: Int,
    @DrawableRes val icon: Int,
    val color: Color,
)

fun hopStyle(kind: ExtraHopKind): HopStyle = when (kind) {
    ExtraHopKind.SHIELD -> HopStyle(R.string.hop_great_wall, R.string.hop_great_wall_desc, R.drawable.ic_hop_great_wall, Color(0xFFC62828))
    ExtraHopKind.PSIPHON -> HopStyle(R.string.hop_ps_pro, R.string.hop_ps_pro_desc, R.drawable.ic_hop_ps_pro, Color(0xFFF59E0B))
    ExtraHopKind.TOR -> HopStyle(R.string.hop_black_cdn, R.string.hop_black_cdn_desc, R.drawable.ic_hop_black_cdn, Color(0xFF1F2937))
    ExtraHopKind.TUNNEL -> HopStyle(R.string.hop_dr_tunnel, R.string.hop_dr_tunnel_desc, R.drawable.ic_hop_dr_tunnel, Color(0xFF2563EB))
    ExtraHopKind.LANTERN -> HopStyle(R.string.hop_silk_road, R.string.hop_silk_road_desc, R.drawable.ic_hop_silk_road, Color(0xFF0D9488))
    ExtraHopKind.EMERGENCY -> HopStyle(R.string.hop_emergency, R.string.hop_emergency_desc, R.drawable.ic_hop_emergency, Color(0xFFDB2777))
}

/** The hop's icon, white on a circle of its color. */
@Composable
fun HopIcon(kind: ExtraHopKind?, size: Dp = 40.dp) {
    val color = kind?.let { hopStyle(it).color } ?: MaterialTheme.colorScheme.outline
    val icon = kind?.let { hopStyle(it).icon } ?: R.drawable.ic_hop_off
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.55f))
    }
}

/**
 * The extra hop settings: where the hop sits (three chips) and which hop (a card each).
 * Silk Road always sits before the server; its card says so.
 */
@Composable
fun ExtraHopPicker(
    mode: String,
    kind: String,
    onModeChange: (String) -> Unit,
    onKindChange: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(
            stringResource(R.string.extra_hop_intro),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 10.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ModeChip(ExtraHopMode.OFF, R.string.extra_hop_mode_off, R.drawable.ic_hop_off, mode, onModeChange, Modifier.weight(1f))
            ModeChip(ExtraHopMode.BEFORE, R.string.extra_hop_mode_before, R.drawable.ic_hop_silk_road, mode, onModeChange, Modifier.weight(1f))
            ModeChip(ExtraHopMode.AFTER, R.string.extra_hop_mode_after, R.drawable.ic_hop_great_wall, mode, onModeChange, Modifier.weight(1f))
        }
        Text(
            stringResource(
                when (ExtraHopMode.from(mode)) {
                    ExtraHopMode.OFF -> R.string.extra_hop_mode_off_desc
                    ExtraHopMode.BEFORE -> R.string.extra_hop_mode_before_desc
                    ExtraHopMode.AFTER -> R.string.extra_hop_mode_after_desc
                }
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 12.dp),
        )
        val enabled = mode != ExtraHopMode.OFF.value
        ExtraHopKind.entries.forEach { k ->
            HopCard(k, selected = k.value == kind, enabled = enabled, onClick = { onKindChange(k.value) })
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ModeChip(
    value: ExtraHopMode,
    @StringRes label: Int,
    @DrawableRes icon: Int,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier,
) {
    val isSelected = value.value == selected
    val shape = RoundedCornerShape(14.dp)
    val bg by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        label = "modeChip",
    )
    val fg = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier
            .clip(shape)
            .background(bg)
            .clickable(role = Role.RadioButton) { onSelect(value.value) }
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun HopCard(kind: ExtraHopKind, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val style = hopStyle(kind)
    val shape = RoundedCornerShape(16.dp)
    val border by animateColorAsState(
        if (selected && enabled) style.color else MaterialTheme.colorScheme.outlineVariant,
        label = "hopCardBorder",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected && enabled) style.color.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface)
            .border(if (selected && enabled) 2.dp else 1.dp, border, shape)
            .clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HopIcon(kind, 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(style.name),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            )
            Text(
                stringResource(style.description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.5f),
            )
        }
        if (selected) {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (enabled) style.color else MaterialTheme.colorScheme.outline),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_action_done), contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

/** What the running connection uses, as the VPN process records it ([AppConfig.PREF_EXTRA_HOP_ACTIVE]). */
private data class ActiveHop(val kind: ExtraHopKind, val mode: ExtraHopMode)

private fun readActiveHop(): ActiveHop? {
    val raw = MmkvManager.decodeSettingsString(AppConfig.PREF_EXTRA_HOP_ACTIVE).orEmpty()
    val parts = raw.split('|')
    if (parts.size != 2) return null
    val kind = ExtraHopKind.entries.find { it.value == parts[0] } ?: return null
    return ActiveHop(kind, ExtraHopMode.from(parts[1]))
}

/** "104.28.194.9" as "104.*.*.9", as Hiddify shows it. */
private fun maskIp(ip: String): String {
    val v4 = ip.split('.')
    if (v4.size == 4) return "${v4[0]}.*.*.${v4[3]}"
    val v6 = ip.split(':').filter { it.isNotEmpty() }
    return if (v6.size >= 2) "${v6.first()}:*:${v6.last()}" else ip
}

/**
 * The top band of the server card on Home, as Hiddify shows its chain: the hop's icon and name,
 * whether it comes before the server or gives the final IP, and its state. Nothing when the
 * extra hop is off.
 */
@Composable
fun ExtraHopBand(isRunning: Boolean, refresh: Int = 0, onClick: () -> Unit = {}) {
    val chosenMode = remember(refresh) { ExtraHopMode.from(MmkvManager.decodeSettingsString(AppConfig.PREF_EXTRA_HOP_MODE)) }
    if (chosenMode == ExtraHopMode.OFF) {
        // A slim row, so the chain can be turned on from Home too.
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HopIcon(null, 24.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(R.string.extra_hop_off_row),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Icon(painterResource(R.drawable.ic_chevron_right_24dp), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
        return
    }
    val chosenKind = remember(refresh) { ExtraHopKind.from(MmkvManager.decodeSettingsString(AppConfig.PREF_EXTRA_HOP_KIND)) }

    var active by remember { mutableStateOf<ActiveHop?>(null) }
    var exitIp by remember { mutableStateOf<String?>(null) }
    // While connected the VPN process may change the hop (emergency) and the IP takes a moment.
    LaunchedEffect(isRunning, refresh) {
        exitIp = null
        while (isRunning) {
            val now = withContext(Dispatchers.IO) { readActiveHop() }
            if (now != active) {
                active = now
                exitIp = null
            }
            if (now != null && exitIp == null) {
                exitIp = withContext(Dispatchers.IO) { runCatching { SpeedtestManager.getRemoteIPInfo()?.ipAddress }.getOrNull() }
            }
            delay(if (exitIp == null) 3_000 else 10_000)
        }
        active = null
    }

    val shown = active
    val kind = shown?.kind ?: chosenKind
    val mode = shown?.mode ?: chosenMode
    val style = hopStyle(kind)
    val name = stringResource(style.name)
    val role = stringResource(if (mode == ExtraHopMode.AFTER) R.string.extra_hop_role_exit else R.string.extra_hop_role_entry)
    val emergency = chosenKind == ExtraHopKind.EMERGENCY

    Row(
        Modifier
            .fillMaxWidth()
            .background(style.color.copy(alpha = 0.10f))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HopIcon(kind, 30.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (emergency && shown != null) stringResource(R.string.extra_hop_emergency_using, name) else name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(role, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        when {
            !isRunning -> Unit
            exitIp != null -> {
                Text(
                    maskIp(exitIp!!),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF15803D),
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.width(6.dp))
                Box(Modifier.size(18.dp).clip(CircleShape).background(Color(0xFF16A34A)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_action_done), contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
            else -> {
                Text(
                    stringResource(R.string.extra_hop_connecting),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(6.dp))
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = style.color)
            }
        }
    }
}

/**
 * The chain settings as a sheet, opened from the band on Home. [onDone] tells whether anything
 * changed, so a running connection can be restarted with the new chain.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtraHopSheet(onDone: (changed: Boolean) -> Unit) {
    val startMode = remember { MmkvManager.decodeSettingsString(AppConfig.PREF_EXTRA_HOP_MODE) ?: ExtraHopMode.OFF.value }
    val startKind = remember { MmkvManager.decodeSettingsString(AppConfig.PREF_EXTRA_HOP_KIND) ?: ExtraHopKind.SHIELD.value }
    var mode by remember { mutableStateOf(startMode) }
    var kind by remember { mutableStateOf(startKind) }
    ModalBottomSheet(
        onDismissRequest = {
            MmkvManager.encodeSettings(AppConfig.PREF_EXTRA_HOP_MODE, mode)
            MmkvManager.encodeSettings(AppConfig.PREF_EXTRA_HOP_KIND, kind)
            onDone(mode != startMode || kind != startKind)
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Text(
                stringResource(R.string.title_extra_hop),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp),
            )
            ExtraHopPicker(mode = mode, kind = kind, onModeChange = { mode = it }, onKindChange = { kind = it })
        }
    }
}
