package com.mimusica.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.mimusica.app.data.settings.TextScale
import com.mimusica.app.ui.components.TabBar
import com.mimusica.app.ui.nav.MainTab
import com.mimusica.app.ui.theme.MiMusicaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TabBarTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(selected: MainTab? = MainTab.HOME, scale: TextScale = TextScale.NORMAL, osFontScale: Float = 1f, onSelect: (MainTab) -> Unit = {}) {
        compose.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, osFontScale)) {
                MiMusicaTheme(textScale = scale) {
                    Box(Modifier.size(411.dp, 200.dp)) { TabBar(selected = selected, onSelect = onSelect) }
                }
            }
        }
    }

    private val names = listOf("Inicio", "Buscar", "Cantar", "Ajustes")

    @Test
    fun showsTheFourTabsByName() {
        show()
        names.forEach { compose.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test
    fun theSelectedTabIsAnnouncedAndSelected() {
        show(selected = MainTab.SING)
        compose.onNodeWithContentDescription("Cantar, pestaña actual").assertIsSelected()
        compose.onNodeWithContentDescription("Inicio").assertIsNotSelected()
        compose.onAllNodesWithContentDescription("Inicio, pestaña actual").assertCountEquals(0)
    }

    @Test
    fun noTabIsSelectedOnAScreenThatBelongsToNone() {
        show(selected = null)
        names.forEach { compose.onNodeWithContentDescription(it).assertIsNotSelected() }
    }

    @Test
    fun tabsAreTabsForTalkBack() {
        show()
        compose.onNodeWithContentDescription("Buscar").assert(
            SemanticsMatcher("role is Tab") {
                it.config.getOrNull(SemanticsProperties.Role)?.toString() == "Tab"
            },
        )
    }

    @Test
    fun eachTabIsAtLeast64dpTallAndTheyShareTheWidth() {
        show()
        val widths = names.map {
            // substring: the selected tab is announced as "<name>, pestaña actual".
            compose.onNodeWithContentDescription(it, substring = true).assertHeightIsAtLeast(64.dp)
            val b = compose.onNodeWithContentDescription(it, substring = true).getUnclippedBoundsInRoot()
            (b.right - b.left).value
        }
        widths.forEach { assertEquals(widths[0], it, 0.5f) }
    }

    @Test
    fun clickingATabReportsIt() {
        val picked = mutableListOf<MainTab>()
        show(onSelect = { picked += it })
        compose.onNodeWithText("Ajustes").performClick()
        compose.onNodeWithText("Buscar").performClick()
        assertEquals(listOf(MainTab.SETTINGS, MainTab.SEARCH), picked)
    }

    @Test
    fun fourLabelsFitWithoutEllipsisAtTheLargestTextScale() {
        show(scale = TextScale.EXTRA_LARGE, osFontScale = 1.3f)
        names.forEach { name ->
            val node = compose.onNodeWithText(name, useUnmergedTree = true)
            val results = mutableListOf<TextLayoutResult>()
            val config = node.fetchSemanticsNode().config
            compose.runOnUiThread { config[SemanticsActions.GetTextLayoutResult].action!!(results) }
            val r = results.first()
            // Width is what decides whether four labels fit on one row. (hasVisualOverflow also reports a 3px
            // line-box difference in height that never clips a glyph, so it is not used here.)
            val tab = compose.onNodeWithContentDescription(name, substring = true).fetchSemanticsNode().size.width
            val line = r.multiParagraph.getLineRight(0) - r.multiParagraph.getLineLeft(0)
            assertTrue("\"$name\" is $line px wide in a tab $tab px wide", line <= tab)
            assertEquals("\"$name\" must stay on one line", 1, results.first().lineCount)
        }
        val tops = names.map { compose.onNodeWithText(it).getUnclippedBoundsInRoot().top.value }
        assertTrue("labels share a row", tops.all { kotlin.math.abs(it - tops[0]) < 1f })
    }
}
