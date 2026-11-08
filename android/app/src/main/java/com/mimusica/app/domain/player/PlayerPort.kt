package com.mimusica.app.domain.player

import com.mimusica.app.domain.model.Song
import kotlinx.coroutines.flow.StateFlow

data class PlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    /** Title of the next item in the queue, or null when this is the last one. */
    val nextTitle: String? = null,
    /** System music volume on the 0..[VOLUME_STEPS] scale shown to the user; null while it is not known yet. */
    val volumeLevel: Int? = null,
)

/** What the UI needs from the audio player. Implemented over Media3 by `PlayerController`. */
interface PlayerPort {
    val state: StateFlow<PlaybackState>
    fun play(songs: List<Song>, startIndex: Int = 0)
    fun togglePlayPause()
    fun next()
    fun previous()
    fun seekTo(positionMs: Long)
    fun volumeUp()
    fun volumeDown()
}
