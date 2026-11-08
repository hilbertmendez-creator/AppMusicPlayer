package com.mimusica.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mimusica.app.ui.components.PreparingScreen
import com.mimusica.app.ui.theme.MiMusicaTheme
import org.junit.Rule
import org.junit.Test

class PreparingScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun showsTheNameAtOnceAndTheReassuranceAfterAWhile() {
        compose.mainClock.autoAdvance = false
        compose.setContent { MiMusicaTheme { PreparingScreen(messageDelayMs = 1_000) } }

        compose.onNodeWithText("Mi Música").assertIsDisplayed()
        compose.onNodeWithText("Preparando tus canciones…").assertDoesNotExist()

        compose.mainClock.advanceTimeBy(1_200)

        compose.onNodeWithText("Preparando tus canciones…").assertIsDisplayed()
    }
}
