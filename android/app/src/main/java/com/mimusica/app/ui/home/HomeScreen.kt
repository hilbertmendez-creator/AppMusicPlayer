package com.mimusica.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mimusica.app.R
import com.mimusica.app.ui.components.BigIconButton
import com.mimusica.app.ui.components.MinTouch
import com.mimusica.app.ui.theme.AmberBrown
import com.mimusica.app.ui.theme.DeepBlue
import com.mimusica.app.ui.theme.DeepTeal
import com.mimusica.app.ui.theme.MiMusicaTheme
import com.mimusica.app.ui.theme.OnSolid
import com.mimusica.app.ui.theme.Plum

data class MiniPlayerUi(val title: String, val artist: String, val isPlaying: Boolean)

/**
 * Presentational "Inicio": the app name on the left, "Tamaño de letra" on the right (no PIN needed),
 * four giant buttons each with its own deep colour, and a mini player.
 */
@Composable
fun HomeScreen(
    miniPlayer: MiniPlayerUi?,
    onSongs: () -> Unit,
    onPlaylists: () -> Unit,
    onSearch: () -> Unit,
    onSing: () -> Unit,
    onSettings: () -> Unit,
    onMiniPlayer: () -> Unit,
    onMiniToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.app_name),
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                TextSizeButton(onClick = onSettings)
            }
            // Full-width tiles (one per row) so labels never break mid-word, even at the largest text scale.
            HomeTile(R.string.home_songs, Icons.Filled.LibraryMusic, DeepBlue, onSongs, Modifier.weight(1f))
            HomeTile(R.string.home_playlists, Icons.Filled.QueueMusic, Plum, onPlaylists, Modifier.weight(1f))
            HomeTile(R.string.home_search, Icons.Filled.Search, DeepTeal, onSearch, Modifier.weight(1f))
            // Lyrics icon, not a microphone: the mic is reserved for "Hablar" (voice search).
            HomeTile(R.string.home_sing, Icons.Filled.Lyrics, AmberBrown, onSing, Modifier.weight(1f))
            if (miniPlayer != null) {
                MiniPlayerBar(miniPlayer, onOpen = onMiniPlayer, onToggle = onMiniToggle)
            }
        }
    }
}

/**
 * Round "Aa" pill: short enough to never wrap at any text size. The visible "Aa" is decorative;
 * TalkBack reads the full "Tamaño de letra".
 */
@Composable
private fun TextSizeButton(onClick: () -> Unit) {
    val label = stringResource(R.string.home_text_size)
    Surface(
        onClick = onClick,
        modifier = Modifier
            .heightIn(min = MinTouch)
            .widthIn(min = MinTouch)
            .semantics { contentDescription = label },
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
    ) {
        Box(modifier = Modifier.padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
            Text("Aa", style = MaterialTheme.typography.titleLarge)
        }
    }
}

/**
 * Full-width tile in its own deep colour: the icon sits in a lighter rounded box, the label takes
 * the remaining width, and a chevron says "this opens a screen".
 */
@Composable
private fun HomeTile(
    labelRes: Int,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp),
        shape = RoundedCornerShape(28.dp),
        color = color,
        contentColor = OnSolid,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(OnSolid.copy(alpha = 0.16f), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(40.dp))
            }
            Text(
                stringResource(labelRes),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Start,
            )
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(36.dp))
        }
    }
}

/** "Sonando ahora" card: a small cover, three lines (label, title, artist) and a big round play/pause. */
@Composable
fun MiniPlayerBar(state: MiniPlayerUi, onOpen: () -> Unit, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onOpen,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .semantics { role = Role.Button },
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(DeepBlue, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = OnSolid, modifier = Modifier.size(36.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.mini_player_now),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
                Text(
                    state.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    state.artist,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BigIconButton(
                icon = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                description = stringResource(if (state.isPlaying) R.string.pause else R.string.play),
                onClick = onToggle,
                size = 72.dp,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun HomeScreenPreview() {
    MiMusicaTheme {
        HomeScreen(
            miniPlayer = MiniPlayerUi("Volver", "Gardel", true),
            onSongs = {}, onPlaylists = {}, onSearch = {}, onSing = {},
            onSettings = {}, onMiniPlayer = {}, onMiniToggle = {},
        )
    }
}
