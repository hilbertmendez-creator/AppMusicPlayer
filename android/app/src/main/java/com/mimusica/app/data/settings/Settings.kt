package com.mimusica.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mimusica.app.data.LastSungStore
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

/**
 * The typeface of the whole app. [id] is what gets stored (never rename one); [googleName] is the Google Fonts
 * family to download, null for the phone's own sans.
 */
enum class FontChoice(val id: String, val displayName: String, val googleName: String?) {
    ATKINSON("atkinson", "Atkinson Hyperlegible", "Atkinson Hyperlegible"),
    LEXEND("lexend", "Lexend", "Lexend"),
    ANDIKA("andika", "Andika", "Andika"),
    SOURCE_SANS("source_sans", "Source Sans 3", "Source Sans 3"),
    SYSTEM("system", "Del sistema", null);

    companion object {
        /** Unknown or missing ids fall back to the default so a bad value can never break start-up. */
        fun parse(id: String?): FontChoice {
            val wanted = id?.trim().orEmpty()
            return entries.firstOrNull { it.id.equals(wanted, ignoreCase = true) } ?: ATKINSON
        }
    }
}

data class AppSettings(
    val serverUrl: String = "",
    val token: String = "",
    val pinHash: String? = null,
    val textScale: TextScale = TextScale.NORMAL,
    val fontChoice: FontChoice = FontChoice.ATKINSON,
) {
    /** True once the caregiver finished the one-time setup. */
    val isConfigured: Boolean get() = serverUrl.isNotBlank() && token.isNotBlank() && pinHash != null
}

interface SettingsStore {
    val settings: Flow<AppSettings>
    suspend fun saveConnection(serverUrl: String, token: String)
    suspend fun savePinHash(pinHash: String)
    suspend fun saveTextScale(scale: TextScale)
    suspend fun saveFontChoice(choice: FontChoice)
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

class DataStoreSettingsStore(private val context: Context) : SettingsStore, SyncStateStore, LastSungStore {
    private val urlKey = stringPreferencesKey("server_url")
    private val tokenKey = stringPreferencesKey("token")
    private val pinKey = stringPreferencesKey("pin_hash")
    private val scaleKey = stringPreferencesKey("text_scale")
    private val fontKey = stringPreferencesKey("font_choice")
    private val libraryIdKey = stringPreferencesKey("last_library_id")
    private val lastSungKey = stringPreferencesKey("last_sung_song_id")

    override val lastSungSongId: Flow<String?> = context.dataStore.data.map { it[lastSungKey] }

    override suspend fun setLastSungSongId(id: String) {
        context.dataStore.edit { it[lastSungKey] = id }
    }

    override val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            serverUrl = p[urlKey].orEmpty(),
            token = p[tokenKey].orEmpty(),
            pinHash = p[pinKey],
            textScale = p[scaleKey]?.let { runCatching { TextScale.valueOf(it) }.getOrNull() }
                ?: TextScale.NORMAL,
            fontChoice = FontChoice.parse(p[fontKey]),
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

    override suspend fun saveFontChoice(choice: FontChoice) {
        context.dataStore.edit { it[fontKey] = choice.id }
    }
}
