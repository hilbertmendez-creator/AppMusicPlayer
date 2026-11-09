package com.mimusica.app.ui

import com.mimusica.app.ui.nav.MainTab
import com.mimusica.app.ui.nav.Routes
import com.mimusica.app.ui.nav.miniPlayerVisible
import com.mimusica.app.ui.nav.showsTabBar
import com.mimusica.app.ui.nav.tabFor
import com.mimusica.app.ui.nav.tabLabelSizeSp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TabsTest {
    @Test
    fun `there are four tabs in order and each opens its root route`() {
        assertEquals(listOf(MainTab.HOME, MainTab.SEARCH, MainTab.SING, MainTab.SETTINGS), MainTab.entries.toList())
        assertEquals(Routes.HOME, MainTab.HOME.route)
        assertEquals(Routes.SEARCH, MainTab.SEARCH.route)
        assertEquals(Routes.SING, MainTab.SING.route)
        assertEquals(Routes.AJUSTES, MainTab.SETTINGS.route)
    }

    @Test
    fun `tab roots belong to their own tab`() {
        assertEquals(MainTab.HOME, tabFor(Routes.HOME))
        assertEquals(MainTab.SEARCH, tabFor(Routes.SEARCH))
        assertEquals(MainTab.SING, tabFor(Routes.SING))
        assertEquals(MainTab.SETTINGS, tabFor(Routes.AJUSTES))
    }

    @Test
    fun `songs and lists screens belong to the home tab`() {
        assertEquals(MainTab.HOME, tabFor(Routes.SONGS))
        assertEquals(MainTab.HOME, tabFor(Routes.PLAYLISTS))
        assertEquals(MainTab.HOME, tabFor(Routes.PLAYLIST))
        assertEquals(MainTab.HOME, tabFor("playlist/abc-123"))
    }

    @Test
    fun `hidden or unknown routes belong to no tab`() {
        listOf(
            null, "", "nope", Routes.SETUP, Routes.PIN, Routes.SETTINGS, Routes.NOW_PLAYING,
            Routes.KARAOKE, "karaoke/42", Routes.PLAYLIST_ADD, "playlist/abc/add", Routes.ADD_TO_LIST, "add_to_list/9",
        ).forEach { assertNull("route $it", tabFor(it)) }
    }

    @Test
    fun `the tab bar shows on the main screens`() {
        listOf(
            Routes.HOME, Routes.SONGS, Routes.PLAYLISTS, Routes.PLAYLIST, "playlist/abc",
            Routes.SEARCH, Routes.SING, Routes.AJUSTES,
        ).forEach { assertTrue("route $it", showsTabBar(it)) }
    }

    @Test
    fun `the tab bar is hidden on setup, pin, caregiver, player, karaoke and add screens`() {
        listOf(
            Routes.SETUP, Routes.PIN, Routes.SETTINGS, Routes.NOW_PLAYING, Routes.KARAOKE, "karaoke/7",
            Routes.PLAYLIST_ADD, "playlist/abc/add", Routes.ADD_TO_LIST, "add_to_list/7", "mystery", null,
        ).forEach { assertFalse("route $it", showsTabBar(it)) }
    }

    @Test
    fun `tab labels stop growing at 20sp after the phone's font scale`() {
        assertEquals(17.6f, tabLabelSizeSp(baseSp = 17.6f, fontScale = 1f), 0.001f)
        assertEquals(20f, tabLabelSizeSp(baseSp = 26.4f, fontScale = 1f), 0.001f)
        assertEquals(10f, tabLabelSizeSp(baseSp = 26.4f, fontScale = 2f), 0.001f)
        assertEquals(20f, tabLabelSizeSp(baseSp = 22f, fontScale = 0.5f).coerceAtMost(20f), 0.001f)
    }

    @Test
    fun `mini player shows where the bar shows, with a song, except in ajustes`() {
        assertTrue(miniPlayerVisible(Routes.HOME, hasSong = true))
        assertTrue(miniPlayerVisible(Routes.SEARCH, hasSong = true))
        assertTrue(miniPlayerVisible("playlist/abc", hasSong = true))
        assertFalse(miniPlayerVisible(Routes.HOME, hasSong = false))
        assertFalse(miniPlayerVisible(Routes.AJUSTES, hasSong = true))
        assertFalse(miniPlayerVisible(Routes.NOW_PLAYING, hasSong = true))
        assertFalse(miniPlayerVisible("karaoke/7", hasSong = true))
        assertFalse(miniPlayerVisible(null, hasSong = true))
    }
}
