package net.drvpn.app.fmt

import net.drvpn.app.AppConfig
import net.drvpn.app.dto.entities.ProfileItem
import net.drvpn.app.enums.EConfigType
import net.drvpn.app.enums.NetworkType
import net.drvpn.app.extension.idnHost
import net.drvpn.app.util.Utils
import java.net.URI

object TrojanFmt : FmtBase() {
    /**
     * Parses a Trojan URI string into a ProfileItem object.
     *
     * @param str the Trojan URI string to parse
     * @return the parsed ProfileItem object, or null if parsing fails
     */
    fun parse(str: String): ProfileItem {
        val config = ProfileItem.create(EConfigType.TROJAN)

        val uri = URI(Utils.fixIllegalUrl(str))
        config.remarks = Utils.decodeURIComponent(uri.fragment.orEmpty()).let { it.ifEmpty { "none" } }
        config.server = uri.idnHost
        config.serverPort = uri.port.toString()
        config.password = uri.userInfo

        if (uri.rawQuery.isNullOrEmpty()) {
            config.network = NetworkType.TCP.type
            config.security = AppConfig.TLS
            config.insecure = false
        } else {
            val queryParam = getQueryParam(uri)

            getItemFormQuery(config, queryParam)
            config.security = queryParam["security"] ?: AppConfig.TLS
        }

        return config
    }

    /**
     * Converts a ProfileItem object to a URI string.
     *
     * @param config the ProfileItem object to convert
     * @return the converted URI string
     */
    fun toUri(config: ProfileItem): String {
        val dicQuery = getQueryDic(config)

        return toUri(config, config.password, dicQuery)
    }
}