package com.mimusica.app.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import androidx.compose.ui.test.onNodeWithText
import com.mimusica.app.domain.model.LyricLine
import com.mimusica.app.domain.model.Song
import com.mimusica.app.ui.karaoke.KaraokeMode
import com.mimusica.app.ui.karaoke.KaraokeScreen
import com.mimusica.app.ui.karaoke.KaraokeUiState
import com.mimusica.app.ui.theme.MiMusicaTheme
import org.junit.Rule
import org.junit.Test

class KaraokeScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val song = Song(
        id = "1", sourceId = "s1", title = "Besame Mucho", artist = "Luis Miguel",
        durationSeconds = 180, thumbnailUrl = null,
        hasSyncedLyrics = true, hasPlainLyrics = false, lyricsOffsetMs = 0, audioPath = "/a.m4a",
    )
    private val longLine = "Cielito lindo, los que vienen de allá, cantando y no llorando, ay ay ay ay, " +
        "porque cantando se alegran, cielito lindo, los corazones"

    private fun show(state: KaraokeUiState, onRestart: () -> Unit = {}) {
        compose.setContent {
            MiMusicaTheme {
                KaraokeScreen(
                    state = state, textScale = 1f, onToggle = {}, onRestart = onRestart, onBack = {},
                )
            }
        }
    }

    private fun synced() = KaraokeUiState(
        mode = KaraokeMode.SYNCED,
        song = song,
        lines = listOf(LyricLine(0, "Primera"), LyricLine(1000, longLine), LyricLine(2000, "Tercera")),
        currentIndex = 1,
        isPlaying = true,
    )

    @Test
    fun songTitleIsAHeading() {
        show(synced())
        compose.onNodeWithText("Besame Mucho · Luis Miguel")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun currentLineIsReadableTextWithoutALiveRegion() {
        show(synced())
        compose.onNodeWithText(longLine).assertIsDisplayed()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion))
    }

    @Test
    fun pauseKeyIsALabelledBigButton() {
        show(synced())
        compose.onNodeWithText("Pausar").assertIsDisplayed().assertHeightIsAtLeast(96.dp)
    }

    @Test
    fun pausedShowsPlayInsteadOfPause() {
        show(synced().copy(isPlaying = false))
        compose.onNodeWithText("Reproducir").assertIsDisplayed()
        compose.onNodeWithText("Pausar").assertDoesNotExist()
    }

    @Test
    fun restartSeeksToTheBeginning() {
        var restarts = 0
        show(synced(), onRestart = { restarts++ })
        compose.onNodeWithText("Empezar de nuevo").assertIsDisplayed().assertHeightIsAtLeast(64.dp).performClick()
        assertEquals(1, restarts)
    }

    @Test
    fun plainLyricsSayTheyDoNotAdvanceByThemselves() {
        show(KaraokeUiState(mode = KaraokeMode.PLAIN, song = song, plain = "la la la", isPlaying = true))
        compose.onNodeWithText("La letra no avanza sola. Léela a tu ritmo.").assertIsDisplayed()
        compose.onNodeWithText("la la la").assertIsDisplayed()
    }

    @Test
    fun syncedLyricsShowNoPlainNotice() {
        show(synced())
        compose.onNodeWithText("La letra no avanza sola. Léela a tu ritmo.").assertDoesNotExist()
    }
}
