package net.drvpn.app.dto.entities

data class SubscriptionItem(
    var remarks: String = "",
    var url: String = "",
    var enabled: Boolean = true,
    val addedTime: Long = System.currentTimeMillis(),
    var lastUpdated: Long = -1,
    var autoUpdate: Boolean = true,
    var updateInterval: Long = 360, // in minutes; Dr VPN default: every 6 hours
    var prevProfile: String? = null,
    var nextProfile: String? = null,
    var filter: String? = null,
    var allowInsecureUrl: Boolean = false,
    var userAgent: String? = null,
    var requestHeaders: String? = null,
    /** From the panel's "subscription-userinfo" header: bytes used (up+down), bytes allowed, expiry (ms). -1 = unknown/unlimited. */
    var usedBytes: Long = -1,
    var totalBytes: Long = -1,
    var expireAt: Long = -1,
)

