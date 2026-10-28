package net.drvpn.app.ui.check

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.drvpn.app.R
import net.drvpn.app.handler.NetworkCheckManager
import net.drvpn.app.handler.ServerCountryManager
import net.drvpn.app.ui.base.BaseComponentActivity
import net.drvpn.app.ui.compose.AppTopBar
import net.drvpn.app.ui.compose.cell
import net.drvpn.app.ui.compose.focusHighlight
import java.util.Locale

private val Good = Color(0xFF16A34A)
private val GoodBg = Color(0xFFDCFCE7)
private val Bad = Color(0xFFDC2626)
private val BadBg = Color(0xFFFEE2E2)

/** Drawer "Check & speed test": a speed test and a DNS leak check, both through the tunnel. */
class NetworkCheckActivity : BaseComponentActivity() {

    @Composable
    override fun ScreenContent() {
        Scaffold(
            contentWindowInsets = WindowInsets(0),
            topBar = { AppTopBar(title = stringResource(R.string.check_title), onBackClick = { finish() }) }
        ) { innerPadding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SpeedTestCard()
                DnsLeakCard()
            }
        }
    }
}

@Composable
private fun CheckCard(icon: Int, title: String, badge: (@Composable () -> Unit)? = null, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.cell)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(icon), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            badge?.invoke()
        }
        content()
    }
}

@Composable
private fun Pill(text: String, fg: Color, bg: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = fg,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun ActionButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .focusHighlight(RoundedCornerShape(16.dp))
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun Note(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun fmt(mbps: Double) = String.format(Locale.US, if (mbps >= 100) "%.0f" else "%.1f", mbps)

private enum class SpeedPhase { Idle, Ping, Download, Upload, Done, Failed }

@Composable
private fun SpeedTestCard() {
    val scope = rememberCoroutineScope()
    var phase by remember { mutableStateOf(SpeedPhase.Idle) }
    var live by remember { mutableStateOf(0.0) }
    var progress by remember { mutableStateOf(0f) }
    var ping by remember { mutableStateOf<Long?>(null) }
    var down by remember { mutableStateOf<Double?>(null) }
    var up by remember { mutableStateOf<Double?>(null) }
    var viaVpn by remember { mutableStateOf<Boolean?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    val running = phase == SpeedPhase.Ping || phase == SpeedPhase.Download || phase == SpeedPhase.Upload

    CheckCard(
        icon = R.drawable.ic_speed_24dp,
        title = stringResource(R.string.check_speed_title),
        badge = viaVpn?.let { vpn ->
            {
                if (vpn) Pill(stringResource(R.string.check_via_vpn), Good, GoodBg)
                else Pill(stringResource(R.string.check_no_vpn), MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            val big = when (phase) {
                SpeedPhase.Download, SpeedPhase.Upload -> fmt(live)
                SpeedPhase.Done -> down?.let(::fmt) ?: "—"
                else -> "—"
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    big,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.displaySmall.copy(textDirection = TextDirection.Ltr, fontFeatureSettings = "tnum"),
                )
                Spacer(Modifier.width(6.dp))
                Text("Mbps", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 12.dp))
            }
            Text(
                when (phase) {
                    SpeedPhase.Ping -> stringResource(R.string.check_ping)
                    SpeedPhase.Download -> stringResource(R.string.check_download)
                    SpeedPhase.Upload -> stringResource(R.string.check_upload)
                    SpeedPhase.Failed -> stringResource(R.string.check_failed)
                    SpeedPhase.Done -> stringResource(R.string.check_download)
                    SpeedPhase.Idle -> ""
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (phase == SpeedPhase.Failed) Bad else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (running) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { if (phase == SpeedPhase.Ping) 0f else progress },
                    drawStopIndicator = {},
                    gapSize = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(stringResource(R.string.check_ping), ping?.let { if (it < 0) "—" else "$it ms" } ?: "—", Modifier.weight(1f))
            StatTile(stringResource(R.string.check_download), down?.let { "${fmt(it)} Mbps" } ?: "—", Modifier.weight(1f))
            StatTile(stringResource(R.string.check_upload), up?.let { "${fmt(it)} Mbps" } ?: "—", Modifier.weight(1f))
        }
        ActionButton(
            text = stringResource(
                when {
                    running -> R.string.check_running
                    phase == SpeedPhase.Idle -> R.string.check_start
                    else -> R.string.check_again
                }
            ),
            enabled = !running,
        ) {
            job?.cancel()
            job = scope.launch {
                ping = null; down = null; up = null; live = 0.0; progress = 0f
                try {
                    val route = NetworkCheckManager.currentRoute()
                    viaVpn = route.viaVpn
                    phase = SpeedPhase.Ping
                    ping = NetworkCheckManager.ping(route)
                    phase = SpeedPhase.Download
                    down = NetworkCheckManager.download(route) { m, f -> live = m; progress = f }
                    live = 0.0; progress = 0f
                    phase = SpeedPhase.Upload
                    up = NetworkCheckManager.upload(route) { m, f -> live = m; progress = f }
                    phase = SpeedPhase.Done
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    phase = SpeedPhase.Failed
                }
            }
        }
        Note(stringResource(R.string.check_speed_note))
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.titleSmall.copy(textDirection = TextDirection.Ltr),
            maxLines = 1,
        )
    }
}

private sealed interface LeakState {
    data object Idle : LeakState
    data object NotConnected : LeakState
    data object Running : LeakState
    data object Failed : LeakState
    class Done(val result: NetworkCheckManager.LeakResult) : LeakState
}

@Composable
private fun DnsLeakCard() {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<LeakState>(LeakState.Idle) }
    val locale = LocalConfiguration.current.locales[0]

    CheckCard(icon = R.drawable.ic_lock_24dp, title = stringResource(R.string.check_dns_title)) {
        Note(stringResource(R.string.check_dns_desc))
        when (val s = state) {
            LeakState.NotConnected -> Text(stringResource(R.string.check_dns_connect_first), color = Bad, style = MaterialTheme.typography.bodyMedium)
            LeakState.Failed -> Text(stringResource(R.string.check_failed), color = Bad, style = MaterialTheme.typography.bodyMedium)
            LeakState.Running -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.check_running), style = MaterialTheme.typography.bodyMedium)
            }
            is LeakState.Done -> LeakResultView(s.result, locale)
            LeakState.Idle -> Unit
        }
        ActionButton(
            text = stringResource(if (state is LeakState.Done) R.string.check_again else R.string.check_dns_start),
            enabled = state != LeakState.Running,
        ) {
            scope.launch {
                val route = NetworkCheckManager.currentRoute()
                if (!route.viaVpn) {
                    state = LeakState.NotConnected
                    return@launch
                }
                state = LeakState.Running
                state = try {
                    LeakState.Done(NetworkCheckManager.dnsLeak(route))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    LeakState.Failed
                }
            }
        }
        Note(stringResource(R.string.check_dns_note))
    }
}

