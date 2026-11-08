package com.mimusica.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.mimusica.app.data.settings.TextScale
import com.mimusica.app.domain.model.Song
import com.mimusica.app.domain.player.PlaybackState
import com.mimusica.app.ui.player.NowPlayingScreen
import com.mimusica.app.ui.theme.MiMusicaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NowPlayingScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val playing = PlaybackState(
        currentSong = Song(
            id = "1", sourceId = "s1", title = "Besame Mucho", artist = "Luis Miguel",
            durationSeconds = 220, thumbnailUrl = null,
            hasSyncedLyrics = true, hasPlainLyrics = false, lyricsOffsetMs = 0, audioPath = "/a.m4a",
        ),
        isPlaying = true, positionMs = 65_000, durationMs = 220_000, hasNext = true, hasPrevious = true,
    )

    /** The target phone, a Pixel 7a: 1080x2400 at 2.625 density is about 411x914dp. Narrower phones fall back to full-width rows. */
    private fun show(scale: TextScale, state: PlaybackState = playing) {
        compose.setContent {
            MiMusicaTheme(textScale = scale) {
                Box(Modifier.size(411.dp, 914.dp)) {
                    NowPlayingScreen(
                        state = state, onToggle = {}, onNext = {}, onPrevious = {},
                        onVolumeUp = {}, onVolumeDown = {}, onRestart = {}, onSing = {}, onBack = {},
                    )
                }
            }
        }
    }

    @Test
    fun everythingFitsAtNormalTextSize() {
        show(TextScale.NORMAL)
        listOf("Volver", "Besame Mucho", "Luis Miguel", "Volumen", "Cantar").forEach {
            compose.onNodeWithText(it).assertIsDisplayed()
        }
    }

    @Test
    fun singStaysReachableAtExtraLargeText() {
        show(TextScale.EXTRA_LARGE)
        compose.onNodeWithText("Volver").assertIsDisplayed()
        compose.onNodeWithText("Cantar").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun showsElapsedAndTotalAsText() {
        show(TextScale.NORMAL)
        compose.onNodeWithText("1:05 / 3:40").assertIsDisplayed()
    }

    @Test
    fun transportButtonsHaveVisibleLabelsAndBigTargets() {
        show(TextScale.NORMAL)
        listOf("Anterior", "Pausar", "Siguiente").forEach {
            compose.onNodeWithText(it).assertIsDisplayed().assertHeightIsAtLeast(64.dp)
        }
    }

    @Test
    fun transportLabelsShareOneBaseline() {
        show(TextScale.NORMAL)
        val bottoms = listOf("Anterior", "Pausar", "Siguiente").map {
            compose.onNodeWithText(it).getUnclippedBoundsInRoot().bottom.value
        }
        assertEquals(bottoms[0], bottoms[1], 0.5f)
        assertEquals(bottoms[0], bottoms[2], 0.5f)
    }

    @Test
    fun transportKeysAreEqualHeightCardsAndPlayPauseIsTheWidestOne() {
        show(TextScale.NORMAL)
        val bounds = listOf("Anterior", "Pausar", "Siguiente").map {
            compose.onNodeWithText(it).getUnclippedBoundsInRoot()
        }
        val widths = bounds.map { (it.right - it.left).value }
        val heights = bounds.map { (it.bottom - it.top).value }
        assertEquals(widths[0], widths[2], 0.5f)
        assert(widths[1] > widths[0]) { "play/pause should be wider than previous/next: $widths" }
        assertEquals(heights[0], heights[1], 0.5f)
        assertEquals(heights[0], heights[2], 0.5f)
        assert(heights[0] >= 112f) { "transport keys must be at least 112dp tall, were ${heights[0]}" }
    }

    @Test
    fun transportKeysBecomeFullWidthRowsWhenLabelsDoNotFit() {
        show(TextScale.EXTRA_LARGE)
        listOf("Pausar", "Siguiente", "Anterior").forEach {
            val b = compose.onNodeWithText(it).performScrollTo().getUnclippedBoundsInRoot()
            assert((b.right - b.left).value > 300f) { "$it should span the width, was ${(b.right - b.left).value}" }
        }
    }

    @Test
    fun volumeKeysHaveBigTargets() {
        show(TextScale.NORMAL)
        listOf("Bajar el volumen", "Subir el volumen").forEach {
            compose.onNodeWithContentDescription(it).assertIsDisplayed().assertHeightIsAtLeast(64.dp)
        }
    }

    @Test
    fun labelFollowsPlayPauseState() {
        show(TextScale.NORMAL, playing.copy(isPlaying = false))
        compose.onNodeWithText("Reproducir").assertIsDisplayed()
    }

    @Test
    fun saysWhatComesNextOnlyWhenThereIsSomething() {
        show(TextScale.NORMAL, playing.copy(nextTitle = "Cielito lindo"))
        compose.onNodeWithText("Sigue: Cielito lindo").assertIsDisplayed()
    }

    @Test
    fun noNextLineWithoutANextSong() {
        show(TextScale.NORMAL)
        compose.onAllNodesWithText("Sigue", substring = true).assertCountEquals(0)
    }

    @Test
    fun everythingStillReachableWithNextLineAtExtraLarge() {
        show(TextScale.EXTRA_LARGE, playing.copy(nextTitle = "Cielito lindo"))
        compose.onNodeWithText("Cantar").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Siguiente").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun singFitsAtNormalSizeWithTheNextLine() {
        show(TextScale.NORMAL, playing.copy(nextTitle = "Cielito lindo"))
        compose.onNodeWithText("Cantar").assertIsDisplayed()
    }

    @Test
    fun withoutLyricsSaysSoInsteadOfOfferingSing() {
        val noLyrics = playing.copy(
            currentSong = playing.currentSong!!.copy(hasSyncedLyrics = false, hasPlainLyrics = false),
        )
        show(TextScale.NORMAL, noLyrics)
        compose.onNodeWithText("Esta canción no tiene letra todavía.").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("Cantar").assertCountEquals(0)
    }

    @Test
    fun withLyricsOffersSingAndNoExplanation() {
        show(TextScale.NORMAL)
        compose.onNodeWithText("Cantar").assertIsDisplayed()
        compose.onAllNodesWithText("Esta canción no tiene letra todavía.").assertCountEquals(0)
    }
}
