package net.drvpn.app.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.drvpn.app.R
import net.drvpn.app.handler.GeoIpLookup
import net.drvpn.app.ui.compose.cell
import net.drvpn.app.ui.compose.focusHighlight
import java.util.Locale

/**
 * Category chips above the server list: All, then each country (most servers first), then each
 * protocol. Hidden when there is nothing to choose between.
 */
@Composable
internal fun ServerCategoryBar(
    rows: List<ServerRowUiModel>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val countries = rows.mapNotNull { it.country.ifEmpty { null } }
        .groupingBy { it }.eachCount()
        .entries.sortedByDescending { it.value }
        .map { it.key }
    val protocols = rows.map { it.protocol }.distinct()
    if (selected == null && countries.size + protocols.size <= 1) return

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            CategoryChip(stringResource(R.string.server_category_all), selected == null, MaterialTheme.colorScheme.primary) {
                onSelect(null)
            }
        }
        items(countries, key = { "c:$it" }) { code ->
            val name = if (code == GeoIpLookup.CDN) "CDN" else Locale("", code).getDisplayCountry(locale)
            CategoryChip("${countryBadge(code)} $name".trim(), selected == "c:$code", MaterialTheme.colorScheme.primary) {
                onSelect("c:$code")
            }
        }
        items(protocols, key = { "p:$it" }) { protocol ->
            CategoryChip(protocol, selected == "p:$protocol", protocolColor(protocol)) {
                onSelect("p:$protocol")
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, isSelected: Boolean, color: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = if (isSelected) Color.White else color,
        maxLines = 1,
        modifier = Modifier
            .focusHighlight(shape)
            .clip(shape)
            .background(if (isSelected) color else MaterialTheme.colorScheme.cell)
            .semantics { this.selected = isSelected }
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}
