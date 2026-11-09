package com.mimusica.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mimusica.app.LocalAppContainer
import com.mimusica.app.ui.home.HomeScreen
import com.mimusica.app.ui.home.HomeSummary
import com.mimusica.app.ui.home.MiniPlayerUi
import com.mimusica.app.ui.home.homeSummary
import com.mimusica.app.ui.karaoke.KaraokeScreen
import com.mimusica.app.ui.karaoke.KaraokeViewModel
import com.mimusica.app.ui.player.NowPlayingScreen
import com.mimusica.app.ui.playlists.AddSongsScreen
import com.mimusica.app.ui.playlists.AddToListScreen
import com.mimusica.app.ui.playlists.NewPlaylistDialog
import com.mimusica.app.ui.playlists.PlaylistDetailScreen
import com.mimusica.app.ui.playlists.PlaylistsScreen
import com.mimusica.app.ui.playlists.PlaylistsViewModel
import com.mimusica.app.ui.search.SearchScreen
import com.mimusica.app.ui.search.SearchViewModel
import com.mimusica.app.ui.settings.PinScreen
import com.mimusica.app.ui.settings.SettingsScreen
import com.mimusica.app.ui.settings.SettingsViewModel
import com.mimusica.app.ui.settings.SetupScreen
import com.mimusica.app.ui.settings.TextSizeScreen
import com.mimusica.app.ui.sing.SingScreen
import com.mimusica.app.ui.components.rememberSpeechInput
import com.mimusica.app.ui.songs.SongsScreen
import com.mimusica.app.ui.songs.filterSongs
import com.mimusica.app.ui.songs.SongsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

@Composable
fun SetupRoute(vm: SettingsViewModel, onDone: () -> Unit) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.settings.isConfigured) {
        if (state.settings.isConfigured) onDone()
    }
    SetupScreen(error = state.setupError, onSave = vm::completeSetup)
}

@Composable
fun HomeRoute(
    onSongs: () -> Unit,
    onPlaylists: () -> Unit,
    onSearch: () -> Unit,
    onSing: () -> Unit,
    onSettings: () -> Unit,
    onOpenPlayer: () -> Unit,
) {
    val container = LocalAppContainer.current
    val player = container.player
    val playback by player.state.collectAsStateWithLifecycle()
    val mini = playback.currentSong?.let { MiniPlayerUi(it.title, it.artist, playback.isPlaying, it.thumbnailUrl, playback.positionMs, playback.durationMs) }
    // The library is resolved off the main thread (it may still be warming up), so Home's first frame never
    // waits for Room. Until the first counts arrive the tiles simply show no subtitle.
    val summaryFlow = remember(container) {
        flow<HomeSummary?> {
            val library = container.library
            emitAll(combine(library.observeDownloadedSongs(), library.observePlaylists(), ::homeSummary))
        }.flowOn(Dispatchers.Default)
    }
    val summary by summaryFlow.collectAsStateWithLifecycle(initialValue = null)
    val updateState by container.updates.state.collectAsStateWithLifecycle()
    HomeScreen(
        miniPlayer = mini,
        summary = summary,
        updateState = updateState,
        onUpdate = container.updates::startUpdate,
        onSongs = onSongs,
        onPlaylists = onPlaylists,
        onSearch = onSearch,
        onSing = onSing,
        onSettings = onSettings,
        onMiniPlayer = onOpenPlayer,
        onMiniToggle = player::togglePlayPause,
    )
}

