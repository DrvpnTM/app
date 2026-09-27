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
)

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
    )
}

internal fun serverFlag(profile: ProfileItem): String {
    // Many sellers already put a flag at the start of the name; don't show two.
    val first = profile.remarks.codePointAt(0).takeIf { profile.remarks.isNotEmpty() }
    if (first != null && first in 0x1F1E6..0x1F1FF) return ""
    return ServerCountryManager.flag(profile.server).orEmpty()
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
