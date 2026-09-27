package net.drvpn.app.ui.main

import net.drvpn.app.dto.ConnectionTestResult
import net.drvpn.app.dto.GroupMapItem
import net.drvpn.app.dto.LocateTarget

/** Locale-neutral state formatted only when it reaches the main UI. */
sealed interface MainStatus {
    data object Disconnected : MainStatus
    data object Connected : MainStatus
    data object Testing : MainStatus
    data class TestProgress(val progress: String) : MainStatus
    data class ConnectionTest(val result: ConnectionTestResult) : MainStatus
}

/**
 * Main UI state
 */
data class MainUiState(
    val groups: List<GroupMapItem> = emptyList(),
    val selectedGroupId: String = "",
    val selectedGuid: String? = null,
    val selectedServerName: String = "",
    val isRunning: Boolean = false,
    val isTesting: Boolean = false,
    val status: MainStatus = MainStatus.Disconnected,
    val locateTarget: LocateTarget? = null,
    val confirmRemove: Boolean = false,
    val doubleColumnDisplay: Boolean = false,
    val showServersWithoutPing: Boolean = false,
    /** Live traffic speed in bytes/s while connected. */
    val speedUp: Long = 0L,
    val speedDown: Long = 0L,
    /** Bytes transferred in the current connection. */
    val totalUp: Long = 0L,
    val totalDown: Long = 0L,
    /** Wall-clock time the current connection started, or null when disconnected. */
    val connectedSince: Long? = null,
    /** Subscription of the selected group (usage/expiry from the panel), if it is a subscription. */
    val subscription: net.drvpn.app.dto.entities.SubscriptionItem? = null,
    /** Owner announcement from notice.json, shown as a card on the home tab until dismissed. */
    val announcement: net.drvpn.app.handler.AnnouncementManager.Announcement? = null,
    val shareQRCodeBitmap: android.graphics.Bitmap? = null
)

/**
 * All possible user interaction intents
 */
sealed interface MainAction {
    data object Initialize : MainAction
    data object RefreshGroups : MainAction
    data object ToggleService : MainAction
    data object TestCurrentServer : MainAction
    data object TestAllServers : MainAction
    data object TestRealAllServers : MainAction
    data object CancelTesting : MainAction
    data object RemoveAllServers : MainAction
    data object RemoveDuplicateServers : MainAction
    data object RemoveInvalidServers : MainAction
    data object SortByTestResults : MainAction
    data object UpdateSubscriptions : MainAction
    data object ExportAll : MainAction

    data object ImportQRcode : MainAction
    data object ImportClipboard : MainAction
    data object ImportConfigLocal : MainAction
    data class ImportManually(val type: Int) : MainAction
    data object RestartService : MainAction
    data object LocateSelectedServer : MainAction

    data class SelectGroup(val groupId: String) : MainAction
    data class SelectServer(val guid: String) : MainAction
    data class RemoveServer(val guid: String) : MainAction
    data class EditServer(val guid: String, val profile: net.drvpn.app.dto.entities.ProfileItem) : MainAction
    data class Search(val query: String) : MainAction
    data class SetShowServersWithoutPing(val show: Boolean) : MainAction
    data object SelectFastest : MainAction
    data class DismissAnnouncement(val id: String) : MainAction
    data class ShareQRCode(val guid: String) : MainAction
    data class ShareClipboard(val guid: String) : MainAction
    data class ShareFullContent(val guid: String) : MainAction
    data object DismissQRCodeDialog : MainAction

    data class ImportBatchConfig(val configText: String) : MainAction

    data object LocateHandled : MainAction
}