@Composable
fun SearchRoute(onBack: () -> Unit, onPlayerOpen: () -> Unit, onAddToList: (songId: String) -> Unit) {
    val container = LocalAppContainer.current
    val vm: SearchViewModel = viewModel(
        factory = viewModelFactory { initializer { SearchViewModel(container.remote, container.library) } },
    )
    val state by vm.uiState.collectAsStateWithLifecycle()
    var micUnavailable by remember { mutableStateOf(false) }
    val startSpeech = rememberSpeechInput(
        onResult = {
            micUnavailable = false
            vm.onVoiceResult(it)
        },
        onUnavailable = { micUnavailable = true },
    )
    SearchScreen(
        state = state,
        micMessage = micUnavailable,
        onQueryChange = vm::onQueryChange,
        onSearch = vm::search,
        onMic = startSpeech,
        onDownload = vm::download,
        onPlayNow = { song ->
            container.player.play(listOf(song), 0)
            onPlayerOpen()
        },
        onAddToList = { onAddToList(it.id) },
        onBack = onBack,
    )
}

/** "Agregar a una lista" for one song: pick a list (or make a new one that starts with the song). */
@Composable
fun AddToListRoute(songId: String, vm: PlaylistsViewModel, onBack: () -> Unit) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var micUnavailable by remember { mutableStateOf(false) }
    // A stale confirmation from an earlier add must not greet the user on arrival.
    LaunchedEffect(songId) { vm.clearLastAdded() }
    val startSpeech = rememberSpeechInput(
        onResult = {
            micUnavailable = false
            name = it
        },
        onUnavailable = { micUnavailable = true },
    )
    AddToListScreen(
        playlists = state.playlists,
        addedTo = state.lastAddedTo,
        song = state.songs.firstOrNull { it.id == songId },
        onChoose = { vm.addSongs(it.id, listOf(songId)) },
        onNew = {
            name = ""
            showDialog = true
        },
        onBack = onBack,
    )
    if (showDialog) {
        NewPlaylistDialog(
            name = name,
            micUnavailable = micUnavailable,
            onNameChange = { name = it },
            onMic = startSpeech,
            onCreate = {
                vm.createPlaylist(name, listOf(songId))
                showDialog = false
            },
            onCancel = { showDialog = false },
        )
    }
}

@Composable
fun SongsRoute(
    vm: SongsViewModel,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onPlayerOpen: () -> Unit,
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val player = LocalAppContainer.current.player
    // The ViewModel is shared by the whole activity: forget "Se borró «…»." when leaving this screen.
    DisposableEffect(vm) { onDispose { vm.clearNotice() } }
    var query by rememberSaveable { mutableStateOf("") }
    var micUnavailable by remember { mutableStateOf(false) }
    val startSpeech = rememberSpeechInput(
        onResult = {
            micUnavailable = false
            query = it
        },
        onUnavailable = { micUnavailable = true },
    )
    SongsScreen(
        songs = state.songs,
        error = state.error,
        deletedTitle = state.deletedTitle,
        query = query,
        onQueryChange = { query = it },
        onMic = startSpeech,
        micMessage = micUnavailable,
        onPlay = { index ->
            // The card index is into what the screen shows, i.e. the filtered list.
            player.play(filterSongs(state.songs, query), index)
            onPlayerOpen()
        },
        onPlayAll = { songs ->
            player.play(songs, 0)
            onPlayerOpen()
        },
        onDelete = vm::delete,
        onSearch = onSearch,
        onBack = onBack,
    )
}

@Composable
fun SingRoute(vm: SongsViewModel, onBack: () -> Unit, onSing: (String) -> Unit, onSearch: () -> Unit) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    SingScreen(songs = state.singable, onSing = { onSing(it.id) }, onSearch = onSearch, onBack = onBack)
}

@Composable
fun PlaylistsRoute(vm: PlaylistsViewModel, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var micUnavailable by remember { mutableStateOf(false) }
    val startSpeech = rememberSpeechInput(
        onResult = {
            micUnavailable = false
            name = it
        },
        onUnavailable = { micUnavailable = true },
    )
    PlaylistsScreen(
        playlists = state.playlists,
        error = state.error,
        onOpen = { onOpen(it.id) },
        onNew = {
            name = ""
            showDialog = true
        },
        onBack = onBack,
    )
    if (showDialog) {
        NewPlaylistDialog(
            name = name,
            micUnavailable = micUnavailable,
            onNameChange = { name = it },
            onMic = startSpeech,
            onCreate = {
                vm.createPlaylist(name)
                showDialog = false
            },
            onCancel = { showDialog = false },
        )
    }
}

