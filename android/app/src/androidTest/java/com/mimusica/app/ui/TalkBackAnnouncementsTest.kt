package com.mimusica.app.ui

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.mimusica.app.data.settings.TextScale
import com.mimusica.app.domain.model.LyricLine
import com.mimusica.app.domain.model.Playlist
import com.mimusica.app.domain.model.Song
import com.mimusica.app.domain.player.PlaybackState
import com.mimusica.app.ui.home.HomeScreen
import com.mimusica.app.ui.karaoke.KaraokeMode
import com.mimusica.app.ui.karaoke.KaraokeScreen
import com.mimusica.app.ui.karaoke.KaraokeUiState
import com.mimusica.app.ui.player.NowPlayingScreen
import com.mimusica.app.ui.playlists.PlaylistDetailScreen
import com.mimusica.app.ui.search.SearchScreen
import com.mimusica.app.ui.search.SearchUiState
import com.mimusica.app.data.settings.FontChoice
import com.mimusica.app.ui.settings.AboutUpdateUi
import com.mimusica.app.ui.settings.AjustesScreen
import com.mimusica.app.ui.songs.SongsScreen
import com.mimusica.app.ui.theme.MiMusicaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Round 4 TalkBack work: state changes that happen without a focus change are announced politely, every
 * Surface used as a button says it is a button, and repeated rows name the song they act on.
 */
class TalkBackAnnouncementsTest {
    @get:Rule
    val compose = createComposeRule()

    private val polite = SemanticsMatcher("polite live region") {
        it.config.getOrNull(SemanticsProperties.LiveRegion) == LiveRegionMode.Polite
    }
    private val isButton = SemanticsMatcher("role is Button") {
        it.config.getOrNull(SemanticsProperties.Role) == Role.Button
    }

    private val song = Song(
        id = "1", sourceId = "id", title = "Cielito Lindo", artist = "Quirino Mendoza", durationSeconds = 180,
        thumbnailUrl = null, hasSyncedLyrics = true, hasPlainLyrics = false, lyricsOffsetMs = 0, audioPath = "/a.m4a",
    )

    // --- Now playing ----------------------------------------------------------------------------------------

    private fun showNowPlaying() {
        val playing = PlaybackState(
            currentSong = song, isPlaying = true, positionMs = 65_000, durationMs = 220_000,
            hasNext = true, hasPrevious = true,
        )
        compose.setContent {
            MiMusicaTheme {
                NowPlayingScreen(
                    state = playing, onToggle = {}, onNext = {}, onPrevious = {},
                    onSing = {}, onBack = {},
                )
            }
        }
    }

    @Test
    fun aSongChangeIsAnnouncedPolitelyAsTitleAndArtist() {
        showNowPlaying()
        compose.onNodeWithText("Cielito Lindo").assert(polite)
        compose.onNodeWithText("Quirino Mendoza").assert(polite)
    }

    @Test
    fun theToggleKeyAnnouncesPlayPauseChanges() {
        showNowPlaying()
        compose.onNodeWithText("Pausar").assert(polite).assert(isButton)
    }

    @Test
    fun transportKeysAreButtons() {
        showNowPlaying()
        compose.onNodeWithText("Siguiente").assert(isButton)
        compose.onNodeWithText("Anterior").assert(isButton)
    }

    @Test
    fun noVolumeKeysRemainOnTheScreen() {
        showNowPlaying()
        compose.onAllNodesWithContentDescription("Subir el volumen").assertCountEquals(0)
        compose.onAllNodesWithContentDescription("Bajar el volumen").assertCountEquals(0)
    }

    // --- Home -----------------------------------------------------------------------------------------------

    @Test
    fun homeHeroAndRowsAreButtons() {
        compose.setContent {
            MiMusicaTheme {
                HomeScreen(
                    summary = com.mimusica.app.ui.home.HomeSummary(songs = 1, playlists = 1, singable = 1, continueSong = song),
                    onContinue = {}, onSearch = {}, onSongs = {}, onPlaylists = {},
                )
            }
        }
        compose.onNodeWithContentDescription("Seguir cantando: Cielito Lindo").assertExists()
        listOf("Mis canciones", "Mis listas").forEach {
            compose.onNodeWithText(it).assert(isButton)
        }
    }

    // --- Search ---------------------------------------------------------------------------------------------

    private fun showSearch(state: SearchUiState) {
        compose.setContent {
            MiMusicaTheme {
                SearchScreen(
                    state = state, micMessage = false,
                    onQueryChange = {}, onSearch = {}, onMic = {}, onDownload = {}, onBack = {},
                    onPlayNow = {}, onAddToList = {},
                )
            }
        }
    }

