package net.drvpn.app.ui.usage

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.drvpn.app.R
import net.drvpn.app.extension.toTrafficString
import net.drvpn.app.handler.UsageStats
import net.drvpn.app.ui.base.BaseComponentActivity
import net.drvpn.app.ui.compose.AppTopBar
import net.drvpn.app.ui.compose.IosBlue
import net.drvpn.app.ui.compose.IosGreen
import net.drvpn.app.ui.compose.IosGroupShape
import net.drvpn.app.ui.compose.PreferenceGroupHeader
import net.drvpn.app.ui.compose.cell
import java.text.SimpleDateFormat
import java.util.Locale

/** Drawer "Usage" screen: today, this month, and a 7-day chart of VPN traffic. */
class UsageActivity : BaseComponentActivity() {

    @Composable
    override fun ScreenContent() {
        val days = remember { UsageStats.lastDays(7) }
        val month = remember { UsageStats.thisMonth() }
        val today = days.last()

        Scaffold(
            contentWindowInsets = WindowInsets(0),
            topBar = { AppTopBar(title = stringResource(R.string.usage_title), onBackClick = { finish() }) }
        ) { innerPadding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TotalCard(stringResource(R.string.usage_today), today, Modifier.weight(1f))
                    TotalCard(stringResource(R.string.usage_month), month, Modifier.weight(1f))
                }

                PreferenceGroupHeader(stringResource(R.string.usage_last7))
                WeekChart(days)

                Text(
                    text = stringResource(R.string.usage_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun TotalCard(title: String, day: UsageStats.Day, modifier: Modifier) {
    Column(
        modifier
            .clip(IosGroupShape)
            .background(MaterialTheme.colorScheme.cell)
            .padding(14.dp)
    ) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(day.total.toTrafficString(), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))
        Text("↓ ${day.down.toTrafficString()}", style = MaterialTheme.typography.bodySmall, color = IosBlue)
        Text("↑ ${day.up.toTrafficString()}", style = MaterialTheme.typography.bodySmall, color = IosGreen)
    }
}

@Composable
private fun WeekChart(days: List<UsageStats.Day>) {
    val max = days.maxOf { it.total }.coerceAtLeast(1L)
    val dayFmt = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    val track = MaterialTheme.colorScheme.surfaceVariant
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(IosGroupShape)
            .background(MaterialTheme.colorScheme.cell)
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            days.forEach { day ->
                Column(Modifier.weight(1f)) {
                    Canvas(
                        Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    ) {
                        val radius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        drawRoundRect(color = track, size = size, cornerRadius = radius)
                        val h = size.height * (day.total.toFloat() / max)
                        if (h > 0f) {
                            drawRoundRect(
                                color = IosBlue,
                                topLeft = Offset(0f, size.height - h),
                                size = Size(size.width, h),
                                cornerRadius = radius
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        dayFmt.format(day.date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        if (day.total > 0) day.total.toTrafficString() else "–",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
