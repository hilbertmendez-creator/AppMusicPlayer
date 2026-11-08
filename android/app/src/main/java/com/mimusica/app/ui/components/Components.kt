package com.mimusica.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mimusica.app.R
import com.mimusica.app.ui.state.UiError
import com.mimusica.app.ui.theme.OnSolid
import com.mimusica.app.ui.theme.Success

/** Minimum touch target for every control in the app (senior-friendly). */
val MinTouch = 64.dp

/** Default height of a primary button. */
val BigButtonHeight = 72.dp

@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    minHeight: Dp = BigButtonHeight,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    description: String? = null,
    loading: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
    ) {
        // While busy the button keeps its normal colours (a greyed-out disabled button is unreadable);
        // the spinner replaces the icon and the caller ignores repeated taps.
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), color = contentColor, strokeWidth = 3.dp)
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        } else {
            ButtonContent(text, icon)
        }
    }
}

@Composable
fun OutlineBigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    minHeight: Dp = MinTouch,
    color: Color = MaterialTheme.colorScheme.onSurface,
    description: String? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, color),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
    ) {
        ButtonContent(text, icon)
    }
}

@Composable
private fun ButtonContent(text: String, icon: ImageVector?) {
    if (icon != null) {
        // Decorative: the text next to it already tells TalkBack what the button does.
        Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(12.dp))
    }
    Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
}

/** Round icon-only button; [description] is read by TalkBack. */
@Composable
fun BigIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    border: BorderStroke? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(size)
            .semantics { role = Role.Button },
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        border = border,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(icon, contentDescription = description, modifier = Modifier.size(size * 0.55f))
        }
    }
}

/**
 * "Volver": icon + word, a thin outline and the minimum touch height. Quieter than the primary buttons on
 * purpose, it is the way out of a screen and not what the screen is for.
 */
@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier, dark: Boolean = false) {
    val color = if (dark) Color.White else MaterialTheme.colorScheme.onSurface
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = MinTouch),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.back), style = MaterialTheme.typography.labelMedium)
    }
}

private val HeaderGap = 12.dp

/** Width the accent chip takes in front of the title: the 44dp chip and its 10dp gap. */
private val ChipSlot = 54.dp

/** Optional accent chip (the Home tile's icon in its colour) followed by the title, which is a TalkBack heading. */
@Composable
private fun TitleWithAccent(title: String, accent: SectionAccent?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (accent != null) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(accent.color, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                // Decorative: the title next to it already names the screen.
                Icon(accent.icon, contentDescription = null, tint = OnSolid, modifier = Modifier.size(28.dp))
            }
        }
        Text(
            title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/**
 * "Volver" and the title share one row so the content starts high on the screen. They go side by side whenever
 * the longest word of the title still fits beside the button (a long title wraps between words); when it does
 * not, as at the largest text sizes, the title drops below "Volver" instead of being squeezed or clipped.
 */
@Composable
private fun ScreenHeader(title: String, onBack: (() -> Unit)?, accent: SectionAccent?) {
    val measurer = rememberTextMeasurer()
    val titleStyle = MaterialTheme.typography.headlineMedium
    // Measured, not asked of the layout system: the widest word of the title, so a word is never broken or squeezed.
    val longestWordPx = title.split(' ').maxOfOrNull {
        measurer.measure(it, titleStyle, maxLines = 1, softWrap = false).size.width
    } ?: 0
    if (onBack == null) {
        TitleWithAccent(title, accent)
        return
    }
    Layout(
        content = {
            BackButton(onBack)
            TitleWithAccent(title, accent)
        },
        modifier = Modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val gap = HeaderGap.roundToPx()
        val backMeasurable = measurables[0]
        val titleMeasurable = measurables[1]
        val titleNeeds = longestWordPx + if (accent != null) (ChipSlot).roundToPx() else 0
        // Natural size of the button (measured loose), so its own label can never be squeezed.
        val back = backMeasurable.measure(Constraints(maxWidth = width))
        val backWidth = back.width
        if (backWidth + gap + titleNeeds <= width) {
            val title = titleMeasurable.measure(Constraints(maxWidth = width - backWidth - gap))
            val height = maxOf(back.height, title.height)
            layout(width, height) {
                back.placeRelative(0, (height - back.height) / 2)
                title.placeRelative(backWidth + gap, (height - title.height) / 2)
            }
        } else {
            val title = titleMeasurable.measure(Constraints(maxWidth = width))
            layout(width, back.height + gap + title.height) {
                back.placeRelative(0, 0)
                title.placeRelative(0, back.height + gap)
            }
        }
    }
}

/**
 * Standard screen: a quiet "Volver" and a large title on one row (stacked only when the text is too big for
 * that), then scrollable content. [accent] opts a destination screen into its Home tile's colour: a chip next
 * to the title and a thin bar under the header. It never recolours anything else.
 */
@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    accent: SectionAccent? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ScreenHeader(title, onBack, accent)
                if (accent != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(accent.color, RoundedCornerShape(3.dp)),
                    )
                }
            }
            val bodyModifier = if (scrollable) {
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            } else {
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            }
            Column(modifier = bodyModifier, verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
        }
    }
}

