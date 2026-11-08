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
    fun `karaoke strings that used to be hardcoded live in strings xml`() {
        assertEquals("%1\$s · %2\$s", strings["karaoke_title_artist"])
        assertEquals("♪", strings["karaoke_rest_mark"])
    }
}
