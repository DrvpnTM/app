package net.drvpn.app.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.drvpn.app.R
import net.drvpn.app.handler.PhoneLinkServer
import net.drvpn.app.util.QRCodeDecoder

/**
 * TV "Send from phone" dialog: shows a QR code of a one-time local page; the link the user sends from
 * their phone is passed to [onLink] and the dialog closes.
 */
@Composable
fun PhoneLinkDialog(onLink: (String) -> Unit, onDismiss: () -> Unit) {
    val latestOnLink by rememberUpdatedState(onLink)
    val latestDismiss by rememberUpdatedState(onDismiss)
    var url by remember { mutableStateOf<String?>(null) }
    var failed by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val server = PhoneLinkServer { link ->
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                latestOnLink(link)
                latestDismiss()
            }
        }
        url = runCatching { server.start() }.getOrNull()
        failed = url == null
        onDispose { server.stop() }
    }

    val qr = remember(url) { url?.let { QRCodeDecoder.createQRCode(it, 600) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.phone_link_title)) },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (failed) {
                    Text(stringResource(R.string.phone_link_no_network), textAlign = TextAlign.Center)
                } else {
                    Text(
                        stringResource(R.string.phone_link_hint),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    if (qr != null) {
                        Image(
                            bitmap = qr.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(8.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(url.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.phone_link_waiting),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
        containerColor = MaterialTheme.colorScheme.cell
    )
}
