package com.mimusica.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.mimusica.app.data.settings.TextScale
import com.mimusica.app.domain.model.DownloadState
import com.mimusica.app.domain.model.MatchType
import com.mimusica.app.domain.model.SearchResult
import com.mimusica.app.domain.model.Song
import com.mimusica.app.domain.player.PlaybackState
import com.mimusica.app.ui.player.NowPlayingScreen
import com.mimusica.app.ui.search.SearchItem
import com.mimusica.app.ui.search.SearchScreen
import com.mimusica.app.ui.search.SearchUiState
import com.mimusica.app.ui.sing.SingScreen
import com.mimusica.app.ui.songs.SongsScreen
import com.mimusica.app.ui.theme.MiMusicaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * TalkBack debt: repeated actions inside song cards must speak the song, the spoken name must START with the
 * visible label, covers inside labelled cards are decorative and the progress is a single focus stop.
 */
class TalkBackLabelsTest {
    @get:Rule
    val compose = createComposeRule()

    private val cover = "Portada de la canción"

    private val song = Song(
        id = "1", sourceId = "id", title = "Cielito Lindo", artist = "Quirino Mendoza", durationSeconds = 180,
        thumbnailUrl = null, hasSyncedLyrics = true, hasPlainLyrics = false, lyricsOffsetMs = 0, audioPath = "/a.m4a",
    )

    private fun searchItem(state: DownloadState) = SearchItem(
        SearchResult(
            "id", "Cielito Lindo", "Quirino Mendoza", null, 180, null, MatchType.TITLE,
            state == DownloadState.InLibrary,
        ),
        state,
        songId = if (state == DownloadState.InLibrary) "1" else null,
        song = if (state == DownloadState.InLibrary) song else null,
    )

    private fun showSearch(state: DownloadState) {
        compose.setContent {
            MiMusicaTheme {
                SearchScreen(
                    state = SearchUiState(hasSearched = true, items = listOf(searchItem(state))),
                    micMessage = false,
                    onQueryChange = {}, onSearch = {}, onMic = {}, onDownload = {}, onBack = {},
                    onPlayNow = {}, onAddToList = {},
                )
            }
        }
    }

    // --- Search ---------------------------------------------------------------------------------------------

    @Test
    fun downloadSpeaksTheSong() {
        showSearch(DownloadState.NotDownloaded)
        compose.onNodeWithContentDescription("Descargar Cielito Lindo").assertIsDisplayed()
        compose.onNodeWithText("Descargar").assertIsDisplayed()
    }

    @Test
    fun retrySpeaksTheSong() {
        showSearch(DownloadState.Failed)
        compose.onNodeWithContentDescription("Intentar otra vez con Cielito Lindo").assertIsDisplayed()
    }

    @Test
    fun playNowAndAddToListSpeakTheSong() {
        showSearch(DownloadState.InLibrary)
        compose.onNodeWithContentDescription("Reproducir ahora Cielito Lindo").assertIsDisplayed()
        compose.onNodeWithContentDescription("Agregar a una lista: Cielito Lindo").assertIsDisplayed()
    }

    @Test
    fun searchResultCoverIsDecorative() {
        showSearch(DownloadState.NotDownloaded)
        compose.onAllNodesWithContentDescription(cover, useUnmergedTree = true).assertCountEquals(0)
    }

    // --- Mis canciones --------------------------------------------------------------------------------------

    private fun showSongs() {
        compose.setContent {
            MiMusicaTheme {
                SongsScreen(
                    songs = listOf(song), error = null, onPlay = {}, onPlayAll = {}, onDelete = {},
                    onSearch = {}, onBack = {},
                )
            }
        }
    }

    @Test
    fun songActionsSpeakTheSong() {
        showSongs()
        compose.onNodeWithContentDescription("Reproducir Cielito Lindo").assertIsDisplayed()
        compose.onNodeWithContentDescription("Borrar Cielito Lindo").assertIsDisplayed()
    }

    @Test
    fun songCoverIsDecorative() {
        showSongs()
        compose.onAllNodesWithContentDescription(cover, useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun songRowHasNoSecondClickTargetDuplicatingPlay() {
        showSongs()
        // Volver, Reproducir, Borrar: the row itself is not clickable any more.
        compose.onAllNodes(hasClickAction()).assertCountEquals(3)
    }

    // --- Cantar ---------------------------------------------------------------------------------------------

    @Test
    fun singButtonSpeaksTheSongAndCoverIsDecorative() {
        compose.setContent {
            MiMusicaTheme { SingScreen(songs = listOf(song), onSing = {}, onSearch = {}, onBack = {}) }
        }
        compose.onNodeWithContentDescription("Cantar Cielito Lindo").assertIsDisplayed()
        compose.onAllNodesWithContentDescription(cover, useUnmergedTree = true).assertCountEquals(0)
    }

    // --- Reproduciendo --------------------------------------------------------------------------------------

    private fun showNowPlaying(height: Int = 807) {
        val playing = PlaybackState(
            currentSong = song, isPlaying = true, positionMs = 65_000, durationMs = 220_000,
            hasNext = true, hasPrevious = true,
        )
        compose.setContent {
            MiMusicaTheme(textScale = TextScale.NORMAL) {
                Box(Modifier.size(392.dp, height.dp)) {
                    NowPlayingScreen(
                        state = playing, onToggle = {}, onNext = {}, onPrevious = {},
                        onVolumeUp = {}, onVolumeDown = {}, onRestart = {}, onSing = {}, onBack = {},
                    )
                }
            }
        }
    }

    @Test
    fun progressIsASingleFocusStop() {
        showNowPlaying()
        // The bar speaks "Avance de la canción, 1:05 de 3:40" ...
        compose.onNodeWithContentDescription("Avance de la canción").assert(
            SemanticsMatcher("state description is 1:05 de 3:40") {
                it.config.getOrNull(SemanticsProperties.StateDescription) == "1:05 de 3:40"
            },
        )
        // ... and the visible "1:05 / 3:40" text is hidden from TalkBack instead of repeating it.
        compose.onNodeWithText("1:05 / 3:40").assert(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.InvisibleToUser),
        )
        compose.onAllNodesWithContentDescription("1:05 de 3:40").assertCountEquals(0)
    }

    @Test
    fun bigCoverOnNowPlayingKeepsItsDescription() {
        showNowPlaying(height = 1100)
        assertTrue(
            compose.onAllNodesWithContentDescription(cover, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty(),
        )
    }
}
