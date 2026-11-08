package com.mimusica.app.domain.player

/** The volume the user sees: "Volumen 6 de 10". Android's own range (often 0..15) is mapped onto it. */
const val VOLUME_STEPS = 10

/** Maps the system music volume [current] (0..[max]) onto 0..[VOLUME_STEPS], rounding to the nearest step. */
fun volumeLevel(current: Int, max: Int): Int {
    if (max <= 0) return 0
    return Math.round(current.coerceIn(0, max) * VOLUME_STEPS.toFloat() / max)
}