/**
 * Confirmation before any delete. The SAFE choice is the primary button and comes first: "No, conservarla"
 * (filled, 72dp). The destructive "Sí, borrar" is secondary: an outlined red button. Both are >= 64dp.
 * [note] is an optional reassurance under the question (e.g. that the song can be downloaded again).
 */
@Composable
fun ConfirmDeleteDialog(message: String, onConfirm: () -> Unit, onDismiss: () -> Unit, note: String? = null) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = null,
        // Buttons live in the content, stacked in an explicit order: Material's button slots wrap
        // full-width buttons with the confirm (destructive) one on top, which is the wrong first choice here.
        // Scrolls: at the largest text size (or on a small phone) the buttons must stay reachable, never cut off.
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(message, style = MaterialTheme.typography.titleMedium)
                if (note != null) Text(note, style = MaterialTheme.typography.bodyLarge)
                BigButton(text = stringResource(R.string.keep_it), onClick = onDismiss)
                OutlineBigButton(
                    text = stringResource(R.string.yes_delete),
                    onClick = onConfirm,
                    minHeight = MinTouch,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        confirmButton = {},
    )
}

@Composable
fun errorText(error: UiError): String = stringResource(
    when (error) {
        UiError.NO_INTERNET -> R.string.error_no_internet
        UiError.UNAUTHORIZED -> R.string.error_unauthorized
        UiError.SERVER -> R.string.error_server
        UiError.UNKNOWN -> R.string.error_unknown
    },
)

/** Boxed message. Red for errors, green for success only. Announced politely by TalkBack. */
@Composable
fun MessageBanner(message: String, modifier: Modifier = Modifier, isError: Boolean = true) {
    val color = if (isError) MaterialTheme.colorScheme.error else Success.text
    Surface(
        modifier = modifier
            .fillMaxWidth()
            // Merged: the message is one node that is both the text and the live region (TalkBack reads it once).
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(2.dp, color),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Decorative: the message already says it all; the icon makes it not rely on colour alone.
            Icon(
                if (isError) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp),
            )
            Text(message, style = MaterialTheme.typography.bodyLarge, color = color)
        }
    }
}

@Composable
fun ErrorBanner(error: UiError, modifier: Modifier = Modifier) = MessageBanner(errorText(error), modifier)

/** Friendly empty state. Pass [actionText] and [onAction] so nobody is left at a dead end. */
@Composable
fun EmptyMessage(
    text: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    actionIcon: ImageVector? = null,
    /** Announce the text politely when it appears (an empty search result shows up without any focus change). */
    live: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (live) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        if (actionText != null && onAction != null) BigButton(actionText, onAction, icon = actionIcon)
    }
}

/** Plain error line with an icon, shown right under the field it belongs to (not a button-like box). */
@Composable
fun InlineError(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(28.dp),
        )
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
    }
}

/**
 * A risky or rarely needed action, kept clearly apart from the main ones: smaller, not full width,
 * pushed to the end and 16dp of its own spacing on top of the column gap (>= 24dp from the button above).
 * Still >= 64dp tall. Callers must still confirm before doing anything destructive.
 */
@Composable
fun DemotedDangerAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    description: String? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier
                .heightIn(min = MinTouch)
                .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Whole-row button for a list entry: title line, separate secondary line, chevron. Left aligned. */
