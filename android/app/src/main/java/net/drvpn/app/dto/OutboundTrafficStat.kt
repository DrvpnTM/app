package net.drvpn.app.dto

data class OutboundTrafficStat(
    val tag: String,
    val direction: String,
    val value: Long,
)