package com.mimusica.app.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mimusica.app.data.LibraryRepository
import com.mimusica.app.domain.model.Playlist
import com.mimusica.app.domain.model.Song
import com.mimusica.app.ui.state.UiError
import com.mimusica.app.ui.state.runCatchingUi
import com.mimusica.app.ui.state.toUiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlaylistsUiState(
    val playlists: List<Playlist> = emptyList(),
    val songs: List<Song> = emptyList(),
    val busy: Boolean = false,
    /** Name of the list a song was just added to, so the screen can confirm it. */
    val lastAddedTo: String? = null,
    val error: UiError? = null,
)

/** One ViewModel for the lists screen, a list's detail and the "add songs" picker. */
class PlaylistsViewModel(private val library: LibraryRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistsUiState())
    val uiState: StateFlow<PlaylistsUiState> = _uiState

    init {
        viewModelScope.launch {
            combine(library.observePlaylists(), library.observeDownloadedSongs()) { playlists, songs ->
                playlists to songs
            }.collect { (playlists, songs) ->
                _uiState.update { it.copy(playlists = playlists, songs = songs) }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearLastAdded() {
        _uiState.update { it.copy(lastAddedTo = null) }
    }

    fun createPlaylist(name: String, songIds: List<String> = emptyList()) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        perform {
            library.createPlaylist(clean, songIds)
            if (songIds.isNotEmpty()) _uiState.update { it.copy(lastAddedTo = clean) }
        }
    }

    fun deletePlaylist(id: String) = perform { library.deletePlaylist(id) }

    fun addSongs(playlistId: String, songIds: List<String>) = perform {
        val current = library.getPlaylist(playlistId) ?: return@perform
        library.updatePlaylist(playlistId, name = null, songIds = (current.songIds + songIds).distinct())
        _uiState.update { it.copy(lastAddedTo = current.name) }
    }

    fun removeSong(playlistId: String, songId: String) = perform {
        val current = library.getPlaylist(playlistId) ?: return@perform
        library.updatePlaylist(playlistId, name = null, songIds = current.songIds - songId)
    }

    private fun perform(block: suspend () -> Unit) {
        _uiState.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val result = runCatchingUi { block() }
            _uiState.update { it.copy(busy = false, error = result.exceptionOrNull()?.toUiError()) }
        }
    }
}
