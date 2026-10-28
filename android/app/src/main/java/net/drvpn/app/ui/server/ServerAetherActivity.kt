package net.drvpn.app.ui.server

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.drvpn.app.R
import net.drvpn.app.core.AetherManager
import net.drvpn.app.enums.AetherIpVersion
import net.drvpn.app.enums.AetherProtocol
import net.drvpn.app.enums.AetherPsiphon
import net.drvpn.app.enums.AetherScanMode
import net.drvpn.app.enums.AetherTor
import net.drvpn.app.enums.AetherTransport
import net.drvpn.app.enums.EConfigType
import net.drvpn.app.extension.toast
import net.drvpn.app.extension.toastSuccess
import net.drvpn.app.handler.AngConfigManager
import net.drvpn.app.handler.MmkvManager
import net.drvpn.app.ui.compose.FormDropdownField
import net.drvpn.app.ui.compose.FormTextField
import net.drvpn.app.ui.server.ProfileEditorResult.finishSaved

/**
 * Editor of an Aether (WARP) profile: the settings PattN's editor has that matter most, and the
 * whole Aether command line for everything else.
 */
class ServerAetherActivity : BaseServerActivity() {

    override val serverConfigType: EConfigType = EConfigType.AETHER

    private val protocols = listOf(
        AetherProtocol.WIREGUARD to "WireGuard (wg)",
        AetherProtocol.MASQUE to "MASQUE",
        AetherProtocol.GOOL to "WARP in WARP (gool)",
        AetherProtocol.MIM to "MASQUE in MASQUE (mim)",
        AetherProtocol.WG_OVER_MASQUE to "WireGuard over MASQUE",
    )
    private val transports = listOf(AetherTransport.HTTP3 to "HTTP/3", AetherTransport.HTTP2 to "HTTP/2")