    @Test
    fun searchingIsOnePoliteFocusStopWithoutAStrayProgressNode() {
        showSearch(SearchUiState(isSearching = true))
        compose.onNodeWithText("Buscando…").assert(polite)
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assertCountEquals(0)
    }

    @Test
    fun anEmptyResultIsAnnounced() {
        showSearch(SearchUiState(hasSearched = true))
        compose.onNodeWithText("No encontré nada. Prueba con otras palabras.").assert(polite)
    }

    @Test
    fun theSearchFieldKeepsAVisibleLabel() {
        showSearch(SearchUiState())
        compose.onNodeWithText("Canción, cantante o letra").assertIsDisplayed()
    }

    // --- Karaoke --------------------------------------------------------------------------------------------

    private fun showKaraoke(state: KaraokeUiState) {
        compose.setContent {
            MiMusicaTheme {
                KaraokeScreen(state = state, textScale = 1f, onToggle = {}, onRestart = {}, onBack = {})
            }
        }
    }

    @Test
    fun karaokeLoadingAndNoLyricsAreAnnounced() {
        showKaraoke(KaraokeUiState(mode = KaraokeMode.LOADING))
        compose.onNodeWithText("Cargando letra…").assert(polite)
    }

    @Test
    fun karaokeNoLyricsIsAnnounced() {
        showKaraoke(KaraokeUiState(mode = KaraokeMode.NONE, song = song))
        compose.onNodeWithText("Esta canción todavía no tiene letra").assert(polite)
    }

    @Test
    fun syncedLinesAreStillNotLive() {
        showKaraoke(
            KaraokeUiState(
                mode = KaraokeMode.SYNCED, song = song, currentIndex = 0,
                lines = listOf(LyricLine(0, "Primera"), LyricLine(1000, "Segunda")),
            ),
        )
        compose.onNodeWithText("Primera").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion))
    }

    // --- Playlist detail ------------------------------------------------------------------------------------

    private fun showPlaylistDetail() {
        compose.setContent {
            MiMusicaTheme {
                PlaylistDetailScreen(
                    playlist = Playlist("p1", "Favoritas", listOf("1")), songs = listOf(song), error = null,
                    onPlayAll = {}, onPlaySong = {}, onAddSongs = {}, onRemoveSong = {}, onDeletePlaylist = {},
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun playlistRowActionsNameTheSongAndStartWithTheVisibleLabel() {
        showPlaylistDetail()
        // Visible "Reproducir" / "Quitar"; the spoken name starts with it and then says which song.
        compose.onNodeWithContentDescription("Reproducir Cielito Lindo").assertIsDisplayed()
        compose.onNodeWithContentDescription("Quitar Cielito Lindo").assertIsDisplayed()
    }

    @Test
    fun playlistRowTitleAndArtistAreOneSpokenUnit() {
        showPlaylistDetail()
        // Merged into a single node: the same node carries both texts.
        val texts = compose.onNodeWithText("Cielito Lindo").fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }
        assertEquals(listOf("Cielito Lindo", "Quirino Mendoza"), texts)
    }

    // --- Mis canciones: delete confirmation -----------------------------------------------------------------

    @Test
    fun aCompletedDeleteIsConfirmedPolitely() {
        compose.setContent {
            MiMusicaTheme {
                SongsScreen(
                    songs = emptyList(), error = null, onPlay = {}, onPlayAll = {}, onDelete = {},
                    onSearch = {}, onBack = {}, deletedTitle = "Cielito Lindo",
                )
            }
        }
        compose.onNodeWithText("Se borró «Cielito Lindo».").assertIsDisplayed().assert(polite)
    }

    // --- Tamaño de letra ------------------------------------------------------------------------------------

    @Test
    fun aTextSizeChangeIsAnnounced() {
        compose.setContent {
            MiMusicaTheme {
                AjustesScreen(
                    fontChoice = FontChoice.ATKINSON,
                    textScale = TextScale.LARGE,
                    versionName = "0.1.0",
                    updateUi = AboutUpdateUi.UpToDate,
                    onSelectFont = {},
                    onSelectTextScale = {},
                    onCheckUpdate = {},
                    onInstallUpdate = {},
                    onCaregiver = {},
                    onBack = {},
                )
            }
        }
        compose.onNodeWithText("Tamaño elegido: Grande").performScrollTo().assertIsDisplayed().assert(polite)
        compose.onNodeWithText("Ya tienes la última versión").performScrollTo().assertIsDisplayed().assert(polite)
    }
}
