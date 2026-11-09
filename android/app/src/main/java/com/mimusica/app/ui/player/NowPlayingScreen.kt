package com.mimusica.app.ui.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mimusica.app.R
import com.mimusica.app.domain.player.PlaybackState
import com.mimusica.app.ui.components.ArtworkFill
import com.mimusica.app.ui.components.BigButton
import com.mimusica.app.ui.components.EmptyMessage
import com.mimusica.app.ui.components.MinTouch
import com.mimusica.app.ui.theme.Ink
import com.mimusica.app.ui.theme.InkSoft
import com.mimusica.app.ui.theme.Line
import com.mimusica.app.ui.theme.Navy
import com.mimusica.app.ui.theme.OnNavy
import com.mimusica.app.ui.theme.ProgressFill
import com.mimusica.app.ui.theme.Sun
import com.mimusica.app.ui.theme.SurfaceFill
import com.mimusica.app.ui.theme.ProgressTrack
import com.mimusica.app.ui.components.ScreenScaffold

private val ArtworkMin = 96.dp
private val ArtworkMax = 240.dp
private val BlockGap = 12.dp

/**
 * Presentational "Reproduciendo". Controls have priority: the cover takes only the space left over
 * (and disappears when that is too little). If even the controls do not fit (largest text size,
 * small phone) the whole body scrolls so "Cantar" is always reachable.
 */
