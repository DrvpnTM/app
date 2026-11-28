package net.drvpn.app.dto

import java.io.Serializable

data class TestServiceMessage(
    val key: Int,
    val subscriptionId: String = "",
    val serverGuids: List<String> = emptyList(),
    val onlyTcp: Boolean = false,
    /** Override the delay-test URL for this run (used by the service-unlock menu). */
    val probeUrl: String? = null
) : Serializable

