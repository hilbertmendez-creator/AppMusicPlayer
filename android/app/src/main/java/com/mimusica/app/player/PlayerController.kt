package com.mimusica.app.player

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.mimusica.app.domain.model.Song
import com.mimusica.app.domain.player.PlaybackState
import com.mimusica.app.domain.player.PlayerPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * [PlayerPort] over a Media3 [MediaController] connected to [PlaybackService].
 * Publishes the playback position roughly every 100 ms while playing (used by the karaoke).
 * Must be created and used on the main thread.
 */
class PlayerController(private val context: Context, scope: CoroutineScope) : PlayerPort {
    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var controller: MediaController? = null
    private var queue: List<Song> = emptyList()
    private val pending = mutableListOf<(MediaController) -> Unit>()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish()
    }

    private var connecting = false

    /**
     * Binding to [PlaybackService] builds an ExoPlayer on the main thread, so it is not done while the first
     * frame is being drawn: it waits a moment after launch, or happens at once when something needs the player.
     */
    private fun connect() {
        if (connecting) return
        connecting = true
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                val connected = future.get()
                controller = connected
                connected.addListener(listener)
                pending.forEach { it(connected) }
                pending.clear()
                publish()
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    init {
        scope.launch {
            delay(CONNECT_DELAY_MS)
            connect()
        }
        scope.launch {
            while (isActive) {
                delay(TICK_MS)
                if (controller?.isPlaying == true) publish()
            }
        }
    }

    override fun play(songs: List<Song>, startIndex: Int) {
        val playable = songs.filter { it.audioPath != null }
        if (playable.isEmpty()) return
        val start = songs.getOrNull(startIndex)?.let { playable.indexOf(it) }?.coerceAtLeast(0) ?: 0
        queue = playable
        withController { c ->
            c.setMediaItems(playable.map(::toMediaItem), start, 0L)
            c.prepare()
            c.play()
        }
    }

    override fun togglePlayPause() = withController { c ->
        if (c.isPlaying) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_ENDED) c.seekToDefaultPosition()
            c.play()
        }
    }

    override fun next() = withController { it.seekToNextMediaItem() }

    override fun previous() = withController { it.seekToPreviousMediaItem() }

    override fun seekTo(positionMs: Long) = withController { it.seekTo(positionMs) }

    override fun volumeUp() = adjustVolume(AudioManager.ADJUST_RAISE)

    override fun volumeDown() = adjustVolume(AudioManager.ADJUST_LOWER)

    private fun adjustVolume(direction: Int) {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
    }

    private fun withController(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) {
            block(c)
        } else {
            pending += block
            connect()
        }
    }

    private fun publish() {
        val c = controller ?: return
        val currentId = c.currentMediaItem?.mediaId
        _state.value = PlaybackState(
            currentSong = queue.firstOrNull { it.id == currentId },
            isPlaying = c.isPlaying,
            positionMs = c.currentPosition.coerceAtLeast(0L),
            durationMs = c.duration.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0L) ?: 0L,
            hasNext = c.hasNextMediaItem(),
            hasPrevious = c.hasPreviousMediaItem(),
            nextTitle = nextTitle(c),
        )
    }

    private fun nextTitle(c: MediaController): String? {
        val index = c.nextMediaItemIndex
        if (index == C.INDEX_UNSET) return null
        return c.getMediaItemAt(index).mediaMetadata.title?.toString()
    }

    private fun toMediaItem(song: Song): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .apply { song.thumbnailUrl?.takeIf { it.isNotBlank() }?.let { setArtworkUri(it.toUri()) } }
            .build()
        return MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(Uri.fromFile(File(requireNotNull(song.audioPath))))
            .setMediaMetadata(metadata)
            .build()
    }

    private companion object {
        const val TICK_MS = 100L
        const val CONNECT_DELAY_MS = 600L
    }
}