    @Composable
    override fun ScreenContent() {
        val c = initialConfig
        var remarks by rememberSaveable { mutableStateOf(c.remarks.ifBlank { "Aether WARP" }) }
        var protocol by rememberSaveable { mutableStateOf(AetherProtocol.fromString(c.aetherProtocol).type) }
        var transport by rememberSaveable { mutableStateOf(AetherTransport.fromString(c.aetherTransport).type) }
        var scan by rememberSaveable { mutableStateOf(AetherScanMode.fromString(c.aetherScanMode).type) }
        var ip by rememberSaveable { mutableStateOf(AetherIpVersion.fromString(c.aetherIpVersion).type) }
        var endpoint by rememberSaveable {
            mutableStateOf(
                c.server?.takeIf { it.isNotBlank() }?.let { host ->
                    val h = if (host.contains(':')) "[$host]" else host
                    c.serverPort?.takeIf { it.isNotBlank() }?.let { "$h:$it" } ?: h
                }.orEmpty()
            )
        }
        var outer by rememberSaveable { mutableStateOf(c.aetherWiwOuter.orEmpty()) }
        var inner by rememberSaveable { mutableStateOf(c.aetherWiwInner.orEmpty()) }
        var tor by rememberSaveable { mutableStateOf(AetherTor.fromString(c.aetherTor).type) }
        var psiphon by rememberSaveable { mutableStateOf(AetherPsiphon.fromString(c.aetherPsiphon).type) }
        var command by rememberSaveable { mutableStateOf(c.aetherCommand.orEmpty()) }

        val modeLabels = listOf(
            stringResource(R.string.aether_mode_off),
            stringResource(R.string.aether_mode_chain),
            stringResource(R.string.aether_mode_reverse),
            stringResource(R.string.aether_mode_only),
        )
        val torModes = AetherTor.entries.map { it.type }
        val psiphonModes = AetherPsiphon.entries.map { it.type }
        val selected = AetherProtocol.fromString(protocol)

        ServerEditorScaffold(
            title = stringResource(R.string.aether_title),
            onSaveClick = {
                save(remarks, protocol, transport, scan, ip, endpoint, outer, inner, tor, psiphon, command)
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!AetherManager.isSupported(this@ServerAetherActivity)) {
                    Text(
                        stringResource(R.string.aether_not_supported),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
                Text(
                    stringResource(R.string.aether_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                FormTextField(stringResource(R.string.server_lab_remarks), remarks, { remarks = it })
                FormDropdownField(
                    stringResource(R.string.aether_protocol),
                    protocols.first { it.first.type == protocol }.second,
                    protocols.map { it.second },
                    { label -> protocol = protocols.first { it.second == label }.first.type }
                )
                if (selected.overMasque) {
                    FormDropdownField(
                        stringResource(R.string.aether_transport),
                        transports.first { it.first.type == transport }.second,
                        transports.map { it.second },
                        { label -> transport = transports.first { it.second == label }.first.type }
                    )
                }
                if (selected.twoHops) {
                    FormTextField(
                        stringResource(R.string.aether_outer_hop), outer, { outer = it },
                        placeholder = "162.159.192.1:2408", supportingText = stringResource(R.string.aether_endpoint_hint)
                    )
                    FormTextField(
                        stringResource(R.string.aether_inner_hop), inner, { inner = it },
                        placeholder = "162.159.195.1:500", supportingText = stringResource(R.string.aether_endpoint_hint)
                    )
                } else {
                    FormTextField(
                        stringResource(R.string.aether_endpoint), endpoint, { endpoint = it },
                        placeholder = "162.159.192.1:2408", supportingText = stringResource(R.string.aether_endpoint_hint)
                    )
                }
                FormDropdownField(
                    stringResource(R.string.aether_scan),
                    scan,
                    AetherScanMode.entries.map { it.type },
                    { scan = it }
                )
                FormDropdownField(
                    stringResource(R.string.aether_ip),
                    ip,
                    AetherIpVersion.entries.map { it.type },
                    { ip = it }
                )
                FormDropdownField(
                    "Tor",
                    modeLabels[torModes.indexOf(tor)],
                    modeLabels,
                    { label -> tor = torModes[modeLabels.indexOf(label)]; if (tor != AetherTor.OFF.type) psiphon = AetherPsiphon.OFF.type }
                )
                FormDropdownField(
                    "Psiphon",
                    modeLabels[psiphonModes.indexOf(psiphon)],
                    modeLabels,
                    { label -> psiphon = psiphonModes[modeLabels.indexOf(label)]; if (psiphon != AetherPsiphon.OFF.type) tor = AetherTor.OFF.type }
                )
                FormTextField(
                    stringResource(R.string.aether_command), command, { command = it },
                    placeholder = "--protocol masque --scan turbo",
                    supportingText = stringResource(R.string.aether_command_hint),
                    maxLines = 8
                )
            }
        }
    }

    private fun save(
        remarks: String, protocol: String, transport: String, scan: String, ip: String, endpoint: String,
        outer: String, inner: String, tor: String, psiphon: String, command: String,
    ): Boolean {
        if (remarks.isBlank()) {
            toast(R.string.server_lab_remarks)
            return false
        }
        val (host, port) = splitEndpoint(endpoint.trim()) ?: run {
            toast(R.string.aether_endpoint_hint)
            return false
        }
        val config = initialConfig.copy().apply {
            this.remarks = remarks.trim()
            aetherProtocol = protocol
            aetherTransport = transport
            aetherScanMode = scan
            aetherIpVersion = ip
            server = host
            serverPort = port
            aetherWiwOuter = outer.trim().ifEmpty { null }
            aetherWiwInner = inner.trim().ifEmpty { null }
            aetherTor = tor.takeUnless { it == AetherTor.OFF.type }
            aetherPsiphon = psiphon.takeUnless { it == AetherPsiphon.OFF.type }
            aetherCommand = command.trim().ifEmpty { null }
        }
        config.description = AngConfigManager.generateDescription(config)
        if (config.subscriptionId.isEmpty() && !subscriptionId.isNullOrEmpty()) {
            config.subscriptionId = subscriptionId.orEmpty()
        }
        val savedGuid = MmkvManager.encodeServerConfig(editGuid, config)
        toastSuccess(R.string.toast_success)
        finishSaved(savedGuid, isRunning)
        return true
    }

    /** "host:port", "[v6]:port" or "" (Aether scans); null when it can't be read. */
    internal fun splitEndpoint(text: String): Pair<String?, String?>? {
        if (text.isEmpty()) return null to null
        val (host, port) = if (text.startsWith("[")) {
            val end = text.indexOf(']').takeIf { it > 0 } ?: return null
            text.substring(1, end) to text.substring(end + 1).removePrefix(":").ifEmpty { null }
        } else if (text.count { it == ':' } == 1) {
            text.substringBefore(':') to text.substringAfter(':')
        } else {
            text to null
        }
        if (host.isBlank()) return null
        if (port != null && (port.toIntOrNull() ?: 0) !in 1..65535) return null
        return host to port
    }
}
