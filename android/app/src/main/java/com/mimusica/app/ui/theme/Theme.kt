package com.mimusica.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mimusica.app.data.settings.TextScale

/*
 * Calm pearl-grey palette with white cards. Every text/background pair is >= 7:1 (WCAG AAA),
 * checked by PaletteContrastTest. Colour meanings are fixed:
 *   blue  = the main action (and the "Mis canciones" tile)
 *   green = success only ("listo", "guardado", "ya está en tus canciones")
 *   red   = delete / error only
 */
val Ink = Color(0xFF1F1611)
val OnSolid = Color(0xFFFFFFFF)

/** Pearl grey ("Gris perla"). Must equal `@color/paper` (window / splash background) so there is no white flash. */
val Paper = Color(0xFFF1F3F5)

/** White surface for cards and dialogs, a step lighter than [Paper]. */
val Card = Color(0xFFFFFFFF)

/** Slate outline for cards and quiet buttons; also readable as secondary text. */
val Outline = Color(0xFF374151)

val DeepBlue = Color(0xFF0B3D91)
val SoftBlue = Color(0xFFE3ECFA)
val DeepGreen = Color(0xFF1B5E20)
val DeepRed = Color(0xFF9B0000)

/** One deep colour per Home tile so Rosa can tell them apart by colour too. Blue is [DeepBlue]. */
val Plum = Color(0xFF6A1B6B)
val DeepTeal = Color(0xFF00585C)
val AmberBrown = Color(0xFF7A3E00)

/** Progress bar: a visible track (>= 3:1 on Paper and Card) and a dark fill (>= 3:1 on the track). */
val ProgressTrack = Color(0xFF7B8794)
val ProgressFill = Color(0xFF0A2F73)

val KaraokeBackground = Color(0xFF000000)
val KaraokeCurrent = Color(0xFFFFEB3B)
val KaraokeDim = Color(0xFFB0B0B0)

private val WarmColors = lightColorScheme(
    primary = DeepBlue,
    onPrimary = OnSolid,
    primaryContainer = SoftBlue,
    onPrimaryContainer = Ink,
    // Neutral on purpose: green is reserved for success (see [Success]), so no control uses `secondary`.
    secondary = Outline,
    onSecondary = OnSolid,
    background = Paper,
    onBackground = Ink,
    surface = Card,
    onSurface = Ink,
    surfaceVariant = Card,
    onSurfaceVariant = Ink,
    error = DeepRed,
    onError = OnSolid,
    outline = Outline,
)

/** Success colours: the only place green appears. */
object Success {
    val container = DeepGreen
    val onContainer = OnSolid
    val text = DeepGreen
}

/** Every size is >= 22sp for body text; [scale] multiplies the whole scale (Tamaño de letra). */
fun scaledTypography(scale: Float): Typography {
    fun style(size: Int, weight: FontWeight = FontWeight.Normal, lineHeight: Int = (size * 1.3f).toInt()) =
        TextStyle(fontSize = (size * scale).sp, fontWeight = weight, lineHeight = (lineHeight * scale).sp)
    return Typography(
        displayLarge = style(44, FontWeight.Bold),
        displayMedium = style(40, FontWeight.Bold),
        displaySmall = style(36, FontWeight.Bold),
        headlineLarge = style(36, FontWeight.Bold),
        headlineMedium = style(32, FontWeight.Bold),
        headlineSmall = style(28, FontWeight.Bold),
        titleLarge = style(28, FontWeight.Bold),
        titleMedium = style(24, FontWeight.SemiBold),
        titleSmall = style(22, FontWeight.SemiBold),
        bodyLarge = style(22),
        bodyMedium = style(22),
        bodySmall = style(22),
        labelLarge = style(24, FontWeight.Bold),
        labelMedium = style(22, FontWeight.SemiBold),
        labelSmall = style(22, FontWeight.SemiBold),
    )
}

@Composable
fun MiMusicaTheme(textScale: TextScale = TextScale.NORMAL, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WarmColors,
        typography = scaledTypography(textScale.factor),
        content = content,
    )
}