@Composable
fun ListRowButton(title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .semantics { role = Role.Button },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(subtitle, style = MaterialTheme.typography.bodyLarge)
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

/** Small informative label on a list row ("Instrumental", "Letra completa"); never tappable. */
@Composable
fun TagChip(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/** Card container used for list rows: a slightly lighter surface with the outline kept. */
@Composable
fun RowCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    // A 2dp dark outline plus a soft shadow: white cards on a pale page must read as separate pieces at a glance.
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 4.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

/**
 * Success status as a plain line: green check icon + green text sitting on the card, with no filled slab and
 * no button shape, so it never looks tappable (DeepGreen on Card is >= 7:1). Pass a live-region modifier to
 * have TalkBack announce it politely when it appears.
 */
@Composable
fun StatusPill(text: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Success.text, modifier = Modifier.size(32.dp))
        Text(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = Success.text)
    }
}

/**
 * A big primary action with a smaller secondary one beside it ("Reproducir" + "Borrar"). When both fit on one
 * line the secondary keeps its natural width at the end and the primary takes the rest; when the text is too
 * large for that (or the screen too narrow) they stack: primary full width, secondary below at the end.
 * Labels therefore never break mid-word and never overlap.
 */
@Composable
fun PrimaryWithSecondary(
    primary: @Composable () -> Unit,
    secondary: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    gap: Dp = 12.dp,
) {
    Layout(content = { primary(); secondary() }, modifier = modifier.fillMaxWidth()) { measurables, constraints ->
        val width = constraints.maxWidth
        val gapPx = gap.roundToPx()
        val primaryMeasurable = measurables[0]
        val secondaryMeasurable = measurables[1]
        val secondaryNatural = secondaryMeasurable.maxIntrinsicWidth(Constraints.Infinity).coerceAtMost(width)
        val primaryNeeds = primaryMeasurable.minIntrinsicWidth(Constraints.Infinity)
        val sideBySide = primaryNeeds + gapPx + secondaryNatural <= width
        if (sideBySide) {
            val secondary = secondaryMeasurable.measure(Constraints.fixedWidth(secondaryNatural))
            val primary = primaryMeasurable.measure(Constraints.fixedWidth(width - gapPx - secondaryNatural))
            val height = maxOf(primary.height, secondary.height)
            layout(width, height) {
                primary.placeRelative(0, (height - primary.height) / 2)
                secondary.placeRelative(width - secondaryNatural, (height - secondary.height) / 2)
            }
        } else {
            val primary = primaryMeasurable.measure(Constraints.fixedWidth(width))
            val secondary = secondaryMeasurable.measure(Constraints.fixedWidth(secondaryNatural))
            layout(width, primary.height + gapPx + secondary.height) {
                primary.placeRelative(0, 0)
                secondary.placeRelative(width - secondaryNatural, primary.height + gapPx)
            }
        }
    }
}

/**
 * Two buttons of the same weight side by side, each half the width and the same height ("Hablar" + "Buscar").
 * When the text is too large for a word to fit in half the width (or the screen is too narrow) they stack,
 * [first] on top, so a label never breaks mid-word.
 */
@Composable
fun TwoUp(
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    gap: Dp = 12.dp,
) {
    Layout(content = { first(); second() }, modifier = modifier.fillMaxWidth()) { measurables, constraints ->
        val width = constraints.maxWidth
        val gapPx = gap.roundToPx()
        val half = (width - gapPx) / 2
        val a = measurables[0]
        val b = measurables[1]
        val sideBySide = a.minIntrinsicWidth(Constraints.Infinity) <= half && b.minIntrinsicWidth(Constraints.Infinity) <= half
        if (sideBySide) {
            // Intrinsic heights (a child may only be measured once): both buttons get the taller one's height.
            val height = maxOf(a.minIntrinsicHeight(half), b.minIntrinsicHeight(half))
            val pa = a.measure(Constraints(minWidth = half, maxWidth = half, minHeight = height, maxHeight = height))
            val pb = b.measure(Constraints(minWidth = half, maxWidth = half, minHeight = height, maxHeight = height))
            val h = maxOf(pa.height, pb.height)
            layout(width, h) {
                pa.placeRelative(0, 0)
                pb.placeRelative(half + gapPx, 0)
            }
        } else {
            val pa = a.measure(Constraints.fixedWidth(width))
            val pb = b.measure(Constraints.fixedWidth(width))
            layout(width, pa.height + gapPx + pb.height) {
                pa.placeRelative(0, 0)
                pb.placeRelative(0, pa.height + gapPx)
            }
        }
    }
}

/** Quiet red outlined button (no icon, text only) for a destructive action that must stay reachable but secondary. */
@Composable
fun DangerOutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, description: String? = null) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = MinTouch)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium)
    }
}
