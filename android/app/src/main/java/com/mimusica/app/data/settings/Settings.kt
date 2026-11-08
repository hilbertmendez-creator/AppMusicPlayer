package com.mimusica.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mimusica.app.data.SyncStateStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

enum class TextScale(val factor: Float) {
    NORMAL(0.8f),
    LARGE(1.0f),
    EXTRA_LARGE(1.2f),
}

data class AppSettings(
    val serverUrl: String = "",
    val token: String = "",
    val pinHash: String? = null,
    val textScale: TextScale = TextScale.NORMAL,
) {
    /** True once the caregiver finished the one-time setup. */
    val isConfigured: Boolean get() = serverUrl.isNotBlank() && token.isNotBlank() && pinHash != null
}

interface SettingsStore {
    val settings: Flow<AppSettings>
    suspend fun saveConnection(serverUrl: String, token: String)
    suspend fun savePinHash(pinHash: String)
    suspend fun saveTextScale(scale: TextScale)
}

/** PIN hashing. The PIN is only a casual guard against accidental taps, not a security boundary. */
object PinHasher {
    private const val SALT = "mimusica:v1:"

    fun hash(pin: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest((SALT + pin).toByteArray())
            .joinToString("") { "%02x".format(it) }

    fun verify(pin: String, hash: String): Boolean = hash(pin) == hash
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreSettingsStore(private val context: Context) : SettingsStore, SyncStateStore {
    private val urlKey = stringPreferencesKey("server_url")
    private val tokenKey = stringPreferencesKey("token")
    private val pinKey = stringPreferencesKey("pin_hash")
    private val scaleKey = stringPreferencesKey("text_scale")
    private val libraryIdKey = stringPreferencesKey("last_library_id")

    override val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            serverUrl = p[urlKey].orEmpty(),
            token = p[tokenKey].orEmpty(),
            pinHash = p[pinKey],
            textScale = p[scaleKey]?.let { runCatching { TextScale.valueOf(it) }.getOrNull() }
                ?: TextScale.NORMAL,
        )
    }

    override suspend fun saveConnection(serverUrl: String, token: String) {
        context.dataStore.edit {
            it[urlKey] = serverUrl
            it[tokenKey] = token
        }
    }

    override suspend fun savePinHash(pinHash: String) {
        context.dataStore.edit { it[pinKey] = pinHash }
    }

    override suspend fun lastLibraryId(): String? = context.dataStore.data.first()[libraryIdKey]

    override suspend fun setLastLibraryId(id: String) {
        context.dataStore.edit { it[libraryIdKey] = id }
    }

    override suspend fun saveTextScale(scale: TextScale) {
        context.dataStore.edit { it[scaleKey] = scale.name }
    }
}
