package com.mimusica.app.ui

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.mimusica.app.domain.model.DownloadState
import com.mimusica.app.domain.model.MatchType
import com.mimusica.app.domain.model.SearchResult
import com.mimusica.app.domain.model.Song
import com.mimusica.app.ui.search.SearchItem
import com.mimusica.app.ui.search.SearchScreen
import com.mimusica.app.ui.search.SearchUiState
import com.mimusica.app.ui.state.UiError
import com.mimusica.app.ui.theme.MiMusicaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SearchScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val song = Song(
        id = "1", sourceId = "id", title = "Cielito lindo", artist = "Quirino Mendoza", durationSeconds = 180,
        thumbnailUrl = null, hasSyncedLyrics = false, hasPlainLyrics = false, lyricsOffsetMs = 0, audioPath = "/a.m4a",
    )

    private fun item(
        state: DownloadState,
        match: MatchType = MatchType.TITLE,
        duration: Int? = 180,
        withSong: Boolean = false,
        title: String = "Cielito lindo",
        failure: UiError? = null,
    ) = SearchItem(
        SearchResult("id", title, "Quirino Mendoza", null, duration, null, match, state == DownloadState.InLibrary),
        state,
        songId = if (withSong) "1" else null,
        song = if (withSong) song else null,
        failure = failure,
    )

    private fun show(
        state: SearchUiState,
        onPlayNow: (Song) -> Unit = {},
        onAddToList: (Song) -> Unit = {},
        onDownload: (String) -> Unit = {},
    ) {
        compose.setContent {
            MiMusicaTheme {
                SearchScreen(
                    state = state,
                    micMessage = false,
                    onQueryChange = {}, onSearch = {}, onMic = {}, onDownload = onDownload, onBack = {},
                    onPlayNow = onPlayNow, onAddToList = onAddToList,
                )
            }
        }
    }

    @Test
    fun showsHintMicrophoneAndBackButtonWithBigTargets() {
        show(SearchUiState())
        // The label stays visible; the longer hint is the placeholder that appears once the field is focused.
        compose.onNodeWithText("Canción, cantante o letra").assertIsDisplayed()
        compose.onNodeWithText("Hablar").assertHeightIsAtLeast(64.dp)
        compose.onNodeWithText("Volver").assertHeightIsAtLeast(64.dp)
    }

    @Test
    fun hablarAndBuscarSitSideBySideAtTheSameHeight() {
        show(SearchUiState())
        // The header title is also "Buscar": the button is the last node with that text.
        val hablar = compose.onNodeWithText("Hablar").getUnclippedBoundsInRoot()
        val buscar = compose.onAllNodesWithText("Buscar").onLast().getUnclippedBoundsInRoot()
        assertTrue("Buscar must sit to the right of Hablar", buscar.left >= hablar.right)
        assertTrue("both share one row", buscar.top < hablar.bottom && hablar.top < buscar.bottom)
    }

    @Test
    fun resultCardShowsDownloadButtonAtLeast64dpTall() {
        show(SearchUiState(hasSearched = true, items = listOf(item(DownloadState.NotDownloaded))))
        compose.onNodeWithText("Descargar").assertIsDisplayed().assertHeightIsAtLeast(64.dp)
    }

    @Test
    fun lyricsMatchShowsTag() {
        show(SearchUiState(hasSearched = true, items = listOf(item(DownloadState.NotDownloaded, MatchType.LYRICS))))
        compose.onNodeWithText("Encontrada por la letra").assertExists()
    }

    @Test
    fun instrumentalTitlesShowTheInstrumentalTag() {
        show(
            SearchUiState(
                hasSearched = true,
                items = listOf(item(DownloadState.NotDownloaded, title = "Cielito lindo (Instrumental)")),
            ),
        )
        compose.onNodeWithText("Instrumental").assertExists()
    }

    @Test
    fun otherTitlesHaveNoInstrumentalTag() {
        show(SearchUiState(hasSearched = true, items = listOf(item(DownloadState.NotDownloaded))))
        compose.onNodeWithText("Instrumental").assertDoesNotExist()
    }

    @Test
    fun resultRowIsCompactCoverTitleArtistDurationThenAction() {
        show(SearchUiState(hasSearched = true, items = listOf(item(DownloadState.NotDownloaded))))
        val title = compose.onNodeWithText("Cielito lindo").getUnclippedBoundsInRoot()
        val action = compose.onNodeWithText("Descargar").getUnclippedBoundsInRoot()
        // The action sits under the identity block and the whole card stays short: no field-sized gaps.
        assertTrue(action.top >= title.bottom)
        assertTrue((action.top - title.top) < 220.dp)
    }

    @Test
    fun showsProgressAndLibraryStates() {
        show(
            SearchUiState(
                hasSearched = true,
                items = listOf(item(DownloadState.Downloading), item(DownloadState.InLibrary)),
            ),
        )
        compose.onNodeWithText("Descargando… puedes seguir, se guarda sola").assertExists()
        compose.onNodeWithText("¡Lista! Ya está en tus canciones").assertExists()
    }

    @Test
    fun showsTheDurationAndHidesItWhenUnknown() {
        show(SearchUiState(hasSearched = true, items = listOf(item(DownloadState.NotDownloaded, duration = 220))))
        compose.onNodeWithText("3:40").assertIsDisplayed()
    }

    @Test
    fun noDurationLineWhenTheServerDoesNotKnowIt() {
        show(SearchUiState(hasSearched = true, items = listOf(item(DownloadState.NotDownloaded, duration = null))))
        compose.onNodeWithText("0:00").assertDoesNotExist()
    }

    @Test
    fun savingOnThePhoneHasItsOwnMessage() {
        show(SearchUiState(hasSearched = true, items = listOf(item(DownloadState.Saving))))
        compose.onNodeWithText("Guardando en tu teléfono…").assertIsDisplayed()
    }

    @Test
    fun readySongOffersPlayNowAndAddToList() {
        var played: Song? = null
        var added: Song? = null
        show(
            SearchUiState(hasSearched = true, items = listOf(item(DownloadState.InLibrary, withSong = true))),
            onPlayNow = { played = it },
            onAddToList = { added = it },
        )
        compose.onNodeWithText("Reproducir ahora").assertHeightIsAtLeast(64.dp).performClick()
        compose.onNodeWithText("Agregar a una lista").assertHeightIsAtLeast(64.dp).performClick()
        assertEquals("1", played?.id)
        assertEquals("1", added?.id)
    }

    @Test
    fun failureSaysWhyAndOffersToTryAgain() {
        var retried = false
        show(
            SearchUiState(
                hasSearched = true,
                items = listOf(item(DownloadState.Failed), item(DownloadState.Failed, failure = UiError.NO_INTERNET)),
            ),
            onDownload = { retried = true },
        )
        compose.onNodeWithText("No se pudo descargar esta canción.").assertExists()
        compose.onNodeWithText("No hay internet. Tus canciones descargadas siguen funcionando.").assertExists()
        compose.onAllNodesWithText("Intentar otra vez")[0].performClick()
        assertTrue(retried)
    }

    @Test
    fun noInternetShowsFriendlyMessage() {
        show(SearchUiState(hasSearched = true, error = UiError.NO_INTERNET))
        compose.onNodeWithText("No hay internet. Tus canciones descargadas siguen funcionando.").assertIsDisplayed()
    }

    @Test
    fun enterSearchesInsteadOfInsertingANewline() {
        var query by mutableStateOf("")
        var searches = 0
        compose.setContent {
            MiMusicaTheme {
                SearchScreen(
                    state = SearchUiState(query = query),
                    micMessage = false,
                    onQueryChange = { query = it }, onSearch = { searches++ }, onMic = {}, onDownload = {}, onBack = {},
                    onPlayNow = {}, onAddToList = {},
                )
            }
        }
        compose.onNode(hasSetTextAction())
            .performTextInput("volver\n")
        compose.runOnIdle {
            assertEquals(1, searches)
            assertTrue('\n' !in query)
            assertEquals("volver", query)
        }
    }
}
