package com.mimusica.app.ui.songs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mimusica.app.data.LibraryRepository
import com.mimusica.app.domain.model.Song
import com.mimusica.app.ui.state.UiError
import com.mimusica.app.ui.state.runCatchingUi
import com.mimusica.app.ui.state.toUiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SongsUiState(
    val songs: List<Song> = emptyList(),
    val error: UiError? = null,
    /** Title of the song that was just deleted, for the "Se borró «…»." confirmation; null otherwise. */
    val deletedTitle: String? = null,
) {
    /** Songs that have lyrics, for the "Cantar" list. */
    val singable: List<Song> get() = songs.filter { it.hasLyrics }
}

class SongsViewModel(private val library: LibraryRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SongsUiState())
    val uiState: StateFlow<SongsUiState> = _uiState

    init {
        viewModelScope.launch {
            library.observeDownloadedSongs().collect { songs -> _uiState.update { it.copy(songs = songs) } }
        }
    }

    fun delete(song: Song) {
        _uiState.update { it.copy(error = null, deletedTitle = null) }
        viewModelScope.launch {
            runCatchingUi { library.deleteSong(song.id) }
                .onSuccess { _uiState.update { it.copy(deletedTitle = song.title) } }
                .onFailure { e -> _uiState.update { it.copy(error = e.toUiError()) } }
        }
    }

    /** Forget the confirmation once the screen is left, so it does not greet the next visit. */
    fun clearNotice() = _uiState.update { it.copy(deletedTitle = null) }
}
