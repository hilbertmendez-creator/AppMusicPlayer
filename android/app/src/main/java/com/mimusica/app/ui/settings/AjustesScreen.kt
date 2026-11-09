package com.mimusica.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimusica.app.R
import com.mimusica.app.data.settings.FontChoice
import com.mimusica.app.data.settings.TextScale
import com.mimusica.app.ui.components.BigButton
import com.mimusica.app.ui.components.EqualRow
import com.mimusica.app.ui.components.InlineError
import com.mimusica.app.ui.components.MinTouch
import com.mimusica.app.ui.components.OutlineBigButton
import com.mimusica.app.ui.components.ScreenScaffold
import com.mimusica.app.ui.components.SectionAccents
import com.mimusica.app.ui.theme.InkSoft
import com.mimusica.app.ui.theme.LocalOsFontScale
import com.mimusica.app.ui.theme.Navy
import com.mimusica.app.ui.theme.OnNavy
import com.mimusica.app.ui.theme.Success
import com.mimusica.app.ui.theme.SunEdge
import com.mimusica.app.ui.theme.SunSoft
import com.mimusica.app.ui.theme.SurfaceFill
import com.mimusica.app.ui.theme.cappedOsFontScale
import com.mimusica.app.ui.theme.fontFamilyFor

/** Height of the three "Tamaño de letra" buttons. */
private val SizeButtonHeight = 76.dp

/**
 * Ajustes: open to everyone (no PIN). The font and the text size apply to the whole app the moment they are
 * chosen. The caregiver door stays at the very bottom as a quiet link.
 */
@Composable
fun AjustesScreen(
    fontChoice: FontChoice,
    textScale: TextScale,
    versionName: String,
    updateUi: AboutUpdateUi,
    onSelectFont: (FontChoice) -> Unit,
    onSelectTextScale: (TextScale) -> Unit,
    onCheckUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onCaregiver: () -> Unit,
    modifier: Modifier = Modifier,
    /** Null when Ajustes is open as a tab: nothing to go back to. */
    onBack: (() -> Unit)? = null,
) {
    ScreenScaffold(
        title = stringResource(R.string.ajustes_title),
        onBack = onBack,
        modifier = modifier,
        accent = SectionAccents.Settings,
    ) {
        FontSection(fontChoice, onSelectFont)
        TextSizeSection(textScale, onSelectTextScale)
        AboutSection(versionName, updateUi, onCheckUpdate, onInstallUpdate)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        CaregiverLink(onCaregiver)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

// --- Fuente de la app -----------------------------------------------------------------------------------------

/** Collapsed: only the font in use. Expanded: the five options. Choosing one applies it at once. */
@Composable
private fun FontSection(current: FontChoice, onSelect: (FontChoice) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier.semantics { isTraversalGroup = true },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionTitle(stringResource(R.string.font_section))
        if (expanded) {
            FontChoice.entries.forEach { FontRow(it, selected = it == current, onClick = { onSelect(it) }) }
        } else {
            FontRow(current, selected = true, onClick = { onSelect(current) })
        }
        OutlineBigButton(
            text = stringResource(if (expanded) R.string.font_done else R.string.font_change),
            onClick = { expanded = !expanded },
            color = Navy,
        )
    }
}

/** One font, drawn in its own typeface. Selected = sun-soft fill, a 2dp sun-edge border and a check mark. */
@Composable
private fun FontRow(option: FontChoice, selected: Boolean, onClick: () -> Unit) {
    val family = fontFamilyFor(option)
    val description = stringResource(R.string.font_option_desc, option.displayName)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        shape = RoundedCornerShape(16.dp),
        color = if (selected) SunSoft else SurfaceFill,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = if (selected) BorderStroke(2.dp, SunEdge) else null,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = MinTouch)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.home_text_size_pill),
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = family),
            )
            Text(
                option.displayName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = family),
            )
            if (selected) {
                // Decorative: the selected state is already part of the row's semantics.
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SunEdge, modifier = Modifier.size(32.dp))
            }
        }
    }
}

// --- Tamaño de letra ------------------------------------------------------------------------------------------

@Composable
private fun TextSizeSection(current: TextScale, onSelect: (TextScale) -> Unit) {
    Column(
        modifier = Modifier.semantics { isTraversalGroup = true },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionTitle(stringResource(R.string.text_size_title))
        EqualRow {
            TextScale.entries.forEach { SizeButton(it, selected = it == current, onClick = { onSelect(it) }) }
        }
        // The whole app re-scales on tap: this line says which size is now active, politely, for TalkBack too.
        Text(
            stringResource(R.string.text_size_chosen, stringResource(sizeLabel(current))),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.labelMedium,
            color = InkSoft,
        )
    }
}

