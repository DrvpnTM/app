package net.drvpn.app.ui.main

import net.drvpn.app.dto.entities.ProfileItem
import net.drvpn.app.dto.entities.ServersCache
import net.drvpn.app.extension.isComplexType
import net.drvpn.app.extension.nullIfBlank
import net.drvpn.app.handler.AngConfigManager
import net.drvpn.app.handler.ServerCountryManager

internal data class ServerRowUiModel(
    val guid: String,
    val profile: ProfileItem,
    val remarks: String,
    val statistics: String,
    val typeDescription: String,
    val testDelayMillis: Long,
    val subscriptionBadge: String,
    /** Country flag emoji of the server address, empty when unknown. */
    val flag: String = "",
    /** ISO country code (from the name or the address), empty when unknown. */
    val country: String = "",
    /** Protocol name, e.g. VLESS; used for the category chips. */
    val protocol: String = profile.configType.name,
)

/** Server category chip: a country ("c:DE") or a protocol ("p:VLESS"). */
internal fun ServerRowUiModel.matchesCategory(category: String?): Boolean = when {
    category.isNullOrEmpty() -> true
    category.startsWith("c:") -> country == category.substring(2)
    category.startsWith("p:") -> protocol == category.substring(2)
    else -> true
}

/** Lowest ping first; untested next; failed last. */
internal val serverPingOrder: Comparator<ServerRowUiModel> =
    compareBy<ServerRowUiModel>({
        when {
            it.testDelayMillis > 0L -> 0
            it.testDelayMillis == 0L -> 1
            else -> 2
        }
    }, { if (it.testDelayMillis > 0L) it.testDelayMillis else 0L })

internal data class ServerGroupUiState(
    val servers: List<ServersCache> = emptyList(),
    /** Rows shown in the list; may hide servers without a ping result (see MainUiState.showServersWithoutPing). */
    val rows: List<ServerRowUiModel> = emptyList(),
    /** Every row for [servers], before the ping filter. */
    val allRows: List<ServerRowUiModel> = rows,
)

internal fun buildServerRowUiModel(
    server: ServersCache,
    subscriptionRemarks: String,
): ServerRowUiModel {
    val profile = server.profile
    return ServerRowUiModel(
        guid = server.guid,
        profile = profile,
        remarks = profile.remarks,
        statistics = profile.description.nullIfBlank()
            ?: AngConfigManager.generateDescription(profile),
        typeDescription = serverProtocolDescription(profile),
        testDelayMillis = server.testDelayMillis,
        subscriptionBadge = subscriptionRemarks.firstOrNull()?.toString().orEmpty(),
        flag = serverFlag(profile),
        country = serverCountry(profile).orEmpty(),
    )
}

/**
 * Where the server is: the country the seller wrote in the name (flag or name) wins, since for CDN
 * servers the address only tells where the CDN is; otherwise the address's country. Nothing is
 * guessed when neither is known.
 */
internal fun serverCountry(profile: ProfileItem): String? =
    ServerCountryManager.countryFromRemarks(profile.remarks)
        ?: ServerCountryManager.countryCode(profile.server)

internal fun serverFlag(profile: ProfileItem): String =
    ServerCountryManager.flagOf(serverCountry(profile)).orEmpty()

/** The in-app language (falls back to the system one). */
internal fun appLocale(): java.util.Locale =
    androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()[0] ?: java.util.Locale.getDefault()

/** Branded display name used instead of the seller's config name: "DRVPN · Germany". */
internal fun serverDisplayName(country: String?, locale: java.util.Locale): String {
    val countryName = country?.takeIf { it.length == 2 }
        ?.let { java.util.Locale("", it).getDisplayCountry(locale) }
        ?.takeIf { it.isNotBlank() && it != country }
    return if (countryName != null) "DRVPN · $countryName" else "DRVPN"
}

private fun serverProtocolDescription(profile: ProfileItem): String {
    if (profile.configType.isComplexType()) return profile.configType.name
    val parts = mutableListOf(profile.configType.name)
    profile.network?.let { network ->
        if (network.isNotBlank() && !network.equals("tcp", ignoreCase = true)) {
            parts.add(network)
        }
    }
    profile.security?.let { security ->
        if (security.isNotBlank()) {
            parts.add(
                if (profile.insecure == true && security.equals("tls", ignoreCase = true)) {
                    "$security insecure"
                } else {
                    security
                }
            )
        }
    }
    return parts.joinToString(" / ")
}
