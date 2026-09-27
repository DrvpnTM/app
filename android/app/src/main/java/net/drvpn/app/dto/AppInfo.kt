package net.drvpn.app.dto

data class AppInfo(
    val appName: String,
    val packageName: String,
    val isSystemApp: Boolean,
    var isSelected: Int
)