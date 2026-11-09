package com.mimusica.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.mimusica.app.data.settings.TextScale
import com.mimusica.app.ui.components.ConfirmDeleteDialog
import com.mimusica.app.ui.home.HomeScreen
import com.mimusica.app.ui.home.HomeSummary
import com.mimusica.app.ui.playlists.NewPlaylistDialog
import com.mimusica.app.data.settings.FontChoice
import com.mimusica.app.ui.settings.AboutUpdateUi
import com.mimusica.app.ui.settings.AjustesScreen
import com.mimusica.app.ui.theme.MiMusicaTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Round 4 minor fixes that only show at the largest text sizes: nothing clipped, nothing unreachable. */
class LargeTextResilienceTest {
    @get:Rule
    val compose = createComposeRule()

    private fun layoutOf(node: SemanticsNodeInteraction): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        // fetchSemanticsNode synchronises with the UI thread, so it must not run inside runOnUiThread.
        val config = node.fetchSemanticsNode().config
        compose.runOnUiThread { config[SemanticsActions.GetTextLayoutResult].action!!(results) }
        return results.first()
    }

    /** The phone's font size at [osFontScale] (like Settings > Display > Font size), under the app's own theme. */
    private fun withOsFontScale(osFontScale: Float, content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, osFontScale)) { content() }
        }
    }

    // --- Home subtitles -------------------------------------------------------------------------------------

    @Test
    fun homeSubtitlesWrapInsteadOfBeingCutAtTheLargestTextSize() {
        withOsFontScale(1.3f) {
            MiMusicaTheme(textScale = TextScale.EXTRA_LARGE) {
                Box(Modifier.size(360.dp, 640.dp)) {
                    HomeScreen(
                        summary = HomeSummary(songs = 0, playlists = 0, singable = 0),
                        onContinue = {}, onSearch = {}, onSongs = {}, onPlaylists = {},
                    )
                }
            }
        }
        listOf("Todavía no tienes canciones", "Ninguna lista todavía").forEach {
            val subtitle = compose.onNodeWithText(it, useUnmergedTree = true).performScrollTo()
            assertFalse("\"$it\" must not be cut off", layoutOf(subtitle).hasVisualOverflow)
        }
    }

    // --- Dialogs scroll -------------------------------------------------------------------------------------

    @Test
    fun deleteDialogScrollsSoItsButtonsStayReachable() {
        withOsFontScale(2f) {
            MiMusicaTheme {
                ConfirmDeleteDialog(
                    message = "¿Quieres borrar “Besame Mucho”?",
                    note = "Podrás volver a descargarla desde Buscar.",
                    onConfirm = {}, onDismiss = {},
                )
            }
        }
        compose.onNode(hasScrollAction()).assertExists()
        compose.onNodeWithText("Sí, borrar").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun newListDialogScrollsSoItsFieldAndMicStayReachable() {
        withOsFontScale(2f) {
            MiMusicaTheme {
                NewPlaylistDialog(
                    name = "", micUnavailable = true, onNameChange = {}, onMic = {}, onCreate = {}, onCancel = {},
                )
            }
        }
        compose.onNode(hasScrollAction()).assertExists()
        compose.onNodeWithText("Hablar").performScrollTo().assertIsDisplayed()
    }

    // --- Ajustes: size samples follow the same cap as the real setting ---------------------------------------

    @Test
    fun everySizeSampleIsCappedAtTheCombinedScaleOfTheRealSetting() {
        withOsFontScale(2f) {
            MiMusicaTheme(textScale = TextScale.NORMAL) {
                AjustesScreen(
                    fontChoice = FontChoice.SYSTEM,
                    textScale = TextScale.NORMAL,
                    versionName = "0.1.0",
                    updateUi = AboutUpdateUi.Idle,
                    onSelectFont = {},
                    onSelectTextScale = {},
                    onCheckUpdate = {},
                    onInstallUpdate = {},
                    onCaregiver = {},
                    onBack = {},
                )
            }
        }
        TextScale.entries.forEach { scale ->
            val node = compose.onNodeWithTag("size_sample_${scale.name}", useUnmergedTree = true).performScrollTo()
            val result = layoutOf(node)
            val drawnSp = result.layoutInput.style.fontSize.value * result.layoutInput.density.fontScale
            // 22sp x the 1.6 combined cap is the most any option may be drawn at.
            assertTrue("sample ${scale.name} is drawn at $drawnSp sp", drawnSp <= 22f * 1.6f + 0.1f)
        }
    }
}
