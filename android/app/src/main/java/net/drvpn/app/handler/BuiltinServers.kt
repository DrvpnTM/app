package net.drvpn.app.handler

import android.content.Context
import net.drvpn.app.AppConfig
import net.drvpn.app.dto.entities.ProfileItem
import net.drvpn.app.enums.EConfigType
import net.drvpn.app.util.LogUtil
import net.drvpn.app.util.Utils

/**
 * Servers the app brings along, added once to "My servers" (the default group). A user who
 * deletes one does not get it back.
 *
 * - "Dr Serverless": needs no server. It goes to sites directly, splitting the TLS ClientHello into
 *   small pieces (fragment) and resolving names over DoH, which gets past filtering that only reads
 *   the SNI or poisons DNS (assets/serverless.json). QUIC (UDP 443) is blocked so apps fall back to
 *   TCP, which can be fragmented. A custom config without inbounds; CoreConfigManager adds the app's.
 * - "Dr Open VPN": RiseupVPN over OpenVPN (RiseupManager, OpenVpnSession), a connection of its own
 *   instead of Xray.
 */
object BuiltinServers {
    const val SERVERLESS_GUID = "__dr_serverless__"
    const val OPENVPN_GUID = "__dr_openvpn__"

    private const val PREF_SERVERLESS_ADDED = "pref_drvpn_serverless_added_v1"
    private const val PREF_OPENVPN_ADDED = "pref_drvpn_openvpn_added_v1"

    /**
     * Never picked as "fastest" or switched to automatically: Dr Open VPN's ping is a bare TCP
     * connect (always lower than a real ping, even where OpenVPN is blocked) and it cannot run as
     * an Xray reload; Dr Serverless only gets past SNI filtering, not every block.
     */
    fun isBuiltin(guid: String?): Boolean = guid == SERVERLESS_GUID || guid == OPENVPN_GUID

    fun ensure(context: Context) {
        addOnce(PREF_SERVERLESS_ADDED, SERVERLESS_GUID) {
            val raw = Utils.readTextFromAssets(context, "serverless.json")
            if (raw.isBlank()) return@addOnce null
            ProfileItem.create(EConfigType.CUSTOM).apply { remarks = "Dr Serverless" } to raw
        }
        addOnce(PREF_OPENVPN_ADDED, OPENVPN_GUID) {
            ProfileItem.create(EConfigType.OPENVPN).apply { remarks = "Dr Open VPN" } to null
        }
    }

    /** [make] gives the profile and, for a custom config, its raw JSON. */
    private fun addOnce(pref: String, guid: String, make: () -> Pair<ProfileItem, String?>?) {
        if (MmkvManager.decodeSettingsBool(pref, false)) return
        try {
            if (MmkvManager.decodeServerConfig(guid) == null) {
                val (profile, raw) = make() ?: return
                MmkvManager.encodeServerConfig(guid, profile)
                raw?.let { MmkvManager.encodeServerRaw(guid, it) }
                // encodeServerConfig puts new servers first; these go last, after the user's own.
                val subId = AppConfig.DEFAULT_SUBSCRIPTION_ID
                val list = MmkvManager.decodeServerList(subId)
                if (list.remove(guid)) {
                    list.add(guid)
                    MmkvManager.encodeServerList(list, subId)
                }
            }
            MmkvManager.encodeSettings(pref, true)
            LogUtil.i(AppConfig.TAG, "Built-in server $guid added")
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Built-in server $guid could not be added", e)
        }
    }
}
