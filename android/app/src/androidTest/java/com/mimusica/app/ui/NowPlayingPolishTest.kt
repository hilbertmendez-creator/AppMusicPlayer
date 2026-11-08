package com.mimusica.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.mimusica.app.domain.model.Song
import com.mimusica.app.domain.player.PlaybackState
import com.mimusica.app.ui.player.NowPlayingScreen
import com.mimusica.app.ui.theme.MiMusicaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Round 4 polish of "Reproduciendo": volume level, a progress bar that is not a slider, key spacing, restart. */
class NowPlayingPolishTest {
    @get:Rule
    val compose = createComposeRule()

    private val playing = PlaybackState(
        currentSong = Song(
            id = "1", sourceId = "s1", title = "Besame Mucho", artist = "Luis Miguel",
            durationSeconds = 220, thumbnailUrl = null,
            hasSyncedLyrics = true, hasPlainLyrics = false, lyricsOffsetMs = 0, audioPath = "/a.m4a",
        ),
        isPlaying = true, positionMs = 65_000, durationMs = 220_000, hasNext = true, hasPrevious = true,
        volumeLevel = 6,
    )

    private fun show(state: PlaybackState = playing, onRestart: () -> Unit = {}) {
        compose.setContent {
            MiMusicaTheme {
                Box(Modifier.size(411.dp, 914.dp)) {
                    NowPlayingScreen(
                        state = state, onToggle = {}, onNext = {}, onPrevious = {},
                        onVolumeUp = {}, onVolumeDown = {}, onRestart = onRestart, onSing = {}, onBack = {},
                    )
                }
            }
        }
    }

    @Test
    fun volumeShowsItsLevelAsVisibleText() {
        show()
        // The two spaces inside "6 de 10" are non-breaking so the readout wraps as "Volumen" / "6 de 10".
        compose.onNodeWithText("Volumen 6 de 10").assertIsDisplayed()
    }

    @Test
    fun volumeSpeaksItsLevelAsAStateDescription() {
        show()
        compose.onNodeWithContentDescription("Volumen").assert(
            SemanticsMatcher("state description is 6 de 10") {
                it.config.getOrNull(SemanticsProperties.StateDescription) == "6 de 10"
            },
        )
    }

    @Test
    fun volumeFallsBackToTheBareWordWhileTheLevelIsUnknown() {
        show(playing.copy(volumeLevel = null))
        compose.onNodeWithText("Volumen").assertIsDisplayed()
    }

    @Test
    fun progressBarIsNotASlider() {
        show()
        compose.onNodeWithContentDescription("Avance de la canción").assert(
            SemanticsMatcher.keyNotDefined(SemanticsActions.SetProgress),
        )
    }

    @Test
    fun transportKeysAreAtLeast12dpApart() {
        show()
        val previous = compose.onNodeWithText("Anterior").getUnclippedBoundsInRoot()
        val toggle = compose.onNodeWithText("Pausar").getUnclippedBoundsInRoot()
        val next = compose.onNodeWithText("Siguiente").getUnclippedBoundsInRoot()
        assertTrue("gap before play/pause was ${toggle.left - previous.right}", toggle.left - previous.right >= 12.dp)
        assertTrue("gap after play/pause was ${next.left - toggle.right}", next.left - toggle.right >= 12.dp)
    }

    @Test
    fun fromTheBeginningIsALabelledButtonThatSeeksToStart() {
        var restarts = 0
        show(onRestart = { restarts++ })
        compose.onNodeWithText("Desde el principio").assertIsDisplayed().assertHeightIsAtLeast(64.dp).performClick()
        assertEquals(1, restarts)
    }
}
