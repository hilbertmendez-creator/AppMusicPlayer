package com.mimusica.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.mimusica.app.data.settings.AppSettings
import com.mimusica.app.ui.nav.AppNav
import com.mimusica.app.ui.theme.MiMusicaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as MiMusicaApp).container
        setContent {
            val settings by container.settingsStore.settings.collectAsState(initial = AppSettings())
            val notificationPermission =
                rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
            LaunchedEffect(Unit) {
                // Needed for the lock-screen / notification playback controls on Android 13+.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            CompositionLocalProvider(LocalAppContainer provides container) {
                MiMusicaTheme(textScale = settings.textScale, fontChoice = settings.fontChoice) {
                    AppNav()
                }
            }
        }
    }
}
