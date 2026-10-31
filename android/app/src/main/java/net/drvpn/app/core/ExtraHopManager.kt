package net.drvpn.app.core

import android.content.Context
import net.drvpn.app.AppConfig
import net.drvpn.app.dto.CoreConfigContext
import net.drvpn.app.dto.V2rayConfig
import net.drvpn.app.dto.entities.ProfileItem
import net.drvpn.app.enums.AetherPsiphon
import net.drvpn.app.enums.AetherTor
import net.drvpn.app.enums.CoreResolvedType
import net.drvpn.app.enums.EConfigType
import net.drvpn.app.fmt.WireguardFmt
import net.drvpn.app.handler.MmkvManager
import net.drvpn.app.util.LogUtil

/** Where the extra hop sits in the chain. */
enum class ExtraHopMode(val value: String) {
    OFF("off"),

    /** Device → extra hop → server: the server sees the extra hop's address. */
    BEFORE("before"),

    /** Device → server → extra hop: sites see the extra hop's address (the final IP). */
    AFTER("after");

    companion object {
        fun from(value: String?) = entries.find { it.value == value } ?: OFF
    }
}

/**
 * What the extra hop is. The names are the app's own; the UI never names the provider.
 * Great Wall, PS Pro and Black CDN are free and need no account; all three run inside Aether.
 */
enum class ExtraHopKind(val value: String) {
    /** "Great Wall": Cloudflare WARP. */
    SHIELD("shield"),

    /** "PS Pro": Psiphon. */
    PSIPHON("psiphon"),

    /** "Black insecure CDN": Tor. */
    TOR("tor"),

    /** "Dr Tunnel": a WireGuard config the user pastes (Mozilla VPN, Mullvad, ...). */
    TUNNEL("tunnel"),

    /** "Silk Road": Lantern (LanternManager). Free; works only before the server. */
    LANTERN("lantern"),

    /** Emergency: tries the others one by one until one connects (CoreServiceManager). */
    EMERGENCY("emergency");

    companion object {
        fun from(value: String?) = entries.find { it.value == value } ?: SHIELD
    }
}

/**
 * Chains one extra hop to the selected server, as Hiddify does with WARP: either in front of it
 * or behind it. Applied only to the running connection, not to ping tests.
 */
object ExtraHopManager {

    fun mode(): ExtraHopMode = ExtraHopMode.from(MmkvManager.decodeSettingsString(AppConfig.PREF_EXTRA_HOP_MODE))

    fun kind(): ExtraHopKind = ExtraHopKind.from(MmkvManager.decodeSettingsString(AppConfig.PREF_EXTRA_HOP_KIND))

    fun isEmergency(): Boolean = mode() != ExtraHopMode.OFF && kind() == ExtraHopKind.EMERGENCY

    /** In emergency, the kind being tried now. Lives in the VPN process; set by CoreServiceManager. */
    @Volatile
    var emergencyKind: ExtraHopKind? = null

    /**
     * What emergency tries, in order: the ones that connect fastest first, Tor last. Dr Tunnel
     * only when it has a config.
     */
    fun emergencyCandidates(): List<ExtraHopKind> = buildList {
        add(ExtraHopKind.SHIELD)
        add(ExtraHopKind.LANTERN)
        if (parseWireGuard(MmkvManager.decodeSettingsString(AppConfig.PREF_EXTRA_HOP_WIREGUARD).orEmpty()) != null) {
            add(ExtraHopKind.TUNNEL)
        }
        add(ExtraHopKind.PSIPHON)
        add(ExtraHopKind.TOR)
    }

    /** The kind the connection uses: the chosen one, or in emergency the one being tried. */
    fun effectiveKind(): ExtraHopKind {
        val chosen = kind()
        if (chosen != ExtraHopKind.EMERGENCY) return chosen
        return emergencyKind ?: emergencyCandidates().first()
    }

    /**
     * The hop's profile for a connection to [main], or null when it is off or cannot be used:
     * the server is itself Aether, a custom config or a policy group, or the WireGuard config is invalid.
     */
    fun hopProfile(context: Context, main: ProfileItem): ProfileItem? {
        if (mode() == ExtraHopMode.OFF) return null
        if (main.configType == EConfigType.AETHER ||
            main.configType == EConfigType.CUSTOM ||
            main.configType == EConfigType.POLICYGROUP
        ) {
            return null
        }
        val kind = effectiveKind()
        val aether = AetherManager.isSupported(context)
        return when (kind) {
            ExtraHopKind.SHIELD -> if (aether) shieldProfile() else null
            ExtraHopKind.PSIPHON -> if (aether) aetherOnly { aetherPsiphon = AetherPsiphon.ONLY.type } else null
            ExtraHopKind.TOR -> if (aether) aetherOnly { aetherTor = AetherTor.ONLY.type } else null
            ExtraHopKind.TUNNEL -> parseWireGuard(MmkvManager.decodeSettingsString(AppConfig.PREF_EXTRA_HOP_WIREGUARD).orEmpty())
            ExtraHopKind.LANTERN -> if (LanternManager.isSupported(context)) LanternManager.hopProfile() else null
            ExtraHopKind.EMERGENCY -> null
        }.also {
            if (it == null) LogUtil.w(AppConfig.TAG, "Extra hop: ${kind.value} is not usable, connecting without it")
        }
    }

