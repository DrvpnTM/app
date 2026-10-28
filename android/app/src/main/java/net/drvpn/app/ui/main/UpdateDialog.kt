package net.drvpn.app.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.drvpn.app.R
import net.drvpn.app.handler.AppUpdater

/**
 * "New version" dialog: Update now (downloads and opens the installer, with progress), Later, and the
 * download page in the browser for when the in-app download can't be used.
 */
@Composable
internal fun UpdateDialog(
    version: String,
    progress: Int?,
    failed: Boolean,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val downloading = progress != null
    AlertDialog(
        onDismissRequest = { if (!downloading) onLater() },
        icon = { Text("🚀", fontSize = 44.sp) },
        title = {
            Text(
                stringResource(R.string.update_title, version),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(
                        when {
                            failed -> R.string.update_failed
                            downloading -> R.string.update_downloading
                            else -> R.string.update_message
                        }
                    ),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (downloading) {
                    if (progress!! >= 0) {
                        LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp))
                        Text("$progress%", fontWeight = FontWeight.Bold)
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(8.dp))
                    }
                }
                if (!downloading) {
                    TextButton(onClick = { runCatching { uriHandler.openUri(AppUpdater.DOWNLOAD_PAGE) } }) {
                        Text("🔗 " + stringResource(R.string.update_download_link))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onUpdate, enabled = !downloading) {
                Text("⬇️ " + stringResource(if (failed) R.string.update_try_again else R.string.update_now), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onLater) {
                Text(stringResource(R.string.update_later))
            }
        },
    )
}
