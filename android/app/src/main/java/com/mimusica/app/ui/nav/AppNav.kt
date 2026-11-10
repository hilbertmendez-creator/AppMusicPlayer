package com.mimusica.app.ui.nav

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import com.mimusica.app.ui.components.MiniPlayerBar
import com.mimusica.app.ui.components.TabBar
import com.mimusica.app.ui.components.toMiniPlayerUi
import com.mimusica.app.ui.theme.Card
import com.mimusica.app.ui.theme.Page
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
    const val AJUSTES = "ajustes"
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

    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    val player = container.player
    val playback by player.state.collectAsStateWithLifecycle()
    val mini = playback.toMiniPlayerUi()
    // While the keyboard is up the bar and the mini player step aside: they would float above it and eat the room
    // the screen needs for the field and the results.
    val keyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val showBar = showsTabBar(route) && !keyboardOpen
    val showMini = showBar && mini != null && miniPlayerVisible(route, hasSong = true)
    val goToTab: (MainTab) -> Unit = { tab ->
        nav.navigate(tab.route) {
            popUpTo(Routes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // The bottom region (mini player + tab bar) owns the bottom system inset while it is visible; the screens above
    // it are told that inset is already consumed. Without it, each screen keeps its own safe-drawing padding.
    Column(modifier = Modifier.fillMaxSize().background(Page)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(
                    if (showBar) Modifier.consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                    else Modifier,
                ),
        ) {
            AppNavHost(nav, start, settingsVm, goToTab)
        }
        if (showBar) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Card)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
            ) {
                if (showMini && mini != null) {
                    MiniPlayerBar(
                        state = mini,
                        onOpen = { nav.navigate(Routes.NOW_PLAYING) },
                        onToggle = player::togglePlayPause,
                        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp),
                    )
                }
                TabBar(selected = tabFor(route), onSelect = goToTab)
            }
        }
    }
}

@Composable
private fun AppNavHost(
    nav: NavHostController,
    start: String,
    settingsVm: SettingsViewModel,
    goToTab: (MainTab) -> Unit,
) {
    NavHost(navController = nav, startDestination = start) {
        composable(Routes.SETUP) {
            SetupRoute(settingsVm) {
                nav.navigate(Routes.HOME) { popUpTo(Routes.SETUP) { inclusive = true } }
            }
        }
        composable(Routes.HOME) {
            HomeRoute(
                onContinue = { nav.navigate(Routes.karaoke(it)) },
                onSearch = { goToTab(MainTab.SEARCH) },
                onSongs = { nav.navigate(Routes.SONGS) },
                onPlaylists = { nav.navigate(Routes.PLAYLISTS) },
            )
        }
        composable(Routes.SEARCH) {
            // A tab root: reached by the bar, so there is no "Volver".
            SearchRoute(
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
                onSearch = { goToTab(MainTab.SEARCH) },
            ) {
                nav.navigate(Routes.NOW_PLAYING)
            }
        }
        composable(Routes.SING) {
            SingRoute(
                songsVm(),
                onSing = { nav.navigate(Routes.karaoke(it)) },
                onSearch = { goToTab(MainTab.SEARCH) },
            )
        }
        composable(Routes.PLAYLISTS) {
            PlaylistsRoute(playlistsVm(), onBack = nav::popBackStack, onOpen = { nav.navigate(Routes.playlist(it)) })
        }
        playlistDestinations(nav, goToTab)
        composable(Routes.NOW_PLAYING) {
            NowPlayingRoute(onBack = nav::popBackStack, onSing = { nav.navigate(Routes.karaoke(it)) })
        }
        composable(
            Routes.KARAOKE,
            arguments = listOf(navArgument(Routes.SONG_ID) { type = NavType.StringType }),
        ) { entry ->
            KaraokeRoute(entry.arguments?.getString(Routes.SONG_ID).orEmpty(), onBack = nav::popBackStack)
        }
        composable(Routes.AJUSTES) {
            AjustesRoute(
                settingsVm,
                onCaregiver = {
                    settingsVm.lock()
                    nav.navigate(Routes.PIN)
                },
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
    goToTab: (MainTab) -> Unit,
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
            onSearch = { goToTab(MainTab.SEARCH) },
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
