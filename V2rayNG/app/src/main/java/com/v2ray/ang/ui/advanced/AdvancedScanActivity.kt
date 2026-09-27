package com.v2ray.ang.ui.advanced

import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedVisibility
import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.MmkvManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.TextField
import com.v2ray.ang.ui.compose.iosFieldColors
import com.v2ray.ang.ui.compose.IosGroupShape
import com.v2ray.ang.R
import com.v2ray.ang.core.LauncherManager
import com.v2ray.ang.extension.toastSuccess
import com.v2ray.ang.ui.base.BaseComponentActivity
import com.v2ray.ang.ui.compose.AppTopBar

class AdvancedScanActivity : BaseComponentActivity() {

    private val viewModel: AdvancedScanViewModel by viewModels()

    @Composable
    override fun ScreenContent() {
        val progress by viewModel.progress.collectAsStateWithLifecycle()
        val range by viewModel.range.collectAsStateWithLifecycle()
        val ports by viewModel.ports.collectAsStateWithLifecycle()
        val resumeIndex by viewModel.resumeIndex.collectAsStateWithLifecycle()
        val clipboard = LocalClipboardManager.current
        var showWarning by remember {
            mutableStateOf(!MmkvManager.decodeSettingsBool(AppConfig.PREF_DRVPN_ADVANCED, false))
        }

        if (showWarning) {
            AlertDialog(
                onDismissRequest = { finish() },
                title = { Text(getString(R.string.advanced_scan_title)) },
                text = { Text(getString(R.string.advanced_scan_warning)) },
                confirmButton = {
                    TextButton(onClick = {
                        MmkvManager.encodeSettings(AppConfig.PREF_DRVPN_ADVANCED, true)
                        showWarning = false
                    }) { Text(getString(R.string.advanced_scan_understood)) }
                },
                dismissButton = {
                    TextButton(onClick = { finish() }) { Text(getString(android.R.string.cancel)) }
                },
            )
        }

        Scaffold(
            contentWindowInsets = WindowInsets(0),
            topBar = {
                AppTopBar(
                    title = stringResourceSafe(),
                    onBackClick = { finish() },
                    isLoading = progress.running,
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                // Friendly headline; the technical range/port controls are hidden under "options".
                Text(
                    text = getString(
                        if (progress.running) R.string.advanced_scan_status_running
                        else R.string.advanced_scan_status_idle
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                var showOptions by remember { mutableStateOf(false) }
                TextButton(onClick = { showOptions = !showOptions }) {
                    Text(getString(R.string.advanced_scan_options))
                }
                AnimatedVisibility(visible = showOptions) {
                    Column {
                        TextField(
                            value = range,
                            onValueChange = viewModel::setRange,
                            label = { Text(getString(R.string.advanced_scan_range)) },
                            singleLine = true,
                            enabled = !progress.running,
                            colors = iosFieldColors(),
                            shape = IosGroupShape,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        TextField(
                            value = ports,
                            onValueChange = viewModel::setPorts,
                            label = { Text(getString(R.string.advanced_scan_ports)) },
                            singleLine = true,
                            enabled = !progress.running,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = iosFieldColors(),
                            shape = IosGroupShape,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = viewModel::useServerRange, enabled = !progress.running) {
                                Text(getString(R.string.advanced_scan_preset_server))
                            }
                            OutlinedButton(onClick = viewModel::useCloudflareRanges, enabled = !progress.running) {
                                Text(getString(R.string.advanced_scan_preset_cloudflare))
                            }
                            OutlinedButton(onClick = viewModel::useWorldRange, enabled = !progress.running) {
                                Text(getString(R.string.advanced_scan_preset_world))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (progress.running) {
                    OutlinedButton(onClick = viewModel::stopScan, modifier = Modifier.fillMaxWidth()) {
                        Text(getString(R.string.advanced_scan_stop))
                    }
                } else {
                    Button(onClick = viewModel::startScan, modifier = Modifier.fillMaxWidth()) {
                        Text(getString(if (resumeIndex > 0) R.string.advanced_scan_resume else R.string.advanced_scan_start))
                    }
                }
                if (progress.total > 0) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { (progress.done.toFloat() / progress.total.coerceAtLeast(1L)).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${progress.done}/${progress.total} · " +
                                getString(R.string.advanced_scan_found, progress.found.size),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(progress.found, key = { "${it.ip}:${it.port}" }) { r ->
                        val text = "${r.ip}:${r.port}"
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    clipboard.setText(AnnotatedString(text))
                                    toastSuccess(R.string.toast_success)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = text,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = "${r.latencyMs} ms",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                FilledTonalButton(onClick = {
                                    if (viewModel.applyToSelectedServer(r.ip, r.port)) {
                                        LauncherManager.restartService(this@AdvancedScanActivity)
                                    }
                                }) {
                                    Text(getString(R.string.advanced_scan_apply))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun stringResourceSafe(): String = getString(R.string.advanced_scan_title)
}
