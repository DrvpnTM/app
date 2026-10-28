package net.drvpn.app.fmt

import net.drvpn.app.dto.entities.ProfileItem
import net.drvpn.app.enums.AetherIpVersion
import net.drvpn.app.enums.AetherProtocol
import net.drvpn.app.enums.AetherPsiphon
import net.drvpn.app.enums.AetherScanMode
import net.drvpn.app.enums.AetherTor
import net.drvpn.app.enums.AetherTransport
import net.drvpn.app.enums.EConfigType
import net.drvpn.app.util.Utils
import java.net.URI

/**
 * aether:// links, compatible with PattN/PattNG for the settings Dr VPN has:
 * aether://[host:port]?protocol=..&transport=..&scan=..&ip=..&tor=..&psiphon=..&outer=..&inner=..#name
 * The parameters PattN has beyond these are ignored when read.
 */
object AetherFmt : FmtBase() {

    private const val PLACEHOLDER = "endpoint.invalid"

    fun parse(str: String): ProfileItem? {
        val config = ProfileItem.create(EConfigType.AETHER)
        // A link without an endpoint ("aether://?protocol=..") has no authority, which URI refuses;
        // it is read with a placeholder host that is then left out.
        val rest = str.substringAfter("://", "")
        val noEndpoint = rest.isEmpty() || rest.startsWith("?") || rest.startsWith("#")
        val uri = URI(Utils.fixIllegalUrl(if (noEndpoint) "aether://$PLACEHOLDER$rest" else str))
        val query = if (uri.rawQuery.isNullOrEmpty()) emptyMap() else getQueryParam(uri)
        val protocol = AetherProtocol.fromString(query["protocol"])

        config.remarks = Utils.decodeURIComponent(uri.fragment.orEmpty()).ifEmpty { "Aether" }
        config.aetherProtocol = protocol.type
        config.aetherTransport = AetherTransport.fromString(query["transport"]).type
        config.aetherScanMode = AetherScanMode.fromString(query["scan"]).type
        config.aetherIpVersion = AetherIpVersion.fromString(query["ip"]).type
        config.aetherTor = AetherTor.fromString(query["tor"]).type.takeUnless { it == AetherTor.OFF.type }
        config.aetherPsiphon = AetherPsiphon.fromString(query["psiphon"]).type.takeUnless { it == AetherPsiphon.OFF.type }
        if (protocol.twoHops) {
            config.aetherWiwOuter = endpointOrNull(query["outer"])
            config.aetherWiwInner = endpointOrNull(query["inner"])
        } else {
            uri.host?.takeIf { it.isNotBlank() && !noEndpoint }?.let { host ->
                config.server = host.removePrefix("[").removeSuffix("]")
                config.serverPort = uri.port.takeIf { it > 0 }?.toString()
            }
        }
        return config
    }

    /** The link without "aether://", which the caller adds. */
    fun toUri(config: ProfileItem): String {
        val protocol = AetherProtocol.fromString(config.aetherProtocol)
        val query = linkedMapOf(
            "protocol" to protocol.type,
            "scan" to AetherScanMode.fromString(config.aetherScanMode).type,
            "ip" to AetherIpVersion.fromString(config.aetherIpVersion).type,
        )
        if (protocol.overMasque) query["transport"] = AetherTransport.fromString(config.aetherTransport).type
        AetherTor.fromString(config.aetherTor).takeUnless { it == AetherTor.OFF }?.let { query["tor"] = it.type }
        AetherPsiphon.fromString(config.aetherPsiphon).takeUnless { it == AetherPsiphon.OFF }?.let { query["psiphon"] = it.type }

        var authority = ""
        if (protocol.twoHops) {
            endpointOrNull(config.aetherWiwOuter)?.let { query["outer"] = it }
            endpointOrNull(config.aetherWiwInner)?.let { query["inner"] = it }
        } else {
            val host = config.server?.trim().orEmpty()
            if (host.isNotEmpty()) {
                val h = if (host.contains(':')) "[$host]" else host
                authority = config.serverPort?.trim()?.takeIf { it.isNotEmpty() }?.let { "$h:$it" } ?: h
            }
        }
        val queryText = query.entries.joinToString("&") { (k, v) -> "$k=${Utils.encodeURIComponent(v)}" }
        return "$authority?$queryText#${Utils.encodeURIComponent(config.remarks)}"
    }

    /** "host:port" as written, or null when blank. */
    private fun endpointOrNull(text: String?): String? = text?.trim()?.takeIf { it.isNotEmpty() }
}
