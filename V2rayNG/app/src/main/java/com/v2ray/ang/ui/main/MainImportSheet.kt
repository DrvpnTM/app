package com.v2ray.ang.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R
import com.v2ray.ang.ui.compose.IosBlue
import com.v2ray.ang.ui.compose.IosGray
import com.v2ray.ang.ui.compose.IosGreen
import com.v2ray.ang.ui.compose.IosIndigo
import com.v2ray.ang.ui.compose.IosOrange
import com.v2ray.ang.ui.compose.IosPink
import com.v2ray.ang.ui.compose.IosRed
import com.v2ray.ang.ui.compose.IosTeal
import com.v2ray.ang.ui.compose.PreferenceGroupHeader
import com.v2ray.ang.ui.compose.SettingsGroup
import com.v2ray.ang.ui.compose.SettingsMenuItem
import com.v2ray.ang.ui.compose.cell
import com.v2ray.ang.ui.compose.iosCaption

private val manualProtocols = listOf(
    ImportMenuAction.Vless to IosBlue,
    ImportMenuAction.Vmess to IosIndigo,
    ImportMenuAction.Trojan to IosRed,
    ImportMenuAction.Shadowsocks to IosGray,
    ImportMenuAction.Hysteria2 to IosOrange,
    ImportMenuAction.WireGuard to IosPink,
    ImportMenuAction.Socks to IosTeal,
    ImportMenuAction.Http to IosGreen,
)

private val advancedItems = listOf(
    ImportMenuAction.PolicyGroup to IosIndigo,
    ImportMenuAction.ProxyChain to IosOrange,
)

/** "Add VLESS [x]" style labels -> the bit inside the brackets, e.g. "VLESS". */
private fun shortLabel(label: String): String =
    Regex("\\[(.+)]").find(label)?.groupValues?.get(1)?.trim() ?: label

/**
 * iOS-style "Add server" sheet: three quick-import tiles (QR, clipboard, file), then grouped lists
 * for adding a server by hand and the advanced profile types.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportSheet(onDismiss: () -> Unit, onAction: (MainAction) -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = stringResource(R.string.import_sheet_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            Text(
                text = stringResource(R.string.import_sheet_hint),
                style = iosCaption,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickTile(R.drawable.ic_copy, stringResource(R.string.import_quick_clipboard), IosGreen, Modifier.weight(1f)) {
                    onAction(MainAction.ImportClipboard)
                }
                QuickTile(R.drawable.ic_scan_24dp, stringResource(R.string.import_quick_qr), IosBlue, Modifier.weight(1f)) {
                    onAction(MainAction.ImportQRcode)
                }
                QuickTile(R.drawable.ic_file_24dp, stringResource(R.string.import_quick_file), IosOrange, Modifier.weight(1f)) {
                    onAction(MainAction.ImportConfigLocal)
                }
            }

            PreferenceGroupHeader(stringResource(R.string.import_section_manual))
            SettingsGroup(dividerInset = 60.dp) {
                manualProtocols.forEach { (item, color) ->
                    SettingsMenuItem(
                        icon = painterResource(R.drawable.ic_lock_24dp),
                        title = shortLabel(stringResource(item.labelRes)),
                        iconTint = color,
                        onClick = { onAction(item.action) }
                    )
                }
            }

            PreferenceGroupHeader(stringResource(R.string.import_section_advanced))
            SettingsGroup(dividerInset = 60.dp) {
                advancedItems.forEach { (item, color) ->
                    SettingsMenuItem(
                        icon = painterResource(if (item == ImportMenuAction.ProxyChain) R.drawable.ic_routing_24dp else R.drawable.ic_per_apps_24dp),
                        title = shortLabel(stringResource(item.labelRes)),
                        iconTint = color,
                        onClick = { onAction(item.action) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickTile(iconRes: Int, label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.cell)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(iconRes), contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
