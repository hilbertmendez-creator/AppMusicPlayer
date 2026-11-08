package com.mimusica.app.ui.components

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import com.mimusica.app.R

/**
 * Returns a function that opens the system speech recognizer (Spanish). The recognized text goes to
 * [onResult]; if the device has no recognizer [onUnavailable] is called instead of crashing.
 */
@Composable
fun rememberSpeechInput(onResult: (String) -> Unit, onUnavailable: () -> Unit): () -> Unit {
    val currentOnResult by rememberUpdatedState(onResult)
    val currentOnUnavailable by rememberUpdatedState(onUnavailable)
    val prompt = stringResource(R.string.mic_prompt)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val text = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
            if (!text.isNullOrEmpty()) currentOnResult(text)
        }
    }
    return {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es")
            putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
        }
        try {
            launcher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            currentOnUnavailable()
        }
    }
}
