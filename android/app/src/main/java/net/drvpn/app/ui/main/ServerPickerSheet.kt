package net.drvpn.app.ui.main

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.drvpn.app.R
import net.drvpn.app.ui.compose.cell
import net.drvpn.app.ui.compose.sheetDragHandle


/**
 * "Select server" sheet opened by pulling Home up: a world map with a dot on every server country
 * (the selected one glowing), and the servers fastest first.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ServerPickerSheet(
    rows: List<ServerRowUiModel>,
    selectedGuid: String?,
    onSelect: (String) -> Unit,
    onSelectFastest: () -> Unit,
    testing: Boolean,
    onDismiss: () -> Unit,
) {
    val sorted = rows.sortedWith(serverPingOrder)
    val fast = sorted.take(3).takeWhile { it.testDelayMillis > 0L }.size
    val selectedCountry = rows.firstOrNull { it.guid == selectedGuid }?.country
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = sheetDragHandle(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        run {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.server_picker_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 4.dp, bottom = 12.dp)
                )
                WorldMap(
                    countries = rows.map { it.country }.filter { it.length == 2 }.toSet(),
                    selectedCountry = selectedCountry,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
                    FastestButton(busy = testing, onClick = {
                        onSelectFastest()
                        onDismiss()
                    })
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((LocalConfiguration.current.screenHeightDp * 0.5f).dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
                ) {
                    itemsIndexed(sorted, key = { _, row -> row.guid }) { index, row ->
                        ServerLocationRow(
                            row = row,
                            isSelected = row.guid == selectedGuid,
                            fast = index < fast,
                            onClick = {
                                onSelect(row.guid)
                                onDismiss()
                            },
                            onMore = null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorldMap(countries: Set<String>, selectedCountry: String?, modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition(label = "mapPulse")
    val glow by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "glow"
    )
    val marker = MaterialTheme.colorScheme.primary
    val ink = MaterialTheme.colorScheme.onSurface
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.cell)
            .aspectRatio(WORLD_MAP_ASPECT)
    ) {
        Image(
            painterResource(R.drawable.world_map),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            colorFilter = ColorFilter.tint(ink.copy(alpha = 0.10f)),
            modifier = Modifier.fillMaxSize()
        )
        for (code in countries) {
            val (fx, fy) = WORLD_MAP_POSITIONS[code] ?: continue
            val selected = code == selectedCountry
            val size = if (selected) 10.dp else 6.dp
            Box(
                Modifier
                    .offset(x = maxWidth * fx - size / 2, y = maxHeight * fy - size / 2)
                    .size(size)
            ) {
                if (selected) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .scale(glow)
                            .clip(CircleShape)
                            .background(marker.copy(alpha = 0.25f))
                    )
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(if (selected) marker else marker.copy(alpha = 0.55f))
                )
            }
        }
    }
}
