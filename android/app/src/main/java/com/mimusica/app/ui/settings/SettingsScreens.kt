package com.mimusica.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimusica.app.R
import com.mimusica.app.data.settings.TextScale
import com.mimusica.app.ui.components.BigButton
import com.mimusica.app.ui.components.ErrorBanner
import com.mimusica.app.ui.components.InlineError
import com.mimusica.app.ui.components.MessageBanner
import com.mimusica.app.ui.components.MinTouch
import com.mimusica.app.ui.components.OutlineBigButton
import com.mimusica.app.ui.components.ScreenScaffold
import com.mimusica.app.ui.theme.LocalOsFontScale
import com.mimusica.app.ui.theme.cappedOsFontScale

@Composable
private fun BigField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    secret: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
        label = { Text(label, style = MaterialTheme.typography.bodyLarge) },
        textStyle = MaterialTheme.typography.titleMedium,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
    )
}

@Composable
private fun setupErrorText(error: SetupError): String = stringResource(
    when (error) {
        SetupError.INVALID_URL -> R.string.setup_error_url
        SetupError.EMPTY_TOKEN -> R.string.setup_error_token
        SetupError.INVALID_PIN -> R.string.setup_error_pin
    },
)

/** One-time caregiver setup: server URL, app token and a 4-digit PIN. */
@Composable
fun SetupScreen(
    error: SetupError?,
    onSave: (url: String, token: String, pin: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var url by rememberSaveable { mutableStateOf("") }
    var token by rememberSaveable { mutableStateOf("") }
    var pin by rememberSaveable { mutableStateOf("") }
    ScreenScaffold(title = stringResource(R.string.setup_title), onBack = null, modifier = modifier) {
        Text(stringResource(R.string.setup_intro), style = MaterialTheme.typography.bodyLarge)
        BigField(url, { url = it }, stringResource(R.string.server_url), keyboardType = KeyboardType.Uri)
        BigField(token, { token = it }, stringResource(R.string.app_token))
        BigField(
            pin,
            { pin = it.filter(Char::isDigit).take(4) },
            stringResource(R.string.pin_new_hint),
            keyboardType = KeyboardType.NumberPassword,
            secret = true,
            imeAction = ImeAction.Done,
        )
        error?.let { MessageBanner(setupErrorText(it)) }
        BigButton(stringResource(R.string.setup_save), { onSave(url, token, pin) })
    }
}

/** PIN gate in front of the caregiver area. Explains itself and says what to do when the number is unknown. */
@Composable
fun PinScreen(
    wrongPin: Boolean,
    onSubmit: (String) -> Unit,
    onPinEdited: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pin by rememberSaveable { mutableStateOf("") }
    ScreenScaffold(title = stringResource(R.string.pin_title), onBack = onBack, modifier = modifier) {
        Text(stringResource(R.string.pin_intro), style = MaterialTheme.typography.bodyLarge)
        BigField(
            pin,
            {
                pin = it.filter(Char::isDigit).take(4)
                onPinEdited()
            },
            stringResource(R.string.pin_prompt),
            keyboardType = KeyboardType.NumberPassword,
            secret = true,
            imeAction = ImeAction.Done,
        )
        if (wrongPin) InlineError(stringResource(R.string.pin_wrong))
        BigButton(stringResource(R.string.pin_enter), { onSubmit(pin) })
        Text(stringResource(R.string.pin_help), style = MaterialTheme.typography.bodyLarge)
    }
}

/** "Tamano de letra": open to everyone (no PIN). Three big rows with a live preview, then a separate caregiver door. */
@Composable
fun TextSizeScreen(
    current: TextScale,
    onSelect: (TextScale) -> Unit,
    onCaregiver: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(title = stringResource(R.string.text_size_title), onBack = onBack, modifier = modifier) {
        Text(stringResource(R.string.text_size_intro), style = MaterialTheme.typography.bodyLarge)
        // The whole app re-scales on tap: this line says which size is now active, politely, for TalkBack too.
        val chosen = stringResource(
            when (current) {
                TextScale.NORMAL -> R.string.text_normal
                TextScale.LARGE -> R.string.text_large
                TextScale.EXTRA_LARGE -> R.string.text_xlarge
            },
        )
        Text(
            stringResource(R.string.text_size_chosen, chosen),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.labelMedium,
        )
        TextScaleOption(R.string.text_normal, TextScale.NORMAL, current, onSelect)
        TextScaleOption(R.string.text_large, TextScale.LARGE, current, onSelect)
        TextScaleOption(R.string.text_xlarge, TextScale.EXTRA_LARGE, current, onSelect)
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        CaregiverLink(onCaregiver)
    }
}

/**
 * The door to the caregiver area, kept quiet on purpose: an underlined text link with a one-line note about who
 * it is for, not a button competing with the text sizes. Still a full 64dp touch target.
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
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

/** Caregiver area (unlocked): connection, PIN and manual sync. Text size lives on its own screen. */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onSaveConnection: (url: String, token: String) -> Unit,
    onChangePin: (String) -> Unit,
    onSync: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var url by rememberSaveable(state.settings.serverUrl) { mutableStateOf(state.settings.serverUrl) }
    var token by rememberSaveable(state.settings.token) { mutableStateOf(state.settings.token) }
    var pin by rememberSaveable { mutableStateOf("") }
    ScreenScaffold(title = stringResource(R.string.settings_title), onBack = onBack, modifier = modifier) {
        BigField(url, { url = it }, stringResource(R.string.server_url), keyboardType = KeyboardType.Uri)
        BigField(token, { token = it }, stringResource(R.string.app_token), imeAction = ImeAction.Done)
        state.setupError?.let { MessageBanner(setupErrorText(it)) }
        if (state.saved) MessageBanner(stringResource(R.string.saved), isError = false)
        BigButton(stringResource(R.string.save), { onSaveConnection(url, token) })

        BigField(
            pin,
            { pin = it.filter(Char::isDigit).take(4) },
            stringResource(R.string.pin_new_hint),
            keyboardType = KeyboardType.NumberPassword,
            secret = true,
            imeAction = ImeAction.Done,
        )
        OutlineBigButton(stringResource(R.string.change_pin), { onChangePin(pin); pin = "" })

        // Stays normal-looking while syncing (the disabled style was 2.46:1); the ViewModel ignores repeated taps.
        BigButton(
            text = stringResource(if (state.syncing) R.string.syncing else R.string.sync_now),
            onClick = onSync,
            loading = state.syncing,
        )
        if (state.syncDone) MessageBanner(stringResource(R.string.sync_done), isError = false)
        state.syncError?.let { ErrorBanner(it) }
    }
}

/** One selectable row: the size name and a sentence already drawn at that size. Selected = thick border + check mark. */
@Composable
private fun TextScaleOption(labelRes: Int, option: TextScale, current: TextScale, onSelect: (TextScale) -> Unit) {
    val selected = option == current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(option) }),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            if (selected) 4.dp else 2.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 96.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(40.dp),
            )
            Column {
                Text(stringResource(labelRes), style = MaterialTheme.typography.titleMedium)
                // Drawn at this option's own size (base 22sp x factor), whatever size is active now, and with the
                // same 1.6 combined cap the real setting gets: the phone's font size is capped for THIS option's
                // factor, not for the one in use, so "Muy grande" previews exactly as it will look once chosen.
                val density = LocalDensity.current
                val previewDensity = Density(density.density, cappedOsFontScale(option.factor, LocalOsFontScale.current))
                CompositionLocalProvider(LocalDensity provides previewDensity) {
                    Text(
                        stringResource(R.string.text_size_preview),
                        style = TextStyle(fontSize = (22 * option.factor).sp, lineHeight = (29 * option.factor).sp),
                    )
                }
            }
        }
    }
}
