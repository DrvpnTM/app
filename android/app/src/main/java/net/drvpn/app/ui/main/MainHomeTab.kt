package net.drvpn.app.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import net.drvpn.app.extension.toSpeedString
import net.drvpn.app.ui.compose.IosBlue
import net.drvpn.app.ui.compose.IosGreen
import net.drvpn.app.ui.compose.cell
import kotlinx.coroutines.delay
import java.util.Locale
import net.drvpn.app.R

private val ConnectedColor = Color(0xFF34C759)
private val DisconnectedColor = Color(0xFF8E8E93)

/**
 * Hiddify-style home: active profile card, a large connect button, live status (timer + speed)
 * and the selected server card. Switches to two columns in landscape.
 */
@Composable
fun MainHomeTab(
    profileName: String,
    selectedServerName: String,
    isRunning: Boolean,
    statusText: String,
    connectedSince: Long?,
    speedUp: Long,
    speedDown: Long,
    isTesting: Boolean,
    onAction: (MainAction) -> Unit,
    onOpenProxies: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight
        val connectBlock: @Composable () -> Unit = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ConnectButton(
                    isRunning = isRunning,
                    onToggle = { onAction(MainAction.ToggleService) },
                    size = if (landscape) 170.dp else 220.dp,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(if (isRunning) R.string.home_connected else R.string.home_tap_to_connect),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isRunning) ConnectedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (isRunning) {
                    Spacer(Modifier.height(6.dp))
                    ConnectionTimer(connectedSince)
                    Spacer(Modifier.height(12.dp))
                    SpeedRow(up = speedUp, down = speedDown)
                    if (statusText.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = { onAction(MainAction.TestCurrentServer) })
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
        if (landscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { connectBlock() }
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    ProfileCard(
                        profileName = profileName,
                        onUpdate = { onAction(MainAction.UpdateSubscriptions) },
                    )
                    Spacer(Modifier.height(12.dp))
                    ServerCard(serverName = selectedServerName, onClick = onOpenProxies)
                    Spacer(Modifier.height(10.dp))
                    FastestButton(busy = isTesting, onClick = { onAction(MainAction.SelectFastest) })
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ProfileCard(
                    profileName = profileName,
                    onUpdate = { onAction(MainAction.UpdateSubscriptions) },
                )
                Spacer(Modifier.height(36.dp))
                connectBlock()
                Spacer(Modifier.height(32.dp))
                ServerCard(serverName = selectedServerName, onClick = onOpenProxies)
                Spacer(Modifier.height(10.dp))
                FastestButton(busy = isTesting, onClick = { onAction(MainAction.SelectFastest) })
            }
        }
    }
}

/** Elapsed connection time, ticking every second (HH:MM:SS). */
@Composable
private fun ConnectionTimer(since: Long?) {
    if (since == null) return
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(since) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val total = ((now - since) / 1000).coerceAtLeast(0)
    val text = String.format(Locale.US, "%02d:%02d:%02d", total / 3600, (total % 3600) / 60, total % 60)
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun SpeedRow(up: Long, down: Long) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.cell)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SpeedItem(arrow = "↓", label = stringResource(R.string.home_download), value = down.toSpeedString(), color = IosBlue)
        SpeedItem(arrow = "↑", label = stringResource(R.string.home_upload), value = up.toSpeedString(), color = IosGreen)
    }
}

@Composable
private fun SpeedItem(arrow: String, label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$arrow $label", style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FastestButton(busy: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .clickable(enabled = !busy, onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "⚡ " + stringResource(if (busy) R.string.home_fastest_testing else R.string.home_fastest),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ProfileCard(profileName: String, onUpdate: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_subscriptions_24dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = profileName.ifBlank { stringResource(R.string.app_name) },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onUpdate) {
                Icon(
                    painter = painterResource(R.drawable.ic_refresh_24dp),
                    contentDescription = stringResource(R.string.title_sub_update),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ConnectButton(isRunning: Boolean, onToggle: () -> Unit, size: Dp = 220.dp) {
    val color by animateColorAsState(
        targetValue = if (isRunning) ConnectedColor else DisconnectedColor,
        label = "connectColor",
    )
    val pulse = rememberInfiniteTransition(label = "connectPulse")
    val haloScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (isRunning) 1.12f else 1.04f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1400), RepeatMode.Reverse),
        label = "haloScale",
    )
    val actionLabel = stringResource(if (isRunning) R.string.acc_stop else R.string.acc_start)
    val stateLabel = stringResource(if (isRunning) R.string.home_connected else R.string.connection_not_connected)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clearAndSetSemantics {
                role = Role.Button
                contentDescription = actionLabel
                stateDescription = stateLabel
                onClick(label = actionLabel) { onToggle(); true }
            },
    ) {
        Box(
            modifier = Modifier
                .size(size * 0.91f)
                .scale(haloScale)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size * 0.73f)
                .clip(CircleShape)
                .background(color)
                .clickable(onClick = onToggle),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_power_24dp),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.33f),
            )
        }
    }
}

@Composable
private fun ServerCard(serverName: String, onClick: () -> Unit) {
    val title = stringResource(R.string.home_current_server)
    val name = serverName.ifBlank { stringResource(R.string.home_no_server) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_proxies_24dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right_24dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
