package com.mimusica.app.ui.karaoke

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mimusica.app.data.LastSungStore
import com.mimusica.app.data.LibraryRepository
import com.mimusica.app.domain.lyrics.LrcParser
import com.mimusica.app.domain.lyrics.LyricsTimeline
import com.mimusica.app.domain.model.LyricLine
import com.mimusica.app.domain.model.Song
import com.mimusica.app.domain.player.PlayerPort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class KaraokeMode { LOADING, SYNCED, PLAIN, NONE }

data class KaraokeUiState(
    val mode: KaraokeMode = KaraokeMode.LOADING,
    val song: Song? = null,
    val lines: List<LyricLine> = emptyList(),
    val plain: String? = null,
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    /** Playback position as a 0..1 fraction of the song; drives the auto-scroll of plain lyrics. */
    val progress: Float = 0f,
)

class KaraokeViewModel(
    private val songId: String,
    private val library: LibraryRepository,
    private val player: PlayerPort,
    private val lastSung: LastSungStore? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(KaraokeUiState())
    val uiState: StateFlow<KaraokeUiState> = _uiState

    private var timeline = LyricsTimeline(emptyList())
    private var totalOffsetMs = 0L

    init {
        viewModelScope.launch {
            load()
            player.state.collect { playback ->
                _uiState.update {
                    it.copy(
                        isPlaying = playback.isPlaying,
                        progress = playbackFraction(playback.positionMs, playback.durationMs),
                        currentIndex = if (it.mode == KaraokeMode.SYNCED) {
                            timeline.currentIndex(playback.positionMs, totalOffsetMs)
                        } else {
                            -1
                        },
                    )
                }
            }
        }
    }

    fun togglePlayPause() = player.togglePlayPause()

    /** "Empezar de nuevo": back to the first second, keeping whatever play/pause state the song is in. */
    fun restart() = player.seekTo(0L)

    private suspend fun load() {
        val song = library.getSong(songId)
        val lyrics = library.getLyrics(songId)
        val parsed = lyrics?.syncedLrc?.takeIf { it.isNotBlank() }?.let { LrcParser.parse(it) }
        val plain = lyrics?.plain?.takeIf { it.isNotBlank() }
        val synced = parsed != null && parsed.lines.isNotEmpty()

        if (synced) {
            timeline = LyricsTimeline(parsed!!.lines)
            totalOffsetMs = parsed.offsetMs + (lyrics?.offsetMs ?: 0)
        }
        val mode = when {
            synced -> KaraokeMode.SYNCED
            plain != null -> KaraokeMode.PLAIN
            else -> KaraokeMode.NONE
        }
        _uiState.update {
            it.copy(
                mode = mode,
                song = song,
                lines = if (synced) parsed!!.lines else emptyList(),
                plain = plain,
            )
        }
        // Home's "Seguir cantando" opens whatever was sung last.
        if (song != null && mode != KaraokeMode.NONE) lastSung?.setLastSungSongId(song.id)
        if (song != null && song.isDownloaded && player.state.value.currentSong?.id != song.id) {
            player.play(listOf(song), 0)
        }
    }
}
