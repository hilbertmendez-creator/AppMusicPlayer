package com.mimusica.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.mimusica.app.domain.model.Song
import com.mimusica.app.domain.player.PlaybackState
import com.mimusica.app.ui.player.NowPlayingScreen
import com.mimusica.app.ui.theme.MiMusicaTheme
import com.mimusica.app.ui.theme.Sun
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Polish of "Reproduciendo": a progress bar that is not a slider, key spacing, the sing button, no volume. */
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

    private fun show(state: PlaybackState = playing, onSing: (String) -> Unit = {}) {
        compose.setContent {
            MiMusicaTheme {
                Box(Modifier.size(411.dp, 914.dp)) {
                    NowPlayingScreen(
                        state = state, onToggle = {}, onNext = {}, onPrevious = {},
                        onSing = onSing, onBack = {},
                    )
                }
            }
        }
    }

    @Test
    fun volumeAndRestartControlsAreGoneEvenWhenTheLevelIsKnown() {
        show()
        compose.onAllNodesWithText("Volumen", substring = true).assertCountEquals(0)
        compose.onAllNodesWithText("Desde el principio").assertCountEquals(0)
    }

    @Test
    fun progressBarIsNotASlider() {
        show()
        compose.onNodeWithContentDescription("Avance de la canción").assert(
            SemanticsMatcher.keyNotDefined(SemanticsActions.SetProgress),
        )
    }

    @Test
    fun transportKeysAreAtLeast8dpApart() {
        show()
        val previous = compose.onNodeWithText("Anterior").getUnclippedBoundsInRoot()
        val toggle = compose.onNodeWithText("Pausar").getUnclippedBoundsInRoot()
        val next = compose.onNodeWithText("Siguiente").getUnclippedBoundsInRoot()
        assertTrue("gap before play/pause was ${toggle.left - previous.right}", toggle.left - previous.right >= 8.dp)
        assertTrue("gap after play/pause was ${next.left - toggle.right}", next.left - toggle.right >= 8.dp)
    }

    @Test
    fun transportKeysAre92dpTallAndPausarIsTheWidest() {
        show()
        val keys = listOf("Anterior", "Pausar", "Siguiente").map {
            compose.onNodeWithText(it).assertHeightIsAtLeast(92.dp).getUnclippedBoundsInRoot()
        }
        val widths = keys.map { it.right - it.left }
        assertTrue("Pausar must be the widest: $widths", widths[1] > widths[0] && widths[1] > widths[2])
    }

    @Test
    fun singButtonIsSunColouredAtLeast64dpAndCallsBackWithTheSong() {
        var sung: String? = null
        show(onSing = { sung = it })
        val sing = compose.onNodeWithText("Cantar con la letra").assertHeightIsAtLeast(64.dp)
        val image = sing.captureToImage().toPixelMap()
        // 10px in from the left edge, mid-height: inside the button's padding, so it is the container colour.
        val pixel = image[10, image.height / 2]
        assertTrue("sing button should be Sun, was $pixel", abs(pixel.red - Sun.red) < 0.02f && abs(pixel.green - Sun.green) < 0.02f && abs(pixel.blue - Sun.blue) < 0.02f)
        sing.performClick()
        assertEquals("1", sung)
    }
}
