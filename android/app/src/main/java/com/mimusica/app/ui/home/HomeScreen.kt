package com.mimusica.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mimusica.app.R
import com.mimusica.app.data.update.UpdateState
import com.mimusica.app.ui.components.Artwork
import com.mimusica.app.ui.components.BigIconButton
import com.mimusica.app.ui.components.MinTouch
import com.mimusica.app.ui.player.ProgressBlock
import com.mimusica.app.ui.theme.MiMusicaTheme
import com.mimusica.app.ui.components.SectionAccent
import com.mimusica.app.ui.components.SectionAccents
import com.mimusica.app.ui.theme.OnSolid

data class MiniPlayerUi(
    val title: String,
    val artist: String,
    val isPlaying: Boolean,
    val thumbnailUrl: String? = null,
    /** Where the song is and how long it lasts; the progress line shows only once the length is known. */
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
)

private val HomeGap = 16.dp

/** Space the header row (name + "Aa") takes at normal text size; used only to size the tiles. */
private val HeaderReserve = 72.dp

/**
 * Room kept for the mini player whether or not it is showing, so the four tiles are the same height and
 * stay put when a song starts (big targets that move under a finger are hard to hit). Fits a two-line title.
 */
private val MiniPlayerReserve = 192.dp
private val TileMinHeight = 104.dp
private val TileMaxHeight = 168.dp

/** Tile height that fills the screen with the mini-player slot reserved; never below [TileMinHeight]. */
private fun tileHeightFor(available: Dp): Dp {
    val gaps = HomeGap * 5 // header, four tiles, mini player slot
    val fit = (available - HeaderReserve - MiniPlayerReserve - gaps) / 4
    return fit.coerceIn(TileMinHeight, TileMaxHeight)
}

/**
 * Presentational "Inicio": the app name on the left, "Tamaño de letra" on the right (no PIN needed),
 * four giant buttons each with its own deep colour and a one-line live subtitle, and a mini player.
 * The tiles scroll when the text is huge instead of clipping; they keep their height with or without the player.
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
    summary: HomeSummary? = null,
    updateState: UpdateState = UpdateState.None,
    onUpdate: () -> Unit = {},
) {
    // Null summary = still loading: no subtitle rather than a wrong "Todavía no tienes canciones".
    val songsSubtitle = summary?.let {
        if (it.songs == 0) stringResource(R.string.home_songs_none)
        else pluralStringResource(R.plurals.song_count, it.songs, it.songs)
    }
    val playlistsSubtitle = summary?.let {
        if (it.playlists == 0) stringResource(R.string.home_playlists_none)
        else pluralStringResource(R.plurals.home_playlists_count, it.playlists, it.playlists)
    }
    val singSubtitle = summary?.let {
        if (it.singable == 0) stringResource(R.string.home_sing_none)
        else stringResource(R.string.home_sing_count, it.singable)
    }
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(HomeGap),
        ) {
            val tileHeight = tileHeightFor(maxHeight)
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(HomeGap)) {
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
                if (updateState !is UpdateState.None) {
                    UpdateBanner(updateState, onClick = onUpdate)
                }
                // Full-width tiles (one per row) so labels never break mid-word; the list scrolls when the
                // text is so large that the four tiles no longer fit.
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(HomeGap),
                ) {
                    HomeTile(R.string.home_songs, songsSubtitle, SectionAccents.Songs, onSongs, tileHeight)
                    HomeTile(R.string.home_playlists, playlistsSubtitle, SectionAccents.Playlists, onPlaylists, tileHeight)
                    HomeTile(
                        R.string.home_search, stringResource(R.string.home_search_subtitle),
                        SectionAccents.Search, onSearch, tileHeight,
                    )
                    HomeTile(R.string.home_sing, singSubtitle, SectionAccents.Sing, onSing, tileHeight)
                }
                if (miniPlayer != null) {
                    MiniPlayerBar(miniPlayer, onOpen = onMiniPlayer, onToggle = onMiniToggle)
                }
            }
        }
    }
}

/**
 * Big, plain "new version" card above the tiles: says what is happening in two short lines and is one tap to
 * act. While the file downloads it is not clickable (a second tap would start a second download).
 */
@Composable
private fun UpdateBanner(state: UpdateState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val busy = state is UpdateState.Downloading
    val title = stringResource(
        when (state) {
            is UpdateState.Downloading -> R.string.update_downloading
            is UpdateState.Failed -> R.string.update_failed
            else -> R.string.update_available
        },
    )
    val hint = stringResource(if (busy) R.string.update_wait else R.string.update_tap)
    Surface(
        onClick = onClick,
        enabled = !busy,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MinTouch)
            .semantics { role = Role.Button },
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(hint, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
        }
    }
}

/**
 * Round "Aa" pill: short enough to never wrap at any text size. TalkBack reads "Aa, Tamaño de letra":
 * the spoken name starts with the visible text.
 */
@Composable
private fun TextSizeButton(onClick: () -> Unit) {
    val label = stringResource(R.string.home_text_size_desc)
    Surface(
        onClick = onClick,
        modifier = Modifier
            .heightIn(min = MinTouch)
            .widthIn(min = MinTouch)
            .semantics {
                role = Role.Button
                contentDescription = label
            },
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
    ) {
        Box(modifier = Modifier.padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.home_text_size_pill), style = MaterialTheme.typography.titleLarge)
        }
    }
}

/**
 * Full-width tile in its own deep colour: the icon sits in a lighter rounded box, the title and a one-line
 * subtitle take the remaining width, and a chevron says "this opens a screen". [minHeight] is a floor, so
 * the tile grows with the text instead of clipping it.
 */
@Composable
private fun HomeTile(
    labelRes: Int,
    subtitle: String?,
    accent: SectionAccent,
    onClick: () -> Unit,
    minHeight: Dp,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .semantics { role = Role.Button },
        shape = RoundedCornerShape(28.dp),
        color = accent.color,
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
                Icon(accent.icon, contentDescription = null, modifier = Modifier.size(40.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(labelRes),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Start,
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Start,
                        // No line cap: at the largest text sizes the subtitle wraps and the tile grows (it has a minimum
                        // height, not a fixed one) instead of cutting "Todavía ninguna con letra" short.
                    )
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(36.dp))
        }
    }
}

/**
 * "Sonando ahora" card: the cover, three lines (label, title, artist) and a big round play/pause.
 * The card opens the player ("Abrir el reproductor" for TalkBack); the pause button stays its own stop.
 */
@Composable
fun MiniPlayerBar(state: MiniPlayerUi, onOpen: () -> Unit, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val openLabel = stringResource(R.string.mini_player_open)
    Surface(
        onClick = onOpen,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .semantics {
                role = Role.Button
                // Label only: the click action itself still comes from the Surface.
                onClick(label = openLabel, action = null)
            },
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Decorative: the card is already named by the title and artist next to it.
                Artwork(url = state.thumbnailUrl, size = 68.dp, decorative = true)
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
            if (state.durationMs > 0) ProgressBlock(state.positionMs, state.durationMs)
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun HomeScreenPreview() {
    MiMusicaTheme {
        HomeScreen(
            miniPlayer = MiniPlayerUi("Volver", "Gardel", true),
            summary = HomeSummary(songs = 12, playlists = 2, singable = 8),
            onSongs = {}, onPlaylists = {}, onSearch = {}, onSing = {},
            onSettings = {}, onMiniPlayer = {}, onMiniToggle = {},
        )
    }
}
