package com.mimusica.app.ui.karaoke

import kotlin.math.roundToInt

/** How long plain lyrics stay where the user put them after a touch, before auto-scroll takes over again. */
const val AUTO_SCROLL_PAUSE_MS = 4_000L

/** Playback position as a 0..1 fraction of the song; 0 while the duration is not known yet. */
fun playbackFraction(positionMs: Long, durationMs: Long): Float =
    if (durationMs <= 0) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)

/** Scroll offset in pixels that matches [fraction] of the song, given the [maxScroll] distance of the lyrics. */
fun plainScrollOffset(fraction: Float, maxScroll: Int): Int =
    (fraction.coerceIn(0f, 1f) * maxScroll.coerceAtLeast(0)).roundToInt()

/** Auto-scroll runs unless the user touched the lyrics less than [pauseMs] ago. */
fun shouldAutoScroll(nowMs: Long, lastTouchMs: Long?, pauseMs: Long = AUTO_SCROLL_PAUSE_MS): Boolean =
    lastTouchMs == null || nowMs - lastTouchMs >= pauseMs