    /** "<kind>|<mode>" of [hop] as it runs, for the Home screen (AppConfig.PREF_EXTRA_HOP_ACTIVE). */
    fun activeLabel(hop: ProfileItem): String {
        val mode = if (LanternManager.isHop(hop)) ExtraHopMode.BEFORE else mode()
        return "${effectiveKind().value}|${mode.value}"
    }

    /** A fresh Aether profile that runs only Psiphon or only Tor (no WARP). */
    private fun aetherOnly(setup: ProfileItem.() -> Unit): ProfileItem =
        ProfileItem.create(EConfigType.AETHER).apply {
            remarks = "hop"
            setup()
        }

    /** The user's first Aether profile (keeps their tuned settings), or Aether's defaults. */
    private fun shieldProfile(): ProfileItem =
        MmkvManager.decodeAllServerList()
            .asSequence()
            .mapNotNull { MmkvManager.decodeServerConfig(it) }
            .firstOrNull { it.configType == EConfigType.AETHER }
            ?: ProfileItem.create(EConfigType.AETHER).apply { remarks = "shield" }

    /** A wg-quick config or a wireguard:// link, or null when keys or the endpoint are missing. */
    fun parseWireGuard(text: String): ProfileItem? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        val profile = try {
            when {
                trimmed.startsWith("wireguard://", true) || trimmed.startsWith("wg://", true) -> WireguardFmt.parse(trimmed)
                trimmed.contains("[Interface]", true) -> WireguardFmt.parseWireguardConfFile(trimmed).also { fixEndpoint(it, trimmed) }
                else -> null
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Extra hop: cannot read the WireGuard config", e)
            null
        } ?: return null
        val valid = !profile.secretKey.isNullOrBlank() &&
                !profile.publicKey.isNullOrBlank() &&
                !profile.server.isNullOrBlank() &&
                profile.serverPort?.toIntOrNull() != null
        return profile.takeIf { valid }?.apply { remarks = "tunnel" }
    }

    /** The conf parser splits "Endpoint" at the first colon, which breaks "[v6]:port"; split at the last. */
    private fun fixEndpoint(profile: ProfileItem, conf: String) {
        val endpoint = conf.lines()
            .map { it.trim() }
            .firstOrNull { it.startsWith("Endpoint", true) && it.contains('=') }
            ?.substringAfter('=')?.trim() ?: return
        val i = endpoint.lastIndexOf(':')
        if (i <= 0) return
        profile.server = endpoint.substring(0, i).removePrefix("[").removeSuffix("]")
        profile.serverPort = endpoint.substring(i + 1)
    }

    /**
     * [configContext] with [hop] added to the main server's chain. Chain order is exit first:
     * index 0 is the outbound sites see, the last one dials the network.
     */
    fun withHop(configContext: CoreConfigContext, hop: ProfileItem): CoreConfigContext {
        val primary = configContext.resolvedOutbounds.firstOrNull() ?: return configContext
        if (primary.resolvedType == CoreResolvedType.POLICYGROUP || primary.resolvedProfiles.isEmpty()) return configContext
        // Lantern's own traffic cannot be sent through the server, so it always goes first.
        val mode = if (LanternManager.isHop(hop)) ExtraHopMode.BEFORE else mode()
        val chain = when (mode) {
            ExtraHopMode.AFTER -> listOf(hop) + primary.resolvedProfiles
            ExtraHopMode.BEFORE -> primary.resolvedProfiles + hop
            ExtraHopMode.OFF -> return configContext
        }
        LogUtil.i(AppConfig.TAG, "Extra hop: ${effectiveKind().value} ${mode.value} the server")
        val chained = primary.copy(resolvedProfiles = chain, resolvedType = CoreResolvedType.PROXYCHAIN)
        return configContext.copy(resolvedOutbounds = listOf(chained) + configContext.resolvedOutbounds.drop(1))
    }

    /**
     * Where Aether's own traffic must go. Before the server it goes out directly. After the server
     * it goes through the server: the Aether outbound ("proxy") only dials 127.0.0.1, so its
     * dialerProxy is dropped and its next hop becomes the target of Aether's upstream instead.
     */
    fun aetherUpstreamTarget(v2rayConfig: V2rayConfig): String {
        if (mode() != ExtraHopMode.AFTER) return AppConfig.TAG_DIRECT
        val aether = v2rayConfig.outbounds.firstOrNull { it.tag == AppConfig.TAG_PROXY } ?: return AppConfig.TAG_DIRECT
        val sockopt = aether.streamSettings?.sockopt ?: return AppConfig.TAG_DIRECT
        val next = sockopt.dialerProxy ?: return AppConfig.TAG_DIRECT
        sockopt.dialerProxy = null
        return next
    }
}
