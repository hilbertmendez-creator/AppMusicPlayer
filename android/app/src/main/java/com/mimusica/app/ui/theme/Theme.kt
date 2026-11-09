package com.mimusica.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.mimusica.app.data.settings.FontChoice
import com.mimusica.app.data.settings.TextScale

/*
 * White page, cool-grey surfaces and one navy for every action. Every text/background pair is >= 7:1 (WCAG AAA),
 * checked by PaletteContrastTest. Colour meanings are fixed:
 *   navy  = every action button (and the "Mis canciones" tile)
 *   sun   = the active tab and "Cantar con la letra"
 *   green = success only ("listo", "guardado", "ya está en tus canciones")
 *   red   = delete / error only
 * Section colours are icon and bar fills only, never text.
 */
val Page = Color(0xFFFFFFFF)
val SurfaceFill = Color(0xFFF2F5F9)
val Ink = Color(0xFF0F2742)
val InkSoft = Color(0xFF3D516B)
val Line = Color(0xFFC5CFDB)
val Navy = Color(0xFF123C69)
val OnNavy = Color(0xFFFFFFFF)
val Sun = Color(0xFFFFD166)
val SunSoft = Color(0xFFFFE7A8)
val SunEdge = Color(0xFF8A6100)
val SuccessGreen = Color(0xFF1B5E20)
val Danger = Color(0xFF9B0000)
val SectionLists = Color(0xFF6A1B6B)
val SectionSearch = Color(0xFF00585C)
val SectionSing = Color(0xFF7A3E00)
val SectionSettings = Color(0xFF374151)
val ChipBg = Color(0xFFE3ECFA)
val CoverBg = Color(0xFFDCE6F2)

// Thin aliases kept so the many existing call sites keep compiling; new code uses the names above.
val OnSolid = OnNavy

/** Window / splash background. Must equal `@color/paper` so there is no white flash. */
val Paper = Page
val Card = Page
val Outline = InkSoft
val DeepBlue = Navy
val SoftBlue = ChipBg
val DeepGreen = SuccessGreen
val DeepRed = Danger
val Plum = SectionLists
val DeepTeal = SectionSearch
val AmberBrown = SectionSing

/** Progress bar: a visible track (>= 3:1 on Page and surfaces) and a dark fill (>= 3:1 on the track). */
val ProgressTrack = Color(0xFF7B8794)
val ProgressFill = Color(0xFF0A2F73)

val KaraokeBackground = Color(0xFF000000)
val KaraokeCurrent = Color(0xFFFFEB3B)
val KaraokeDim = Color(0xFFB0B0B0)

private val AppColors = lightColorScheme(
    primary = Navy,
    onPrimary = OnNavy,
    primaryContainer = ChipBg,
    onPrimaryContainer = Ink,
    // Neutral on purpose: green is reserved for success (see [Success]), so no control uses `secondary`.
    secondary = InkSoft,
    onSecondary = OnNavy,
    background = Page,
    onBackground = Ink,
    surface = Page,
    onSurface = Ink,
    surfaceVariant = SurfaceFill,
    onSurfaceVariant = Ink,
    error = Danger,
    onError = OnNavy,
    outline = InkSoft,
    outlineVariant = Line,
)

/** Success colours: the only place green appears. */
object Success {
    val container = SuccessGreen
    val onContainer = OnNavy
    val text = SuccessGreen
}

/** Every size is >= 22sp for body text; [scale] multiplies the whole scale (Tamaño de letra). */
fun scaledTypography(scale: Float, fontFamily: FontFamily = FontFamily.Default): Typography {
    fun style(size: Int, weight: FontWeight = FontWeight.Normal, lineHeight: Int = (size * 1.3f).toInt()) =
        TextStyle(
            fontFamily = fontFamily,
            fontSize = (size * scale).sp,
            fontWeight = weight,
            lineHeight = (lineHeight * scale).sp,
        )
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

/** Largest combined text scale (in-app "Tamaño de letra" x the phone's font size) any screen is asked to handle. */
const val MaxCombinedTextScale = 1.6f

/**
 * The OS font scale to hand to [LocalDensity] so that in-app [appFactor] x it never exceeds
 * [MaxCombinedTextScale]. It only ever lowers the OS value, so the in-app choice survives whenever the product
 * is already within the cap (e.g. app 1.2 at OS 1.0 stays 1.2).
 */
fun cappedOsFontScale(appFactor: Float, osFontScale: Float): Float =
    osFontScale.coerceAtMost(MaxCombinedTextScale / appFactor.coerceAtLeast(0.01f))

/** The text scale a user actually gets: in-app factor x capped OS font scale. */
fun effectiveTextScale(appFactor: Float, osFontScale: Float): Float =
    appFactor * cappedOsFontScale(appFactor, osFontScale)

/**
 * The phone's own font size as the system reports it, before [MiMusicaTheme] caps it for [LocalDensity].
 * Previews of other text sizes (the "Tamaño de letra" rows) need it to apply the same cap to their own factor.
 */
val LocalOsFontScale = staticCompositionLocalOf { 1f }

@Composable
fun MiMusicaTheme(
    textScale: TextScale = TextScale.NORMAL,
    fontChoice: FontChoice = FontChoice.ATKINSON,
    content: @Composable () -> Unit,
) {
    // The typography already multiplies sp by the in-app factor, and the OS font scale multiplies it again at
    // draw time. Capping the OS part keeps the worst case (1.2 x 2.0) from breaking every layout.
    val density = LocalDensity.current
    val osScale = cappedOsFontScale(textScale.factor, density.fontScale)
    val cappedDensity = if (osScale == density.fontScale) density else Density(density.density, osScale)
    CompositionLocalProvider(LocalDensity provides cappedDensity, LocalOsFontScale provides density.fontScale) {
        MaterialTheme(
            colorScheme = AppColors,
            typography = remember(textScale, fontChoice) {
                scaledTypography(textScale.factor, fontFamilyFor(fontChoice))
            },
            content = content,
        )
    }
}
