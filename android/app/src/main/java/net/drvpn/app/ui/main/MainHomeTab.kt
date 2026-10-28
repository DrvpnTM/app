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
import net.drvpn.app.extension.toTrafficString
import net.drvpn.app.ui.compose.IosBlue
import net.drvpn.app.ui.compose.IosGreen
import net.drvpn.app.ui.compose.cell
import kotlinx.coroutines.delay
import java.util.Locale
import net.drvpn.app.dto.entities.SubscriptionItem
import net.drvpn.app.ui.compose.IosRed
import net.drvpn.app.ui.compose.IosOrange
import androidx.compose.material3.LinearProgressIndicator
import net.drvpn.app.handler.AnnouncementManager
import androidx.compose.ui.platform.LocalUriHandler
import net.drvpn.app.R
import net.drvpn.app.ui.compose.DrVpnFont
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import net.drvpn.app.util.DeviceUtil
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.coerceIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxHeight
import net.drvpn.app.ui.compose.focusHighlight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush

private val ConnectedColor = Color(0xFF34C759)

/**
 * Hiddify-style home: active profile card, a large connect button, live status (timer + speed)
 * and the selected server card. Switches to two columns in landscape.
 */
@Composable
internal fun MainHomeTab(
    profileName: String,
    subscription: SubscriptionItem?,
    announcement: AnnouncementManager.Announcement?,
    selectedServerName: String,
    selectedGuid: String?,
    isRunning: Boolean,
    statusText: String,
    connectedSince: Long?,
    speedUp: Long,
    speedDown: Long,
    totalUp: Long,
    totalDown: Long,
    isTesting: Boolean,
    onAction: (MainAction) -> Unit,
    onOpenProxies: () -> Unit,
    serverRows: List<ServerRowUiModel> = emptyList(),
    modifier: Modifier = Modifier,
) {
    // Pulling Home up (or tapping the handle / server card) opens the server picker sheet.
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val selectedRow = serverRows.firstOrNull { it.guid == selectedGuid }
    val openPicker: () -> Unit = { if (serverRows.isNotEmpty()) showPicker = true else onOpenProxies() }
    val latestOpen by rememberUpdatedState(openPicker)
    val pullUp = remember {
        object : NestedScrollConnection {
            private var pulled = 0f
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y < 0f) {
                    pulled -= available.y
                    if (pulled > 140f) {
                        pulled = 0f
                        latestOpen()
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                pulled = 0f
                return Velocity.Zero
            }
        }
    }
    if (showPicker) {
        ServerPickerSheet(
            rows = serverRows,
            selectedGuid = selectedGuid,
            onSelect = { onAction(MainAction.SelectServer(it)) },
            onSelectFastest = { onAction(MainAction.SelectFastest) },
            testing = isTesting,
            onDismiss = { showPicker = false }
        )
    }
    // On a TV the remote starts on the connect button instead of the first control on screen.
    val isTv = DeviceUtil.isTv(LocalContext.current)
    val connectFocus = remember { FocusRequester() }
    var initialFocusDone by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(isTv) {
        if (isTv && !initialFocusDone) {
            runCatching { connectFocus.requestFocus() }
            initialFocusDone = true
        }
    }
    Column(modifier.fillMaxSize()) {
    BoxWithConstraints(
        Modifier
            .weight(1f)
            .fillMaxWidth()
            .nestedScroll(pullUp)
    ) {
        val landscape = maxWidth > maxHeight
        val availableHeight = maxHeight
        val connectBlock: @Composable () -> Unit = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                StatusPill(isRunning)
                Spacer(Modifier.height(if (landscape) 8.dp else 14.dp))
                ConnectButton(
                    isRunning = isRunning,
                    onToggle = { onAction(MainAction.ToggleService) },
                    focusRequester = connectFocus,
                    // In landscape (phones on their side, TVs) the button shrinks with the height so the
                    // status, timer and speed below it stay visible.
                    size = if (landscape) (availableHeight * 0.42f).coerceIn(110.dp, 200.dp) else 220.dp,
                )
                Spacer(Modifier.height(12.dp))
                if (!isRunning) {
                    Text(
                        text = stringResource(R.string.home_tap_to_connect),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isRunning) {
                    ConnectionTimer(connectedSince)
                    Spacer(Modifier.height(12.dp))
                    SpeedRow(up = speedUp, down = speedDown, totalUp = totalUp, totalDown = totalDown)
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
                val columnHeight = availableHeight - 24.dp
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = columnHeight),
                        contentAlignment = Alignment.Center
                    ) { connectBlock() }
                }
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (announcement != null) {
                        AnnouncementCard(announcement, onDismiss = { onAction(MainAction.DismissAnnouncement(announcement.id)) })
                        Spacer(Modifier.height(12.dp))
                    }
                    ProfileCard(
                        profileName = profileName,
                        subscription = subscription,
                        onUpdate = { onAction(MainAction.UpdateSubscriptions) },
                    )
                    Spacer(Modifier.height(12.dp))
                    LocationCard(
                        row = selectedRow,
                        fallbackName = selectedServerName,
                        onClick = openPicker,
                        onShowQr = selectedGuid?.let { guid -> { onAction(MainAction.ShareQRCode(guid)) } }
                    )
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
                if (announcement != null) {
                    AnnouncementCard(announcement, onDismiss = { onAction(MainAction.DismissAnnouncement(announcement.id)) })
                    Spacer(Modifier.height(12.dp))
                }
                ProfileCard(
                    profileName = profileName,
                    subscription = subscription,
                    onUpdate = { onAction(MainAction.UpdateSubscriptions) },
                )
                Spacer(Modifier.height(20.dp))
                connectBlock()
                Spacer(Modifier.height(20.dp))
                LocationCard(
                    row = selectedRow,
                    fallbackName = selectedServerName,
                    onClick = openPicker,
                    onShowQr = selectedGuid?.let { guid -> { onAction(MainAction.ShareQRCode(guid)) } }
                )
            }
        }
    }
    PullUpHandle(onOpen = openPicker)
    }
}

