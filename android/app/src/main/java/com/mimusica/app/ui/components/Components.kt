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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mimusica.app.R
import com.mimusica.app.ui.state.UiError
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
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
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

@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier, dark: Boolean = false) {
    val color = if (dark) Color.White else MaterialTheme.colorScheme.onSurface
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = MinTouch),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, color),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.back), style = MaterialTheme.typography.labelLarge)
    }
}

/** Standard screen: a big "Volver" button, a large title (a TalkBack heading) and scrollable content. */
@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
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
            if (onBack != null) BackButton(onBack)
            Text(
                title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
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

/** Confirmation before any delete: big "Sí, borrar" and "No". */
@Composable
fun ConfirmDeleteDialog(message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = null,
        text = { Text(message, style = MaterialTheme.typography.titleMedium) },
        confirmButton = {
            BigButton(
                text = stringResource(R.string.yes_delete),
                onClick = onConfirm,
                minHeight = MinTouch,
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            )
        },
        dismissButton = {
            OutlineBigButton(text = stringResource(R.string.no), onClick = onDismiss)
        },
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
            .semantics { liveRegion = LiveRegionMode.Polite },
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
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text,
            modifier = Modifier.fillMaxWidth(),
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
fun DemotedDangerAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.heightIn(min = MinTouch),
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

/** Card container used for list rows: a slightly lighter surface with the outline kept. */
@Composable
fun RowCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

/** Non-clickable success badge with a big readable label, used instead of a disabled button. */
@Composable
fun StatusPill(text: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = BigButtonHeight)
            .background(Success.container, RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Success.onContainer, modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = Success.onContainer)
    }
}
