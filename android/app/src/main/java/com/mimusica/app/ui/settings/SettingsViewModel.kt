package com.mimusica.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mimusica.app.data.settings.AppSettings
import com.mimusica.app.data.settings.PinHasher
import com.mimusica.app.data.settings.SettingsStore
import com.mimusica.app.data.settings.TextScale
import com.mimusica.app.ui.state.UiError
import com.mimusica.app.ui.state.runCatchingUi
import com.mimusica.app.ui.state.toUiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

enum class SetupError { INVALID_URL, EMPTY_TOKEN, INVALID_PIN }

object SettingsValidation {
    fun isValidPin(pin: String): Boolean = pin.length == 4 && pin.all { it in '0'..'9' }

    /** Trims, strips trailing slashes and requires an http(s) URL; null when it is not usable. */
    fun normalizeUrl(raw: String): String? {
        val cleaned = raw.trim().trimEnd('/')
        if (!cleaned.startsWith("http://") && !cleaned.startsWith("https://")) return null
        return cleaned.takeIf { it.toHttpUrlOrNull() != null }
    }
}

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val loaded: Boolean = false,
    val unlocked: Boolean = false,
    val pinError: Boolean = false,
    val setupError: SetupError? = null,
    val syncing: Boolean = false,
    val syncDone: Boolean = false,
    val syncError: UiError? = null,
    val saved: Boolean = false,
)

class SettingsViewModel(
    private val store: SettingsStore,
    private val sync: suspend () -> Unit,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            store.settings.collect { s -> _uiState.update { it.copy(settings = s, loaded = true) } }
        }
    }

    fun completeSetup(url: String, token: String, pin: String) {
        val normalized = SettingsValidation.normalizeUrl(url)
        val error = when {
            normalized == null -> SetupError.INVALID_URL
            token.isBlank() -> SetupError.EMPTY_TOKEN
            !SettingsValidation.isValidPin(pin) -> SetupError.INVALID_PIN
            else -> null
        }
        if (error != null || normalized == null) {
            _uiState.update { it.copy(setupError = error) }
            return
        }
        _uiState.update { it.copy(setupError = null) }
        viewModelScope.launch {
            store.saveConnection(normalized, token.trim())
            store.savePinHash(PinHasher.hash(pin))
            _uiState.update { it.copy(unlocked = true) }
        }
    }

    fun unlock(pin: String) {
        val hash = _uiState.value.settings.pinHash
        val ok = hash != null && PinHasher.verify(pin, hash)
        _uiState.update { it.copy(unlocked = ok, pinError = !ok) }
    }

    /** The wrong-number message goes away as soon as the person starts typing again. */
    fun onPinEdited() {
        if (_uiState.value.pinError) _uiState.update { it.copy(pinError = false) }
    }

    fun lock() {
        _uiState.update { it.copy(unlocked = false, pinError = false, saved = false, syncDone = false) }
    }

    fun saveConnection(url: String, token: String) {
        val normalized = SettingsValidation.normalizeUrl(url)
        val error = when {
            normalized == null -> SetupError.INVALID_URL
            token.isBlank() -> SetupError.EMPTY_TOKEN
            else -> null
        }
        if (error != null || normalized == null) {
            _uiState.update { it.copy(setupError = error, saved = false) }
            return
        }
        viewModelScope.launch {
            store.saveConnection(normalized, token.trim())
            _uiState.update { it.copy(setupError = null, saved = true) }
        }
    }

    fun changePin(pin: String) {
        if (!SettingsValidation.isValidPin(pin)) {
            _uiState.update { it.copy(setupError = SetupError.INVALID_PIN, saved = false) }
            return
        }
        viewModelScope.launch {
            store.savePinHash(PinHasher.hash(pin))
            _uiState.update { it.copy(setupError = null, saved = true) }
        }
    }

    fun setTextScale(scale: TextScale) {
        viewModelScope.launch { store.saveTextScale(scale) }
    }

    fun syncNow() {
        // The button keeps its normal look while busy, so a second tap must do nothing.
        if (_uiState.value.syncing) return
        _uiState.update { it.copy(syncing = true, syncDone = false, syncError = null) }
        viewModelScope.launch {
            val result = runCatchingUi { sync() }
            _uiState.update {
                it.copy(syncing = false, syncDone = result.isSuccess, syncError = result.exceptionOrNull()?.toUiError())
            }
        }
    }
}
