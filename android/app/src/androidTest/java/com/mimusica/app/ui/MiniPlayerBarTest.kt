package com.mimusica.app.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.mimusica.app.ui.components.MiniPlayerBar
import com.mimusica.app.ui.components.MiniPlayerUi
import com.mimusica.app.ui.theme.MiMusicaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MiniPlayerBarTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(state: MiniPlayerUi, onOpen: () -> Unit = {}, onToggle: () -> Unit = {}) {
        compose.setContent { MiMusicaTheme { MiniPlayerBar(state, onOpen = onOpen, onToggle = onToggle) } }
    }

    private val playing = MiniPlayerUi("Volver", "Carlos Gardel", isPlaying = true)
    private val openPlaying = "Abrir reproductor: Volver, sonando"

    @Test
    fun showsTheTitleAndSaysItIsPlaying() {
        show(playing)
        compose.onNodeWithText("Volver").assertIsDisplayed()
        compose.onNodeWithText("Sonando").assertIsDisplayed()
    }

    @Test
    fun saysPausedWhenPaused() {
        show(playing.copy(isPlaying = false))
        compose.onNodeWithText("En pausa").assertIsDisplayed()
        compose.onNodeWithContentDescription("Abrir reproductor: Volver, en pausa").assertIsDisplayed()
    }

    @Test
    fun openAreaAndToggleAreSeparateLabelledNodes() {
        show(playing)
        compose.onNodeWithContentDescription(openPlaying).assertIsDisplayed()
        compose.onNodeWithContentDescription("Pausar").assertIsDisplayed()
    }

    @Test
    fun theToggleSaysSeguirWhenPaused() {
        show(playing.copy(isPlaying = false))
        compose.onNodeWithContentDescription("Seguir").assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Pausar").assertCountEquals(0)
    }

    @Test
    fun theToggleIsAtLeast64dpEachWay() {
        show(playing)
        compose.onNodeWithContentDescription("Pausar").assertHeightIsAtLeast(64.dp).assertWidthIsAtLeast(64.dp)
    }

    @Test
    fun theOpenAreaIsAtLeast64dpTall() {
        show(playing)
        compose.onNodeWithContentDescription(openPlaying).assertHeightIsAtLeast(64.dp)
    }

    @Test
    fun noClickableNodeContainsTheToggle() {
        show(playing)
        compose.onAllNodes(hasClickAction() and hasAnyDescendant(hasContentDescription("Pausar")))
            .assertCountEquals(0)
    }

    @Test
    fun openAndToggleFireTheirOwnCallbacksOnly() {
        var opened = 0
        var toggled = 0
        show(playing, onOpen = { opened++ }, onToggle = { toggled++ })
        compose.onNodeWithContentDescription(openPlaying).performClick()
        assertEquals(1 to 0, opened to toggled)
        compose.onNodeWithContentDescription("Pausar").performClick()
        assertEquals(1 to 1, opened to toggled)
    }

    @Test
    fun theCoverIsDecorative() {
        show(playing)
        compose.onAllNodesWithContentDescription("Portada de la canción", useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodesWithText("Sonando ahora").assertCountEquals(0)
    }

    @Test
    fun theOpenAreaIsAButtonForTalkBack() {
        show(playing)
        compose.onNodeWithContentDescription(openPlaying).assert(
            SemanticsMatcher("has a click action") { it.config.contains(SemanticsActions.OnClick) },
        )
        compose.onNodeWithContentDescription(openPlaying).assert(
            SemanticsMatcher("role is Button") {
                it.config.getOrNull(SemanticsProperties.Role)?.toString() == "Button"
            },
        )
    }
}