@Composable
fun NowPlayingScreen(
    state: PlaybackState,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSing: (songId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val song = state.currentSong
    ScreenScaffold(
        title = stringResource(R.string.now_playing_title),
        onBack = onBack,
        modifier = modifier,
        scrollable = false,
    ) {
        if (song == null) {
            EmptyMessage(stringResource(R.string.nothing_playing))
            return@ScreenScaffold
        }
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val viewportHeight = maxHeight
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                CoverAboveControls(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = viewportHeight),
                    cover = { ArtworkFill(url = song.thumbnailUrl, decorative = false, corner = 34.dp) },
                    controls = {
                        Controls(
                            state = state,
                            title = song.title,
                            artist = song.artist,
                            canSing = song.hasLyrics,
                            onToggle = onToggle,
                            onNext = onNext,
                            onPrevious = onPrevious,
                            onSing = { onSing(song.id) },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun Controls(
    state: PlaybackState,
    title: String,
    artist: String,
    canSing: Boolean,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSing: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BlockGap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // One spoken unit, announced politely whenever the song changes (next / previous / the queue moving on).
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(BlockGap),
        ) {
            Text(
                title,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                artist,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                color = InkSoft,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (state.durationMs > 0) ProgressBlock(state.positionMs, state.durationMs)
        TransportKeys(
            isPlaying = state.isPlaying,
            onPrevious = onPrevious,
            onToggle = onToggle,
            onNext = onNext,
        )
        state.nextTitle?.let {
            Text(
                stringResource(R.string.up_next, it),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (canSing) {
            // Sun marks this one action; the label is the whole sentence so nobody wonders what "Cantar" opens.
            BigButton(
                text = stringResource(R.string.sing_with_lyrics),
                onClick = onSing,
                icon = Icons.Filled.Lyrics,
                minHeight = MinTouch,
                containerColor = Sun,
                contentColor = Ink,
            )
        } else {
            // A quiet explanation, not a disabled button: nothing here looks tappable.
            Text(
                stringResource(R.string.now_playing_no_lyrics),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Height of a transport key in the row: icon + label stacked, well above the 64dp floor. */
private val TransportKeyHeight = 92.dp

/** Minimum height of a transport key when the keys fall back to full-width rows. */
private val TransportRowHeight = 72.dp

/** Icon slot shared by every key so all labels sit on one baseline, whatever the icon size. */
private val TransportIconSlot = 40.dp

private val TransportGap = 8.dp

/** Width of the play/pause key relative to previous and next. */
private const val CenterKeyWeight = 1.5f

/** Inner side padding so a label never touches the 2dp card border (it did with "Siguiente" on a Pixel 7a). */
private val TransportKeyPadding = 6.dp

/**
 * Previous / play-pause / next as three labeled cards (play/pause a little wider). When a label would not fit its
 * card on one line (bigger "Tamaño de letra", narrow phone) the keys become full-width rows instead,
 * so a word is never broken. Both play and pause labels are measured, so toggling never flips layout.
 */
@Composable
private fun TransportKeys(
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
) {
    val previous = stringResource(R.string.previous)
    val next = stringResource(R.string.next)
    val pause = stringResource(R.string.pause)
    val play = stringResource(R.string.resume)
    val style = MaterialTheme.typography.labelMedium
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // 8dp between keys so a finger that lands between two never fires the wrong one. The play/pause key is
        // wider than its neighbours: it is the main action.
        val unit = (maxWidth - TransportGap * 2) / (2 + CenterKeyWeight)
        val sideInner = unit - TransportKeyPadding * 2
        val centerInner = unit * CenterKeyWeight - TransportKeyPadding * 2
        fun widthOf(vararg labels: String) = with(density) {
            labels.maxOf { measurer.measure(it, style, maxLines = 1, softWrap = false).size.width }.toDp()
        }
        val stacked = widthOf(previous, next) <= sideInner && widthOf(pause, play) <= centerInner
        val toggleIcon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow
        val toggleLabel = if (isPlaying) pause else play
        if (stacked) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TransportGap),
            ) {
                TransportKey(Icons.Filled.SkipPrevious, previous, onPrevious, primary = false, stacked = true, Modifier.weight(1f))
                TransportKey(toggleIcon, toggleLabel, onToggle, primary = true, stacked = true, Modifier.weight(CenterKeyWeight))
                TransportKey(Icons.Filled.SkipNext, next, onNext, primary = false, stacked = true, Modifier.weight(1f))
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(TransportGap),
            ) {
                TransportKey(toggleIcon, toggleLabel, onToggle, primary = true, stacked = false, Modifier.fillMaxWidth())
                TransportKey(Icons.Filled.SkipNext, next, onNext, primary = false, stacked = false, Modifier.fillMaxWidth())
                TransportKey(Icons.Filled.SkipPrevious, previous, onPrevious, primary = false, stacked = false, Modifier.fillMaxWidth())
            }
        }
    }
}

/**
 * Labeled playback key: a card with its icon and name. Play/pause is the only filled key;
 * previous/next are outlined so the main action stands out by colour, not size.
 * [stacked] puts the icon above the label (grid card); otherwise beside it (full-width row).
 */
@Composable
private fun TransportKey(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    primary: Boolean,
    stacked: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = if (stacked) TransportKeyHeight else TransportRowHeight)
            .semantics {
                role = Role.Button
                // The play/pause key changes its label in place: say the new state politely.
                if (primary) liveRegion = LiveRegionMode.Polite
            },
        shape = RoundedCornerShape(18.dp),
        color = if (primary) Navy else SurfaceFill,
        contentColor = if (primary) OnNavy else Ink,
        border = if (primary) null else BorderStroke(2.dp, Line),
    ) {
        val iconSlot: @Composable () -> Unit = {
            Box(modifier = Modifier.size(TransportIconSlot), contentAlignment = Alignment.Center) {
                // Decorative: the label is the accessible name.
                Icon(icon, contentDescription = null, modifier = Modifier.size(if (primary) 40.dp else 32.dp))
            }
        }
        if (stacked) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = TransportKeyPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            ) {
                iconSlot()
                Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 1, softWrap = false)
            }
        } else {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                iconSlot()
                Text(label, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/**
 * Progress bar plus "1:05 / 3:40"; TalkBack gets one stop: "Avance de la canción, 1:05 de 3:40".
 * Used by the player screen.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ProgressBlock(positionMs: Long, durationMs: Long, modifier: Modifier = Modifier) {
    val fraction = (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    val spoken = stringResource(
        R.string.now_playing_progress_spoken,
        formatClock(positionMs.coerceIn(0L, durationMs)),
        formatDuration(durationMs),
    )
    val label = stringResource(R.string.now_playing_progress)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LinearProgressIndicator(
            progress = { fraction },
            color = ProgressFill,
            trackColor = ProgressTrack,
            // A plain bar: no gap before the track and no dot at its end, so it never looks like a slider
            // you could drag (it can't be: the transport keys are the way to move around).
            gapSize = 0.dp,
            drawStopIndicator = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .semantics {
                    contentDescription = label
                    stateDescription = spoken
                    progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                },
        )
        // Sighted users read "1:05 / 3:40"; TalkBack already gets it from the bar above (one focus stop).
        Text(
            formatPlaybackProgress(positionMs, durationMs),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { invisibleToUser() },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Lays out [controls] at the bottom and gives [cover] a square in whatever height is left above them
 * (at most [ArtworkMax]); hides the cover below [ArtworkMin]. With a taller min height than the controls
 * need, the extra goes to the cover; otherwise the content simply grows past the viewport and scrolls.
 */
@Composable
private fun CoverAboveControls(
    modifier: Modifier,
    cover: @Composable () -> Unit,
    controls: @Composable () -> Unit,
) {
    Layout(content = { cover(); controls() }, modifier = modifier) { measurables, constraints ->
        val width = constraints.maxWidth
        val gap = BlockGap.roundToPx()
        val controlsPlaceable = measurables[1].measure(
            Constraints(minWidth = width, maxWidth = width, minHeight = 0, maxHeight = Constraints.Infinity),
        )
        val free = constraints.minHeight - controlsPlaceable.height - gap
        val side = minOf(free, width, ArtworkMax.roundToPx())
        val showCover = side >= ArtworkMin.roundToPx()
        val coverPlaceable = if (showCover) measurables[0].measure(Constraints.fixed(side, side)) else null
        val totalHeight = if (showCover) constraints.minHeight else maxOf(constraints.minHeight, controlsPlaceable.height)
        layout(width, totalHeight) {
            if (coverPlaceable != null) {
                coverPlaceable.placeRelative((width - side) / 2, (free - side) / 2)
                controlsPlaceable.placeRelative(0, free + gap)
            } else {
                controlsPlaceable.placeRelative(0, 0)
            }
        }
    }
}
