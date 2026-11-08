package com.mimusica.app.ui

import com.mimusica.app.data.settings.AppSettings
import com.mimusica.app.data.settings.PinHasher
import com.mimusica.app.data.settings.SettingsStore
import com.mimusica.app.data.settings.TextScale
import com.mimusica.app.fakes.MainDispatcherRule
import com.mimusica.app.ui.settings.SettingsValidation
import com.mimusica.app.ui.settings.SettingsViewModel
import com.mimusica.app.ui.settings.SetupError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private class FakeStore : SettingsStore {
        val state = MutableStateFlow(AppSettings())
        override val settings: Flow<AppSettings> = state
        override suspend fun saveConnection(serverUrl: String, token: String) {
            state.value = state.value.copy(serverUrl = serverUrl, token = token)
        }
        override suspend fun savePinHash(pinHash: String) {
            state.value = state.value.copy(pinHash = pinHash)
        }
        override suspend fun saveTextScale(scale: TextScale) {
            state.value = state.value.copy(textScale = scale)
        }
    }

    private val store = FakeStore()

    @Test
    fun `pin hasher is deterministic and verifies`() {
        val h = PinHasher.hash("1234")
        assertEquals(h, PinHasher.hash("1234"))
        assertNotEquals(h, PinHasher.hash("1235"))
        assertTrue(PinHasher.verify("1234", h))
        assertFalse(PinHasher.verify("0000", h))
        assertFalse(h.contains("1234"))
    }

    @Test
    fun `validation accepts four digit pins only`() {
        assertTrue(SettingsValidation.isValidPin("0420"))
        assertFalse(SettingsValidation.isValidPin("123"))
        assertFalse(SettingsValidation.isValidPin("12345"))
        assertFalse(SettingsValidation.isValidPin("12a4"))
    }

    @Test
    fun `server url is normalized and validated`() {
        assertEquals("http://192.168.1.5:8000", SettingsValidation.normalizeUrl(" http://192.168.1.5:8000/ "))
        assertEquals("https://mi.casa/musica", SettingsValidation.normalizeUrl("https://mi.casa/musica//"))
        assertNull(SettingsValidation.normalizeUrl("mi.casa"))
        assertNull(SettingsValidation.normalizeUrl(""))
    }

    @Test
    fun `setup with valid data saves everything`() = runTest {
        val vm = SettingsViewModel(store, sync = {})
        vm.completeSetup("http://10.0.0.2:8000/", " tok ", "4321")
        advanceUntilIdle()

        val saved = store.state.value
        assertEquals("http://10.0.0.2:8000", saved.serverUrl)
        assertEquals("tok", saved.token)
        assertTrue(PinHasher.verify("4321", saved.pinHash!!))
        assertTrue(saved.isConfigured)
        assertNull(vm.uiState.value.setupError)
    }

    @Test
    fun `setup rejects bad url token or pin without saving`() = runTest {
        val vm = SettingsViewModel(store, sync = {})

        vm.completeSetup("nope", "tok", "1234")
        advanceUntilIdle()
        assertEquals(SetupError.INVALID_URL, vm.uiState.value.setupError)

        vm.completeSetup("http://a.b", "  ", "1234")
        advanceUntilIdle()
        assertEquals(SetupError.EMPTY_TOKEN, vm.uiState.value.setupError)

        vm.completeSetup("http://a.b", "tok", "12")
        advanceUntilIdle()
        assertEquals(SetupError.INVALID_PIN, vm.uiState.value.setupError)

        assertFalse(store.state.value.isConfigured)
    }

    @Test
    fun `unlock only succeeds with the right pin`() = runTest {
        store.state.value = AppSettings("http://a.b", "t", PinHasher.hash("2468"))
        val vm = SettingsViewModel(store, sync = {})
        advanceUntilIdle()

        vm.unlock("0000")
        assertFalse(vm.uiState.value.unlocked)
        assertTrue(vm.uiState.value.pinError)

        vm.unlock("2468")
        assertTrue(vm.uiState.value.unlocked)
        assertFalse(vm.uiState.value.pinError)
    }

    @Test
    fun `text scale is persisted`() = runTest {
        val vm = SettingsViewModel(store, sync = {})
        vm.setTextScale(TextScale.EXTRA_LARGE)
        advanceUntilIdle()
        assertEquals(TextScale.EXTRA_LARGE, store.state.value.textScale)
    }

    @Test
    fun `editing the pin clears the wrong pin error`() = runTest {
        store.state.value = AppSettings("http://a.b", "t", PinHasher.hash("2468"))
        val vm = SettingsViewModel(store, sync = {})
        advanceUntilIdle()
        vm.unlock("0000")
        assertTrue(vm.uiState.value.pinError)

        vm.onPinEdited()

        assertFalse(vm.uiState.value.pinError)
    }

    @Test
    fun `locking forgets a previous unlock and error`() = runTest {
        store.state.value = AppSettings("http://a.b", "t", PinHasher.hash("2468"))
        val vm = SettingsViewModel(store, sync = {})
        advanceUntilIdle()
        vm.unlock("2468")
        assertTrue(vm.uiState.value.unlocked)

        vm.lock()

        assertFalse(vm.uiState.value.unlocked)
        assertFalse(vm.uiState.value.pinError)
    }

    @Test
    fun `text size can change while the caregiver area stays locked`() = runTest {
        val vm = SettingsViewModel(store, sync = {})
        advanceUntilIdle()

        vm.setTextScale(TextScale.LARGE)
        advanceUntilIdle()

        assertEquals(TextScale.LARGE, vm.uiState.value.settings.textScale)
        assertFalse(vm.uiState.value.unlocked)
    }

    @Test
    fun `a second sync tap while syncing is ignored`() = runTest {
        var calls = 0
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        val vm = SettingsViewModel(store, sync = {
            calls++
            gate.await()
        })
        advanceUntilIdle()

        vm.syncNow()
        vm.syncNow()
        advanceUntilIdle()
        assertEquals(1, calls)
        assertTrue(vm.uiState.value.syncing)

        gate.complete(Unit)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.syncing)
        assertTrue(vm.uiState.value.syncDone)
    }
}
