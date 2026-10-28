package net.drvpn.app.enums

/**
 * Aether settings, with the names and values PattN/PattNG use in profiles and aether:// links.
 * [type] is what a link and a profile store; [core] is the word Aether's command line takes.
 */
enum class AetherProtocol(val type: String, val core: String = type) {
    MASQUE("masque"),
    WIREGUARD("wg"),
    /** WARP inside WARP over WireGuard (Aether's classic gool). */
    GOOL("gool"),
    /** MASQUE inside MASQUE. */
    MIM("mim"),
    /** WireGuard inside a MASQUE tunnel. */
    WG_OVER_MASQUE("wg-over-masque", "gool");

    val overMasque: Boolean get() = this == MASQUE || this == MIM || this == WG_OVER_MASQUE
    val twoHops: Boolean get() = this == GOOL || this == MIM || this == WG_OVER_MASQUE

    companion object {
        fun fromString(type: String?) = entries.find { it.type == type } ?: WIREGUARD
    }
}

enum class AetherTransport(val type: String) {
    HTTP3("h3"),
    HTTP2("h2");

    companion object {
        fun fromString(type: String?) = entries.find { it.type == type } ?: HTTP3
    }
}

enum class AetherScanMode(val type: String) {
    TURBO("turbo"),
    BALANCED("balanced"),
    THOROUGH("thorough"),
    VERIFIED("verified"),
    IRONCLAD("ironclad");

    companion object {
        fun fromString(type: String?) = entries.find { it.type == type } ?: if (type == "stealth") VERIFIED else BALANCED
    }
}

enum class AetherIpVersion(val type: String) {
    V4("v4"),
    V6("v6"),
    DUAL("both");

    companion object {
        fun fromString(type: String?) = entries.find { it.type == type } ?: V4
    }
}

/** Tor with WARP: off; inside the tunnel (exit is Tor); around it (WARP reached over Tor); Tor alone. */
enum class AetherTor(val type: String) {
    OFF("off"),
    CHAIN("chain"),
    REVERSE("reverse"),
    ONLY("only");

    companion object {
        fun fromString(type: String?) = entries.find { it.type == type } ?: OFF
    }
}

/** Psiphon with WARP, in the same four ways as [AetherTor]. */
enum class AetherPsiphon(val type: String) {
    OFF("off"),
    CHAIN("chain"),
    REVERSE("reverse"),
    ONLY("only");

    companion object {
        fun fromString(type: String?) = entries.find { it.type == type } ?: OFF
    }
}
