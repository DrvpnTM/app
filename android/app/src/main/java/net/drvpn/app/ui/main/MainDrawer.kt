package net.drvpn.app.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import net.drvpn.app.BuildConfig
import net.drvpn.app.ui.compose.SettingsGroup
import net.drvpn.app.ui.compose.SettingsMenuItem
import net.drvpn.app.ui.compose.IosBlue
import net.drvpn.app.ui.compose.IosIndigo
import net.drvpn.app.ui.compose.IosOrange
import net.drvpn.app.ui.compose.IosTeal
import net.drvpn.app.ui.compose.IosGreen
import net.drvpn.app.ui.compose.IosRed
import net.drvpn.app.ui.compose.IosPink
import net.drvpn.app.ui.compose.IosGray
import net.drvpn.app.R
import net.drvpn.app.ui.compose.AppDivider
import net.drvpn.app.ui.compose.verticalScrollbar

enum class MainDestination(@DrawableRes val iconRes: Int, @StringRes val labelRes: Int) {
    Subscriptions(R.drawable.ic_subscriptions_24dp, R.string.title_sub_setting),
    PerAppProxy(R.drawable.ic_per_apps_24dp, R.string.per_app_proxy_settings),
    Routing(R.drawable.ic_routing_24dp, R.string.routing_settings_title),
    UserAssets(R.drawable.ic_file_24dp, R.string.title_user_asset_setting),
    Settings(R.drawable.ic_settings_24dp, R.string.title_settings),
    RegionLanguage(R.drawable.ic_translate_24dp, R.string.onboarding_drawer_title),
    Advanced(R.drawable.ic_routing_24dp, R.string.advanced_scan_title),
    KillSwitch(R.drawable.ic_lock_24dp, R.string.kill_switch_title),
    AppLogView(R.drawable.ic_logcat_24dp, R.string.title_app_log),
    Promotion(R.drawable.ic_promotion_24dp, R.string.title_pref_promotion),
    Logcat(R.drawable.ic_logcat_24dp, R.string.title_logcat),
    CheckUpdate(R.drawable.ic_check_update_24dp, R.string.update_check_for_update),
    BackupRestore(R.drawable.ic_restore_24dp, R.string.title_configuration_backup_restore),
    About(R.drawable.ic_about_24dp, R.string.title_about)
}

private val primaryDrawerItems = listOf(
    MainDestination.Subscriptions,
    MainDestination.PerAppProxy,
    MainDestination.Routing,
    MainDestination.UserAssets,
    MainDestination.RegionLanguage,
    MainDestination.Advanced,
    MainDestination.KillSwitch,
    MainDestination.AppLogView,
    MainDestination.Settings
)

// The Promotion entry is not shown in Dr VPN.
private val drawerItems = primaryDrawerItems + listOf(
    MainDestination.CheckUpdate,
    MainDestination.BackupRestore,
    MainDestination.About
)

private fun MainDestination.badgeColor(): Color = when (this) {
    MainDestination.Subscriptions -> IosBlue
    MainDestination.PerAppProxy -> IosIndigo
    MainDestination.Routing -> IosOrange
    MainDestination.UserAssets -> IosTeal
    MainDestination.RegionLanguage -> IosGreen
    MainDestination.Advanced -> IosRed
    MainDestination.KillSwitch -> IosPink
    MainDestination.AppLogView -> IosGray
    MainDestination.Settings -> IosGray
    MainDestination.CheckUpdate -> IosBlue
    MainDestination.BackupRestore -> IosOrange
    MainDestination.About -> IosIndigo
    else -> IosBlue
}

@Composable
fun MainDrawerContent(drawerState: DrawerState, onNavigate: (MainDestination) -> Unit) {
    val drawerScrollState = rememberScrollState()

    ModalDrawerSheet(
        drawerState = drawerState,
        modifier = Modifier.fillMaxWidth(0.82f),
        drawerContainerColor = MaterialTheme.colorScheme.background,
        drawerShape = RoundedCornerShape(topEnd = 22.dp, bottomEnd = 22.dp)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(drawerScrollState)
                .verticalScrollbar(drawerScrollState)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.drvpn_logo),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            listOf(primaryDrawerItems, drawerItems.drop(primaryDrawerItems.size)).forEach { section ->
                Spacer(Modifier.height(14.dp))
                SettingsGroup(dividerInset = 60.dp) {
                    section.forEach { item ->
                        SettingsMenuItem(
                            icon = painterResource(item.iconRes),
                            title = stringResource(item.labelRes),
                            iconTint = item.badgeColor(),
                            onClick = { onNavigate(item) }
                        )
                    }
                }
            }
        }
    }
}
