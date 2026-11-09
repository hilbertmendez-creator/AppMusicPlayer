package com.mimusica.app.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.mimusica.app.R
import com.mimusica.app.ui.components.BigButton
import com.mimusica.app.ui.components.ErrorBanner
import com.mimusica.app.ui.components.InlineError
import com.mimusica.app.ui.components.MessageBanner
import com.mimusica.app.ui.components.OutlineBigButton
import com.mimusica.app.ui.components.ScreenScaffold

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

/** Caregiver area (unlocked): connection, PIN and manual sync. Font and text size live in Ajustes. */
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
