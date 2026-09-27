package net.drvpn.app.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.drvpn.app.R
import net.drvpn.app.handler.AppLog
import net.drvpn.app.ui.base.BaseComponentActivity
import net.drvpn.app.ui.compose.AppTopBar

/** Drawer "Log" screen: the last 10 minutes of Dr VPN activity. */
class AppLogActivity : BaseComponentActivity() {

    @Composable
    override fun ScreenContent() {
        AppLog.refresh()
        val lines by AppLog.lines.collectAsStateWithLifecycle()

        Scaffold(
            contentWindowInsets = WindowInsets(0),
            topBar = {
                AppTopBar(
                    title = stringResource(R.string.title_app_log),
                    onBackClick = { finish() },
                    actions = {
                        IconButton(onClick = { AppLog.clear() }) {
                            Icon(
                                painterResource(R.drawable.ic_delete_24dp),
                                contentDescription = stringResource(R.string.action_delete),
                            )
                        }
                    },
                )
            }
        ) { innerPadding ->
            if (lines.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.app_log_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(lines) { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
        }
    }
}
