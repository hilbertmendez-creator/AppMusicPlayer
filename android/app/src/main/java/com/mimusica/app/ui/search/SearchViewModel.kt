package com.mimusica.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mimusica.app.data.LibraryRepository
import com.mimusica.app.domain.model.DownloadState
import com.mimusica.app.domain.model.DownloadStatus
import com.mimusica.app.domain.model.SearchResult
import com.mimusica.app.domain.model.Song
import com.mimusica.app.domain.repository.MusicRemote
import com.mimusica.app.ui.state.UiError
import com.mimusica.app.ui.state.runCatchingUi
import com.mimusica.app.ui.state.toUiError
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * One result card. [songId] is known once the server finished the job, [song] once the audio is on the phone
 * (so it can be played), and [failure] says why the last download failed when we know (null = unknown).
 */
data class SearchItem(
    val result: SearchResult,
    val state: DownloadState,
    val songId: String? = null,
    val song: Song? = null,
    val failure: UiError? = null,
)

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val items: List<SearchItem> = emptyList(),
    val error: UiError? = null,
)

class SearchViewModel(
    private val remote: MusicRemote,
    private val library: LibraryRepository,
    private val pollDelayMs: Long = 2_000,
    private val maxPolls: Int = 150,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState

    private var searchJob: Job? = null

    /** Songs already playable on this phone; a card turns "ready" the moment its song shows up here. */
    private var downloaded: List<Song> = emptyList()

    init {
        viewModelScope.launch {
            library.observeDownloadedSongs().collect { songs ->
                downloaded = songs
                _uiState.update { ui -> ui.copy(items = ui.items.map(::resolve)) }
            }
        }
    }

    private fun resolve(item: SearchItem): SearchItem {
        val waiting = item.state == DownloadState.Saving ||
            item.state == DownloadState.InLibrary ||
            item.state == DownloadState.NotDownloaded
        if (!waiting) return item
        val match = downloaded.firstOrNull {
            (item.songId != null && it.id == item.songId) || it.sourceId == item.result.sourceId
        } ?: return item
        return item.copy(state = DownloadState.InLibrary, songId = match.id, song = match, failure = null)
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = sanitizeQuery(query)) }
    }

    fun onVoiceResult(text: String) {
        onQueryChange(text)
        search()
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun search() {
        val query = _uiState.value.query.trim()
        if (query.isEmpty()) return
        searchJob?.cancel()
        _uiState.update { it.copy(isSearching = true, error = null) }
        searchJob = viewModelScope.launch {
            runCatchingUi { remote.search(query, mode = "auto", limit = MAX_RESULTS) }
                .onSuccess { results ->
                    val items = results.take(MAX_RESULTS).map { resolve(SearchItem(it, initialState(it))) }
                    _uiState.update { it.copy(isSearching = false, hasSearched = true, items = items) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isSearching = false, hasSearched = true, items = emptyList(), error = e.toUiError())
                    }
                }
        }
    }

    fun download(sourceId: String) {
        val item = _uiState.value.items.firstOrNull { it.result.sourceId == sourceId } ?: return
        val busy = item.state == DownloadState.InLibrary ||
            item.state == DownloadState.Downloading ||
            item.state == DownloadState.Saving
        if (busy) return
        updateItem(sourceId) { it.copy(state = DownloadState.Downloading, failure = null) }
        _uiState.update { it.copy(error = null) }
        viewModelScope.launch {
            runCatchingUi { runDownload(item.result) }
                .onSuccess { songId ->
                    if (songId == null) {
                        updateItem(sourceId) { it.copy(state = DownloadState.Failed) }
                    } else {
                        // The server has it; the phone saves the file in the background (WorkManager).
                        updateItem(sourceId) { resolve(it.copy(state = DownloadState.Saving, songId = songId)) }
                    }
                }
                .onFailure { e ->
                    val reason = e.toUiError()
                    updateItem(sourceId) { it.copy(state = DownloadState.Failed, failure = reason) }
                    _uiState.update { it.copy(error = reason) }
                }
        }
    }

    /** Returns the new song id once the server job is ready and the phone was told to fetch it; null if it failed. */
    private suspend fun runDownload(result: SearchResult): String? {
        var job = remote.requestDownload(result.sourceId, result.title, result.artist)
        var polls = 0
        while ((job.status == DownloadStatus.PENDING || job.status == DownloadStatus.DOWNLOADING) && polls < maxPolls) {
            delay(pollDelayMs)
            job = remote.getDownload(job.id)
            polls++
        }
        val songId = job.songId
        if (job.status != DownloadStatus.READY || songId == null) return null
        library.addDownloadedSong(songId)
        return songId
    }

    private fun initialState(result: SearchResult): DownloadState =
        if (result.inLibrary) DownloadState.InLibrary else DownloadState.NotDownloaded

    private fun updateItem(sourceId: String, change: (SearchItem) -> SearchItem) {
        _uiState.update { ui ->
            ui.copy(items = ui.items.map { if (it.result.sourceId == sourceId) change(it) else it })
        }
    }

    private companion object {
        const val MAX_RESULTS = 5
    }
}

private val LineBreaks = Regex("""\s*[\r\n]+\s*""")

/** The query is a single line: Enter, voice and paste never leave line breaks in it. */
fun sanitizeQuery(text: String): String =
    if ('\n' in text || '\r' in text) text.replace(LineBreaks, " ").trim() else text