private class Verdict(val fg: Color, val bg: Color, val title: String, val sub: String)

@Composable
private fun LeakResultView(result: NetworkCheckManager.LeakResult, locale: Locale) {
    val verdict = when (result.leaking) {
        false -> Verdict(Good, GoodBg, stringResource(R.string.check_dns_ok), stringResource(R.string.check_dns_ok_sub))
        true -> Verdict(Bad, BadBg, stringResource(R.string.check_dns_leak), stringResource(R.string.check_dns_leak_sub))
        null -> Verdict(
            MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceVariant,
            stringResource(R.string.check_dns_unknown), ""
        )
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(verdict.bg)
            .padding(14.dp)
    ) {
        Text(verdict.title, style = MaterialTheme.typography.titleSmall, color = verdict.fg)
        if (verdict.sub.isNotEmpty()) Text(verdict.sub, style = MaterialTheme.typography.bodySmall, color = verdict.fg)
    }
    result.ownIsp?.let { isp ->
        Text(stringResource(R.string.check_dns_own_isp, isp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    result.exit?.let { exit ->
        Text(stringResource(R.string.check_dns_exit), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ServerLine(exit, locale)
    }
    if (result.servers.isNotEmpty()) {
        Text(
            stringResource(R.string.check_dns_servers, result.servers.size),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        result.servers.take(8).forEach { ServerLine(it, locale) }
    }
}

@Composable
private fun ServerLine(server: NetworkCheckManager.DnsServer, locale: Locale) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Text(ServerCountryManager.flagOf(server.country) ?: "🌐", fontSize = 16.sp)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(server.ip, style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr), maxLines = 1)
            val place = listOfNotNull(
                server.country.takeIf { it.length == 2 }?.let { Locale("", it).getDisplayCountry(locale) },
                server.isp.substringAfter(' ', server.isp).takeIf { it.isNotBlank() }
            ).joinToString(" · ")
            if (place.isNotEmpty()) Text(place, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}