@Composable
fun PlaylistDetailRoute(
    playlistId: String,
    vm: PlaylistsViewModel,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onPlayerOpen: () -> Unit,
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val player = LocalAppContainer.current.player
    val playlist = state.playlists.firstOrNull { it.id == playlistId }
    if (playlist == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val byId = state.songs.associateBy { it.id }
    val songs = playlist.songIds.mapNotNull { byId[it] }
    PlaylistDetailScreen(
        playlist = playlist,
        songs = songs,
        error = state.error,
        onPlayAll = {
            player.play(songs, 0)
            onPlayerOpen()
        },
        onPlaySong = { index ->
            player.play(songs, index)
            onPlayerOpen()
        },
        onAddSongs = onAdd,
        onRemoveSong = { vm.removeSong(playlistId, it.id) },
        onDeletePlaylist = { vm.deletePlaylist(playlistId) },
        onBack = onBack,
    )
}

@Composable
fun AddSongsRoute(playlistId: String, vm: PlaylistsViewModel, onSearch: () -> Unit, onDone: () -> Unit) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableStateOf(listOf<String>()) }
    val inList = state.playlists.firstOrNull { it.id == playlistId }?.songIds.orEmpty().toSet()
    AddSongsScreen(
        candidates = state.songs.filter { it.id !in inList },
        selected = selected.toSet(),
        onToggle = { id -> selected = if (id in selected) selected - id else selected + id },
        onConfirm = {
            vm.addSongs(playlistId, selected)
            onDone()
        },
        onSearch = onSearch,
        onBack = onDone,
    )
}

@Composable
fun NowPlayingRoute(onBack: () -> Unit, onSing: (String) -> Unit) {
    val player = LocalAppContainer.current.player
    val state by player.state.collectAsStateWithLifecycle()
    NowPlayingScreen(
        state = state,
        onToggle = player::togglePlayPause,
        onNext = player::next,
        onPrevious = player::previous,
        onVolumeUp = player::volumeUp,
        onVolumeDown = player::volumeDown,
        onRestart = { player.seekTo(0L) },
        onSing = onSing,
        onBack = onBack,
    )
}

@Composable
fun KaraokeRoute(songId: String, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: KaraokeViewModel = viewModel(
        key = "karaoke-$songId",
        factory = viewModelFactory { initializer { KaraokeViewModel(songId, container.library, container.player) } },
    )
    val state by vm.uiState.collectAsStateWithLifecycle()
    val settings by container.settingsStore.settings.collectAsStateWithLifecycle(
        initialValue = com.mimusica.app.data.settings.AppSettings(),
    )
    KaraokeScreen(
        state = state,
        textScale = settings.textScale.factor,
        onToggle = vm::togglePlayPause,
        onRestart = vm::restart,
        onBack = onBack,
    )
}

@Composable
fun PinRoute(vm: SettingsViewModel, onBack: () -> Unit, onUnlocked: () -> Unit) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.unlocked) {
        if (state.unlocked) onUnlocked()
    }
    PinScreen(wrongPin = state.pinError, onSubmit = vm::unlock, onPinEdited = vm::onPinEdited, onBack = onBack)
}

/** "Tamaño de letra": no PIN. The caregiver door at the bottom leads to the PIN gate. */
@Composable
fun TextSizeRoute(vm: SettingsViewModel, onCaregiver: () -> Unit, onBack: () -> Unit) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    TextSizeScreen(
        current = state.settings.textScale,
        onSelect = vm::setTextScale,
        onCaregiver = onCaregiver,
        onBack = onBack,
    )
}

@Composable
fun SettingsRoute(vm: SettingsViewModel, onBack: () -> Unit) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onSaveConnection = vm::saveConnection,
        onChangePin = vm::changePin,
        onSync = vm::syncNow,
        onBack = onBack,
    )
}
