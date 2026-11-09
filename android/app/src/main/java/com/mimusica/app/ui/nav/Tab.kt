package com.mimusica.app.ui.nav

/** The four sections of the bottom bar, in the order they appear, each with the route it opens. */
enum class MainTab(val route: String) {
    HOME(Routes.HOME),
    SEARCH(Routes.SEARCH),
    SING(Routes.SING),
    SETTINGS(Routes.AJUSTES),
}

/**
 * The route pattern a destination belongs to. The NavHost reports patterns ("playlist/{playlistId}"), but a
 * concrete route ("playlist/abc") is accepted too, so matching never depends on the argument value.
 */
private fun canonicalRoute(route: String?): String? {
    val parts = route?.substringBefore('?')?.split('/').orEmpty()
    return when {
        parts.isEmpty() || parts[0].isEmpty() -> null
        parts.size == 1 -> parts[0]
        parts[0] == "playlist" && parts.size == 2 -> Routes.PLAYLIST
        parts[0] == "playlist" && parts.size == 3 && parts[2] == "add" -> Routes.PLAYLIST_ADD
        parts[0] == "karaoke" && parts.size == 2 -> Routes.KARAOKE
        parts[0] == "add_to_list" && parts.size == 2 -> Routes.ADD_TO_LIST
        else -> null
    }
}

/** Which tab a screen lives under; Songs, Lists and a list's detail are children of Inicio. Null = no tab. */
fun tabFor(route: String?): MainTab? = when (canonicalRoute(route)) {
    Routes.HOME, Routes.SONGS, Routes.PLAYLISTS, Routes.PLAYLIST -> MainTab.HOME
    Routes.SEARCH -> MainTab.SEARCH
    Routes.SING -> MainTab.SING
    Routes.AJUSTES -> MainTab.SETTINGS
    else -> null
}

/** Labels in the bar never exceed this many sp once every text scale is applied, so four always fit. */
const val TabLabelCapSp = 20f

/** Size to give a tab label: [baseSp] (already scaled by the in-app size), held so that x [fontScale] <= the cap. */
fun tabLabelSizeSp(baseSp: Float, fontScale: Float): Float =
    minOf(baseSp, TabLabelCapSp / fontScale.coerceAtLeast(0.01f))

/** The bar shows on the main screens only; setup, PIN, caregiver, player, karaoke and add screens hide it. */
fun showsTabBar(route: String?): Boolean = tabFor(route) != null

/** The mini player sits above the bar while a song is loaded, but never over Ajustes. */
fun miniPlayerVisible(route: String?, hasSong: Boolean): Boolean =
    hasSong && showsTabBar(route) && canonicalRoute(route) != Routes.AJUSTES
