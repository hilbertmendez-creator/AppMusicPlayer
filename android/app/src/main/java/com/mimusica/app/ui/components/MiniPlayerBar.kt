package com.mimusica.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mimusica.app.R
import com.mimusica.app.domain.player.PlaybackState
import com.mimusica.app.ui.theme.InkSoft
import com.mimusica.app.ui.theme.Line
import com.mimusica.app.ui.theme.SurfaceFill

/** What the persistent mini player shows: the song that is loaded and whether it is playing. */
data class MiniPlayerUi(
    val title: String,
    val artist: String,
    val isPlaying: Boolean,
    val thumbnailUrl: String? = null,
)

/** Null while nothing is loaded, which hides the mini player. */
fun PlaybackState.toMiniPlayerUi(): MiniPlayerUi? =
    currentSong?.let { MiniPlayerUi(it.title, it.artist, isPlaying, it.thumbnailUrl) }

/**
 * The mini player above the tab bar: cover, title and "Sonando" / "En pausa", and a round Navy pause / play.
 * The open area and the toggle are two siblings, never nested, so each is its own clean target for touch and
 * for TalkBack ("Abrir reproductor: <title>, sonando" and "Pausar").
 */
@Composable
fun MiniPlayerBar(state: MiniPlayerUi, onOpen: () -> Unit, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(22.dp)
    val status = stringResource(if (state.isPlaying) R.string.mini_player_playing else R.string.mini_player_paused)
    val openLabel = stringResource(R.string.mini_player_open_titled, state.title, status.lowercase())
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceFill, shape)
            .border(2.dp, Line, shape)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = MinTouch)
                .clip(RoundedCornerShape(16.dp))
                .clickable(role = Role.Button, onClick = onOpen)
                .semantics(mergeDescendants = true) { contentDescription = openLabel },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Artwork(url = state.thumbnailUrl, size = 44.dp, decorative = true)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    state.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(status, style = MaterialTheme.typography.bodyLarge, color = InkSoft, maxLines = 1)
            }
        }
        BigIconButton(
            icon = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            description = stringResource(if (state.isPlaying) R.string.pause else R.string.resume),
            onClick = onToggle,
            size = MinTouch,
        )
    }
}