@Composable
private fun PullUpHandle(onOpen: () -> Unit) {
    val latest by rememberUpdatedState(onOpen)
    Column(
        Modifier
            .fillMaxWidth()
            .focusHighlight(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onOpen)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount -> if (dragAmount < -6f) latest() }
            }
            .padding(top = 6.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .width(40.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.server_picker_title),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
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
        fontSize = 30.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = DrVpnFont,
        style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum", textDirection = TextDirection.Ltr),
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun SpeedRow(up: Long, down: Long, totalUp: Long, totalDown: Long) {
    Row(
        modifier = Modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.cell)
            .height(IntrinsicSize.Min)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SpeedItem(arrow = "↓", label = stringResource(R.string.home_download), value = down.toSpeedString(), total = totalDown.toTrafficString(), color = IosBlue, modifier = Modifier.weight(1f))
        Box(
            Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        SpeedItem(arrow = "↑", label = stringResource(R.string.home_upload), value = up.toSpeedString(), total = totalUp.toTrafficString(), color = IosGreen, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SpeedItem(arrow: String, label: String, value: String, total: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(arrow, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(total, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
internal fun FastestButton(busy: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusHighlight(RoundedCornerShape(20.dp))
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

/**
 * Compact subscription card: icon, name, usage and days left on one line, a slim usage bar, and a
 * clear "Update" pill (a spinning-arrow icon alone was not obvious to users).
 */
@Composable
private fun ProfileCard(profileName: String, subscription: SubscriptionItem?, onUpdate: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(18.dp)
    val sub = subscription?.takeIf { it.usedBytes > 0 || it.totalBytes > 0 || it.expireAt > 0 }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.cell)
            .padding(start = 12.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_subscriptions_24dp),
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = profileName.ifBlank { stringResource(R.string.app_name) },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (sub != null) SubscriptionUsage(sub)
        }
        Spacer(Modifier.width(10.dp))
        val pill = RoundedCornerShape(50)
        Text(
            text = stringResource(R.string.home_sub_update),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = primary,
            maxLines = 1,
            modifier = Modifier
                .focusHighlight(pill)
                .clip(pill)
                .background(primary.copy(alpha = 0.12f))
                .clickable(role = Role.Button, onClick = onUpdate)
                .padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

/** "136 MB of 33 GB · 177 days left" and a slim usage bar, from the subscription-userinfo header. */
@Composable
private fun SubscriptionUsage(sub: SubscriptionItem) {
    val total = sub.totalBytes
    val used = sub.usedBytes.coerceAtLeast(0)
    val fraction = if (total > 0) (used.toFloat() / total).coerceIn(0f, 1f) else 0f
    val barColor = when {
        fraction >= 0.9f -> IosRed
        fraction >= 0.7f -> IosOrange
        else -> IosGreen
    }
    val usageText = if (total > 0) {
        stringResource(R.string.home_sub_usage, used.toTrafficString(), total.toTrafficString())
    } else {
        stringResource(R.string.home_sub_usage_unlimited, used.toTrafficString())
    }
    val expired = sub.expireAt in 1 until System.currentTimeMillis()
    val expiryText = when {
        sub.expireAt <= 0 -> null
        expired -> stringResource(R.string.home_sub_expired)
        else -> stringResource(R.string.home_sub_days_left, ((sub.expireAt - System.currentTimeMillis()) / 86_400_000L).toInt())
    }
    Text(
        text = listOfNotNull(usageText, expiryText).joinToString("  ·  "),
        style = MaterialTheme.typography.bodySmall,
        color = if (expired) IosRed else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 1.dp)
    )
    if (total > 0) {
        LinearProgressIndicator(
            progress = { fraction },
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            drawStopIndicator = {},
            gapSize = 0.dp,
            modifier = Modifier
                .padding(top = 6.dp)
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
        )
    }
}

/** Small capsule above the connect button: coloured dot + connection state. */
@Composable
private fun StatusPill(isRunning: Boolean) {
    val color = if (isRunning) ConnectedColor else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(if (isRunning) R.string.home_connected else R.string.connection_not_connected),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

@Composable
private fun ConnectButton(isRunning: Boolean, onToggle: () -> Unit, size: Dp = 220.dp, focusRequester: FocusRequester? = null) {
    // Off: brand blue (an invitation to tap, not a "disabled" grey). On: green.
    val top by animateColorAsState(
        targetValue = if (isRunning) Color(0xFF4ADE80) else Color(0xFF60A5FA),
        label = "connectTop",
    )
    val bottom by animateColorAsState(
        targetValue = if (isRunning) Color(0xFF16A34A) else Color(0xFF2563EB),
        label = "connectBottom",
    )
    val color = bottom
    val pulse = rememberInfiniteTransition(label = "connectPulse")
    val haloScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (isRunning) 1.12f else 1.05f,
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
                .background(color.copy(alpha = 0.12f)),
        )
        Box(
            modifier = Modifier
                .size(size * 0.80f)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.18f)),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size * 0.68f)
                .shadow(elevation = 16.dp, shape = CircleShape, ambientColor = color, spotColor = color)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .focusHighlight(CircleShape)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(top, bottom)))
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

/**
 * The selected server as design B draws it: round flag, country, protocol and signal bars on a
 * white card; tapping it opens the server picker.
 */
@Composable
private fun LocationCard(
    row: ServerRowUiModel?,
    fallbackName: String,
    onClick: () -> Unit,
    onShowQr: (() -> Unit)?,
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, shape, ambientColor = Color(0x140F172A), spotColor = Color(0x140F172A))
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .focusHighlight(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (row != null) {
            FlagCircle(row.flag, row.country.length == 2, size = 42.dp)
        } else {
            FlagCircle("", false, size = 42.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = row?.let { serverRowTitle(it.country) } ?: fallbackName.ifBlank { stringResource(R.string.home_no_server) },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (row != null) {
                Text(
                    text = row.typeDescription.replace(" / ", " · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (row != null && row.testDelayMillis != 0L) {
            SignalBars(row.testDelayMillis, Modifier.padding(horizontal = 6.dp))
        }
        if (onShowQr != null && row != null) {
            IconButton(onClick = onShowQr, modifier = Modifier.focusHighlight(CircleShape)) {
                Icon(
                    painter = painterResource(R.drawable.ic_qr_code_24dp),
                    contentDescription = stringResource(R.string.qr_transfer_title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AnnouncementCard(notice: AnnouncementManager.Announcement, onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusHighlight(RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .clickable(enabled = notice.link != null) { notice.link?.let { runCatching { uriHandler.openUri(it) } } }
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text("📢", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            if (notice.title.isNotBlank()) {
                Text(notice.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
            if (notice.message.isNotBlank()) {
                Text(notice.message, style = MaterialTheme.typography.bodyMedium)
            }
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Text("✕", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
