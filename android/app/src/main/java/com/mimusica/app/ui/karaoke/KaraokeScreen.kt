package com.mimusica.app.ui.karaoke

import android.app.Activity
import android.os.SystemClock
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimusica.app.R
import com.mimusica.app.domain.model.LyricLine
import com.mimusica.app.ui.components.BackButton
import com.mimusica.app.ui.components.BigButton
import com.mimusica.app.ui.components.OutlineBigButton
import com.mimusica.app.ui.theme.KaraokeBackground
import com.mimusica.app.ui.theme.KaraokeCurrent
import com.mimusica.app.ui.theme.KaraokeDim

/** Presentational karaoke: previous line dim above, current line very large and highlighted, next line below. */
@Composable
fun KaraokeScreen(
    state: KaraokeUiState,
    textScale: Float,
    onToggle: () -> Unit,
    onRestart: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LightSystemBarIcons()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KaraokeBackground)
            // Black reaches behind the system bars, but content stays clear of them (edge-to-edge).
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BackButton(onBack, dark = true)
        state.song?.let {
            Text(
                stringResource(R.string.karaoke_title_artist, it.title, it.artist),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.semantics { heading() },
                color = KaraokeDim,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (state.mode) {
                KaraokeMode.LOADING -> LyricText(stringResource(R.string.karaoke_loading), 28.sp * textScale, KaraokeDim, live = true)
                KaraokeMode.NONE -> LyricText(stringResource(R.string.karaoke_no_lyrics), 30.sp * textScale, Color.White, live = true)
                KaraokeMode.PLAIN -> PlainLyrics(state.plain.orEmpty(), textScale, state.progress)
                KaraokeMode.SYNCED -> SyncedLines(state.lines, state.currentIndex, textScale)
            }
        }
        if (state.mode != KaraokeMode.NONE && state.mode != KaraokeMode.LOADING) {
            // Labelled controls: the yellow key says what it does, "Empezar de nuevo" is the quiet second choice.
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BigButton(
                    text = stringResource(if (state.isPlaying) R.string.pause else R.string.play),
                    onClick = onToggle,
                    icon = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    minHeight = 96.dp,
                    containerColor = KaraokeCurrent,
                    contentColor = Color.Black,
                )
                OutlineBigButton(
                    text = stringResource(R.string.karaoke_restart),
                    onClick = onRestart,
                    icon = Icons.Filled.Replay,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun SyncedLines(lines: List<LyricLine>, currentIndex: Int, textScale: Float) {
    val previous = lines.getOrNull(currentIndex - 1)?.text.orEmpty()
    val current = if (currentIndex >= 0) lines[currentIndex].text else ""
    val next = lines.getOrNull(currentIndex + 1)?.text.orEmpty()
    // Long lines wrap; if the three together outgrow the viewport (huge text, long verse) they scroll.
    // No live region on purpose: TalkBack would read every line over the music.
    val scroll = rememberScrollState()
    LaunchedEffect(currentIndex) { scroll.scrollTo(0) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scroll),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LyricText(previous, 26.sp * textScale, KaraokeDim)
        LyricText(
            current.ifBlank { stringResource(R.string.karaoke_rest_mark) },
            (CURRENT_LINE_SP * textScale).sp,
            KaraokeCurrent,
            weight = FontWeight.Bold,
        )
        LyricText(next, 28.sp * textScale, Color.White.copy(alpha = 0.75f))
    }
}

@Composable
private fun PlainLyrics(text: String, textScale: Float, progress: Float) {
    val scroll = rememberScrollState()
    var lastTouchMs by remember { mutableStateOf<Long?>(null) }
    // The words drift down with the song (position / duration). Any touch on them parks the auto-scroll for a
    // few seconds so a reader who scrolled ahead or back is not yanked away.
    LaunchedEffect(progress) {
        if (shouldAutoScroll(SystemClock.elapsedRealtime(), lastTouchMs)) {
            scroll.animateScrollTo(
                plainScrollOffset(progress, scroll.maxValue),
                tween(durationMillis = 250, easing = LinearEasing),
            )
        }
    }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Fixed above the words so it stays visible however far they have scrolled.
        Text(
            stringResource(R.string.karaoke_plain_notice),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge,
            color = KaraokeDim,
            textAlign = TextAlign.Center,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial)
                            lastTouchMs = SystemClock.elapsedRealtime()
                        }
                    }
                }
                .verticalScroll(scroll),
        ) {
            Text(
                text,
                modifier = Modifier.fillMaxWidth(),
                style = TextStyle(fontSize = 28.sp * textScale, lineHeight = 40.sp * textScale),
                color = Color.White,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun LyricText(
    text: String,
    size: TextUnit,
    color: Color,
    weight: FontWeight = FontWeight.Normal,
    live: Boolean = false,
) {
    Text(
        text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .then(if (live) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier),
        style = TextStyle(fontSize = size, lineHeight = size * 1.25f, fontWeight = weight),
        color = color,
        textAlign = TextAlign.Center,
    )
}

/** Current line size in sp; the spec asks for at least 34sp. */
const val CURRENT_LINE_SP = 38f

/** The karaoke background is black, so the clock and system icons must be light while it is shown. */
@Composable
private fun LightSystemBarIcons() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previousStatus = controller?.isAppearanceLightStatusBars
        val previousNavigation = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            if (previousStatus != null) controller.isAppearanceLightStatusBars = previousStatus
            if (previousNavigation != null) controller.isAppearanceLightNavigationBars = previousNavigation
        }
    }
}