private fun sizeLabel(scale: TextScale): Int = when (scale) {
    TextScale.NORMAL -> R.string.text_normal
    TextScale.LARGE -> R.string.text_large
    TextScale.EXTRA_LARGE -> R.string.text_xlarge
}

/** "Aa" drawn at this option's own size above its name. Selected = navy fill with white text. */
@Composable
private fun SizeButton(option: TextScale, selected: Boolean, onClick: () -> Unit) {
    val label = stringResource(sizeLabel(option))
    val description = stringResource(R.string.text_size_option_desc, label)
    Surface(
        modifier = Modifier
            .selectable(selected = selected, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        shape = RoundedCornerShape(16.dp),
        color = if (selected) Navy else SurfaceFill,
        contentColor = if (selected) OnNavy else MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .heightIn(min = SizeButtonHeight)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Drawn at this option's own size (22sp x factor) with the same 1.6 combined cap the real setting gets,
            // so "Muy grande" previews exactly as it will look once chosen, whatever size is active now.
            val density = LocalDensity.current
            val previewDensity = Density(density.density, cappedOsFontScale(option.factor, LocalOsFontScale.current))
            CompositionLocalProvider(LocalDensity provides previewDensity) {
                Text(
                    stringResource(R.string.home_text_size_pill),
                    modifier = Modifier.testTag("size_sample_${option.name}"),
                    style = TextStyle(
                        fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
                        fontSize = (22 * option.factor).sp,
                        lineHeight = (29 * option.factor).sp,
                    ),
                )
            }
            Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
        }
    }
}

// --- Acerca de ------------------------------------------------------------------------------------------------

@Composable
private fun AboutSection(
    versionName: String,
    updateUi: AboutUpdateUi,
    onCheckUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
) {
    Column(
        modifier = Modifier.semantics { isTraversalGroup = true },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionTitle(stringResource(R.string.about_title))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = SurfaceFill,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium)
                Text(
                    versionLabel(stringResource(R.string.about_version), versionName),
                    style = MaterialTheme.typography.bodyLarge,
                    color = InkSoft,
                )
                UpdateStatus(updateUi, onInstallUpdate)
                BigButton(
                    text = stringResource(R.string.update_check),
                    onClick = onCheckUpdate,
                    minHeight = MinTouch,
                )
            }
        }
    }
}

/** One line about updates. "Ya tienes la última versión" is plain green text with a check, never a button shape. */
@Composable
private fun UpdateStatus(ui: AboutUpdateUi, onInstall: () -> Unit) {
    when (ui) {
        AboutUpdateUi.Idle -> Unit
        AboutUpdateUi.Checking -> PoliteLine(stringResource(R.string.update_checking))
        AboutUpdateUi.UpToDate -> Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Success.text, modifier = Modifier.size(32.dp))
            Text(
                stringResource(R.string.update_up_to_date),
                style = MaterialTheme.typography.labelLarge,
                color = Success.text,
            )
        }
        AboutUpdateUi.CheckFailed -> InlineError(stringResource(R.string.update_check_failed))
        AboutUpdateUi.NotChecked -> Text(
            stringResource(R.string.update_not_checked),
            style = MaterialTheme.typography.bodyLarge,
            color = InkSoft,
        )
        AboutUpdateUi.Available -> {
            PoliteLine(stringResource(R.string.update_available), MaterialTheme.typography.titleMedium)
            BigButton(stringResource(R.string.update_install), onInstall, minHeight = MinTouch)
        }
        AboutUpdateUi.Downloading -> {
            PoliteLine(stringResource(R.string.update_downloading), MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.update_wait), style = MaterialTheme.typography.bodyLarge, color = InkSoft)
        }
        AboutUpdateUi.DownloadFailed -> {
            InlineError(stringResource(R.string.update_failed))
            BigButton(stringResource(R.string.download_retry), onInstall, minHeight = MinTouch)
        }
    }
}

@Composable
private fun PoliteLine(text: String, style: TextStyle = MaterialTheme.typography.bodyLarge) {
    Text(text, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }, style = style)
}

// --- Para quien ayuda -----------------------------------------------------------------------------------------

/**
 * The door to the caregiver area, kept quiet on purpose: an underlined text link with a one-line note about who
 * it is for, not a button competing with the settings above. Still a full 64dp touch target.
 */
@Composable
private fun CaregiverLink(onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        TextButton(
            onClick = onClick,
            modifier = Modifier.heightIn(min = MinTouch),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        ) {
            Text(
                stringResource(R.string.caregiver_entry),
                style = MaterialTheme.typography.labelMedium.copy(textDecoration = TextDecoration.Underline),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            stringResource(R.string.caregiver_note),
            modifier = Modifier.padding(horizontal = 4.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = InkSoft,
        )
    }
}
