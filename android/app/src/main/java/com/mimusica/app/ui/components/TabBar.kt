package com.mimusica.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimusica.app.R
import com.mimusica.app.ui.nav.MainTab
import com.mimusica.app.ui.nav.tabLabelSizeSp
import com.mimusica.app.ui.theme.Ink
import com.mimusica.app.ui.theme.InkSoft
import com.mimusica.app.ui.theme.Line
import com.mimusica.app.ui.theme.Navy
import com.mimusica.app.ui.theme.Page
import com.mimusica.app.ui.theme.SectionSearch
import com.mimusica.app.ui.theme.SectionSettings
import com.mimusica.app.ui.theme.SectionSing
import com.mimusica.app.ui.theme.Sun

private class TabLook(val labelRes: Int, val icon: ImageVector, val color: Color)

private fun MainTab.look() = when (this) {
    MainTab.HOME -> TabLook(R.string.tab_home, Icons.Filled.Home, Navy)
    MainTab.SEARCH -> TabLook(R.string.tab_search, Icons.Filled.Search, SectionSearch)
    MainTab.SING -> TabLook(R.string.tab_sing, Icons.Filled.Lyrics, SectionSing)
    MainTab.SETTINGS -> TabLook(R.string.tab_settings, Icons.Filled.Tune, SectionSettings)
}

/**
 * The fixed bottom bar: four equal tabs, icon above a one-word label. The active tab sits on Sun with Ink text and
 * its icon in the section's colour. Not Material's NavigationBar, so every size here is ours: tabs are at least
 * 64dp tall and the label stops growing at 20sp (after every text scale) so four always fit on one line.
 * [selected] is null on screens that belong to no tab.
 */
@Composable
fun TabBar(selected: MainTab?, onSelect: (MainTab) -> Unit, modifier: Modifier = Modifier) {
    val groupName = stringResource(R.string.tabs_label)
    Column(modifier = modifier.fillMaxWidth().background(Page)) {
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Line))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .selectableGroup()
                .semantics { contentDescription = groupName },
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MainTab.entries.forEach { tab ->
                TabItem(tab, selected = tab == selected, onClick = { onSelect(tab) }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TabItem(tab: MainTab, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val look = tab.look()
    val label = stringResource(look.labelRes)
    val spoken = if (selected) stringResource(R.string.tab_current, label) else label
    val fontScale = LocalDensity.current.fontScale
    val labelSize = tabLabelSizeSp(MaterialTheme.typography.labelMedium.fontSize.value, fontScale)
    Column(
        modifier = modifier
            .heightIn(min = MinTouch)
            .background(if (selected) Sun else Color.Transparent, RoundedCornerShape(18.dp))
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 6.dp)
            .semantics(mergeDescendants = true) { contentDescription = spoken },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        // Decorative: the label names the tab.
        Icon(look.icon, contentDescription = null, modifier = Modifier.size(28.dp), tint = if (selected) look.color else InkSoft)
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontSize = labelSize.sp,
            // The style's own line height belongs to the unscaled size; use one that matches the capped size so the
            // text box is never shorter than its glyphs.
            lineHeight = (labelSize * 1.25f).sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Ink else InkSoft,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
        )
    }
}
