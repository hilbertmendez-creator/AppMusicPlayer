package com.mimusica.app.ui.nav

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mimusica.app.LocalAppContainer
import com.mimusica.app.ui.components.PreparingScreen
import com.mimusica.app.ui.playlists.PlaylistsViewModel
import com.mimusica.app.ui.settings.SettingsViewModel
import com.mimusica.app.ui.songs.SongsViewModel

object Routes {
    const val SETUP = "setup"
    const val HOME = "home"
    const val SONGS = "songs"
    const val PLAYLISTS = "playlists"
    const val SEARCH = "search"
    const val SING = "sing"
    const val NOW_PLAYING = "now_playing"
    const val PIN = "pin"
    const val SETTINGS = "settings"
    const val TEXT_SIZE = "text_size"
    const val PLAYLIST_ID = "playlistId"
    const val SONG_ID = "songId"
    const val PLAYLIST = "playlist/{$PLAYLIST_ID}"
    const val PLAYLIST_ADD = "playlist/{$PLAYLIST_ID}/add"
    const val KARAOKE = "karaoke/{$SONG_ID}"
    const val ADD_TO_LIST = "add_to_list/{$SONG_ID}"

    fun playlist(id: String) = "playlist/$id"
    fun playlistAdd(id: String) = "playlist/$id/add"
    fun karaoke(songId: String) = "karaoke/$songId"
    fun addToList(songId: String) = "add_to_list/$songId"
}

@Composable
fun AppNav() {
    val container = LocalAppContainer.current
    val nav = rememberNavController()

    val settingsVm: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SettingsViewModel(container.settingsStore, sync = { container.library.sync() }) }
        },
    )
    val settingsState by settingsVm.uiState.collectAsStateWithLifecycle()

    // First frame is a light, warm screen. The library (Room, Retrofit) is only touched once settings are read,
    // and AppContainer.start() already warms it on a background thread.
    if (!settingsState.loaded) {
        PreparingScreen()
        return
    }
    val start = if (settingsState.settings.isConfigured) Routes.HOME else Routes.SETUP

    NavHost(navController = nav, startDestination = start) {
        composable(Routes.SETUP) {
            SetupRoute(settingsVm) {
                nav.navigate(Routes.HOME) { popUpTo(Routes.SETUP) { inclusive = true } }
            }
        }
        composable(Routes.HOME) {
            HomeRoute(
                onSongs = { nav.navigate(Routes.SONGS) },
                onPlaylists = { nav.navigate(Routes.PLAYLISTS) },
                onSearch = { nav.navigate(Routes.SEARCH) },
                onSing = { nav.navigate(Routes.SING) },
                onSettings = { nav.navigate(Routes.TEXT_SIZE) },
                onOpenPlayer = { nav.navigate(Routes.NOW_PLAYING) },
            )
        }
        composable(Routes.SEARCH) {
            SearchRoute(
                onBack = nav::popBackStack,
                onPlayerOpen = { nav.navigate(Routes.NOW_PLAYING) },
                onAddToList = { nav.navigate(Routes.addToList(it)) },
            )
        }
        composable(
            Routes.ADD_TO_LIST,
            arguments = listOf(navArgument(Routes.SONG_ID) { type = NavType.StringType }),
        ) { entry ->
            AddToListRoute(entry.arguments?.getString(Routes.SONG_ID).orEmpty(), playlistsVm(), onBack = nav::popBackStack)
        }
        composable(Routes.SONGS) {
            SongsRoute(
                songsVm(),
                onBack = nav::popBackStack,
                onSing = { nav.navigate(Routes.karaoke(it)) },
                onSearch = { nav.navigate(Routes.SEARCH) },
            ) {
                nav.navigate(Routes.NOW_PLAYING)
            }
        }
        composable(Routes.SING) {
            SingRoute(songsVm(), onBack = nav::popBackStack, onSing = { nav.navigate(Routes.karaoke(it)) })
        }
        composable(Routes.PLAYLISTS) {
            PlaylistsRoute(playlistsVm(), onBack = nav::popBackStack, onOpen = { nav.navigate(Routes.playlist(it)) })
        }
        playlistDestinations(nav)
        composable(Routes.NOW_PLAYING) {
            NowPlayingRoute(onBack = nav::popBackStack, onSing = { nav.navigate(Routes.karaoke(it)) })
        }
        composable(
            Routes.KARAOKE,
            arguments = listOf(navArgument(Routes.SONG_ID) { type = NavType.StringType }),
        ) { entry ->
            KaraokeRoute(entry.arguments?.getString(Routes.SONG_ID).orEmpty(), onBack = nav::popBackStack)
        }
        composable(Routes.TEXT_SIZE) {
            TextSizeRoute(
                settingsVm,
                onCaregiver = {
                    settingsVm.lock()
                    nav.navigate(Routes.PIN)
                },
                onBack = nav::popBackStack,
            )
        }
        composable(Routes.PIN) {
            PinRoute(settingsVm, onBack = nav::popBackStack) {
                nav.navigate(Routes.SETTINGS) { popUpTo(Routes.PIN) { inclusive = true } }
            }
        }
        composable(Routes.SETTINGS) {
            SettingsRoute(settingsVm) {
                settingsVm.lock()
                nav.popBackStack()
            }
        }
    }
}

private fun androidx.navigation.NavGraphBuilder.playlistDestinations(
    nav: NavHostController,
) {
    val idArg = listOf(navArgument(Routes.PLAYLIST_ID) { type = NavType.StringType })
    composable(Routes.PLAYLIST, arguments = idArg) { entry ->
        val id = entry.arguments?.getString(Routes.PLAYLIST_ID).orEmpty()
        PlaylistDetailRoute(
            playlistId = id,
            vm = playlistsVm(),
            onBack = nav::popBackStack,
            onAdd = { nav.navigate(Routes.playlistAdd(id)) },
            onPlayerOpen = { nav.navigate(Routes.NOW_PLAYING) },
        )
    }
    composable(Routes.PLAYLIST_ADD, arguments = idArg) { entry ->
        AddSongsRoute(
            playlistId = entry.arguments?.getString(Routes.PLAYLIST_ID).orEmpty(),
            vm = playlistsVm(),
            onSearch = { nav.navigate(Routes.SEARCH) },
            onDone = nav::popBackStack,
        )
    }
}

/*
 * Songs and playlists ViewModels are created the first time a screen needs them (still shared, scoped to the
 * activity), so Home never waits for Room / Retrofit to be built during its first frame.
 */
@Composable
private fun songsVm(): SongsViewModel {
    val container = LocalAppContainer.current
    return viewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity,
        factory = viewModelFactory { initializer { SongsViewModel(container.library) } },
    )
}

@Composable
private fun playlistsVm(): PlaylistsViewModel {
    val container = LocalAppContainer.current
    return viewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity,
        factory = viewModelFactory { initializer { PlaylistsViewModel(container.library) } },
    )
}
