package com.v2ray.ang.handler

/**
 * Dr VPN: replaces channel IDs / links that config sellers put in server names with our site.
 */
object RemarksBranding {
    const val SITE = "drvpn.net"

    private val patterns = listOf(
        Regex("""(?i)\b(?:https?://)?(?:t|telegram)\.(?:me|dog)/(?!drvpn_net\b)\S+"""),
        Regex("""(?i)\bhttps?://(?!(?:t|telegram)\.me/drvpn_net\b)\S+"""),
        Regex("""(?i)\bwww\.\S+"""),
        // Any @handle except our own channel.
        Regex("""(?i)@(?!drvpn_net\b)[A-Za-z][A-Za-z0-9_]{2,}"""),
    )

    fun apply(remarks: String?): String {
        if (remarks.isNullOrBlank()) return SITE
        var result: String = remarks
        patterns.forEach { result = it.replace(result, SITE) }
        // Collapse repeats such as "drvpn.net | drvpn.net".
        result = Regex("""(${Regex.escape(SITE)})(?:[\s|_\-–—:/]*${Regex.escape(SITE)})+""").replace(result, SITE)
        return result.trim().ifEmpty { SITE }
    }
}
