package com.mimusica.app.ui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the Spanish copy: one name for the caregiver's secret ("PIN") and no dead strings. */
class StringsConsistencyTest {
    private val strings: Map<String, String> by lazy {
        // Gradle runs unit tests with the module directory (app/) as the working directory.
        val xml = listOf("src/main/res/values/strings.xml", "app/src/main/res/values/strings.xml")
            .map(::File).first { it.exists() }.readText()
        Regex("""<string name="([^"]+)">(.*?)</string>""").findAll(xml).associate { it.groupValues[1] to it.groupValues[2] }
    }

    @Test
    fun `the secret is always called PIN`() {
        strings.forEach { (name, text) ->
            assertFalse("$name still says 'número secreto': $text", text.contains("número secreto", ignoreCase = true))
            assertFalse("$name still says 'secreto': $text", text.contains("secreto", ignoreCase = true))
        }
        listOf("pin_prompt", "pin_help", "pin_wrong").forEach {
            assertTrue("$it must name the PIN: ${strings[it]}", strings.getValue(it).contains("PIN"))
        }
        assertEquals("Escribe el PIN", strings["pin_prompt"])
        assertEquals("Cambiar PIN", strings["change_pin"])
    }

    @Test
    fun `the caregiver link carries a note about who it is for`() {
        assertEquals("Solo para quien configuró la app", strings["caregiver_note"])
    }

    @Test
    fun `the unused text size tile label is gone`() {
        assertFalse(strings.containsKey("home_text_size"))
    }

    @Test
    fun `ajustes copy is present and the old text size screen copy is gone`() {
        assertEquals("Ajustes", strings["ajustes_title"])
        assertEquals("Fuente de la app", strings["font_section"])
        assertEquals("Cambiar fuente", strings["font_change"])
        assertEquals("Listo", strings["font_done"])
        assertEquals("Acerca de", strings["about_title"])
        assertEquals("Versión %1\$s", strings["about_version"])
        assertEquals("Buscar actualización", strings["update_check"])
        assertEquals("Ya tienes la última versión", strings["update_up_to_date"])
        assertFalse(strings.containsKey("text_size_intro"))
        assertFalse(strings.containsKey("text_size_preview"))
    }

    @Test
    fun `navigation shell copy is present`() {
        assertEquals("Inicio", strings["tab_home"])
        assertEquals("Buscar", strings["tab_search"])
        assertEquals("Cantar", strings["tab_sing"])
        assertEquals("Ajustes", strings["tab_settings"])
        assertEquals("Secciones", strings["tabs_label"])
        assertEquals("%1\$s, pestaña actual", strings["tab_current"])
        assertEquals("Seguir cantando", strings["continue_singing"])
        assertEquals("Todavía no hay canciones con letra", strings["continue_singing_none"])
        assertEquals("Cantar con la letra", strings["sing_with_lyrics"])
        assertEquals("Seguir", strings["resume"])
        assertEquals("Sonando", strings["mini_player_playing"])
        assertEquals("En pausa", strings["mini_player_paused"])
        assertTrue(strings.containsKey("mini_player_open_titled"))
    }

    @Test
    fun `strings of removed controls are gone`() {
        listOf(
            "home_text_size_desc", "home_search", "home_sing", "home_search_subtitle",
            "home_sing_count", "home_sing_none", "volume", "volume_level", "volume_state", "volume_up",
            "volume_down", "now_playing_restart", "mini_player_now", "mini_player_open",
        ).forEach { assertFalse("$it should be removed", strings.containsKey(it)) }
    }

    @Test
    fun `karaoke strings that used to be hardcoded live in strings xml`() {
        assertEquals("%1\$s · %2\$s", strings["karaoke_title_artist"])
        assertEquals("♪", strings["karaoke_rest_mark"])
    }
}
