package com.mimusica.app.ui.theme

import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.mimusica.app.R
import com.mimusica.app.data.settings.FontChoice

private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

/** The only weights the typography uses (Normal body, SemiBold titles, Bold headings). */
private val usedWeights = listOf(FontWeight.Normal, FontWeight.SemiBold, FontWeight.Bold)

private val familyCache = mutableMapOf<FontChoice, FontFamily>()

/**
 * The [FontFamily] for a choice. Downloadable families list the Google font and, right after it, the phone's own
 * sans for the same weight: until the download finishes (or if it never does: no network, no Google Play
 * services) text is drawn in the system sans instead of staying blank.
 */
fun fontFamilyFor(choice: FontChoice): FontFamily {
    val name = choice.googleName ?: return FontFamily.SansSerif
    return familyCache.getOrPut(choice) {
        val google = GoogleFont(name)
        FontFamily(
            usedWeights.flatMap { weight ->
                listOf(
                    Font(googleFont = google, fontProvider = googleFontProvider, weight = weight),
                    Font(DeviceFontFamilyName("sans-serif"), weight),
                )
            },
        )
    }
}
