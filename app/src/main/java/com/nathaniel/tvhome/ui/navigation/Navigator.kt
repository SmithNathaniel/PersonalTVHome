package com.nathaniel.tvhome.ui.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

sealed interface Screen {
    data object Home : Screen
    data object Apps : Screen
    data object Wallpapers : Screen
    data object Appearance : Screen
    data object NowPlaying : Screen
}

enum class SettingsPage { Main, Dock, Weather, Clock, Applications, About }

/** Simple back stack plus the settings side panel and transient messages. */
@Stable
class Navigator {
    private val stack = mutableStateListOf<Screen>(Screen.Home)
    val current: Screen get() = stack.last()

    var settingsOpen by mutableStateOf(false)
        private set
    var settingsPage by mutableStateOf(SettingsPage.Main)

    var message by mutableStateOf<String?>(null)
        private set
    var messageId by mutableIntStateOf(0)
        private set

    /** Package whose options sheet is showing (long-press / Menu on an app). */
    var appOptions by mutableStateOf<String?>(null)

    /** Incremented whenever Home should reclaim focus (e.g. the Home key was pressed). */
    var homeFocusRequest by mutableIntStateOf(0)
        private set

    /** Index of the dock item that last held focus, restored when returning Home. */
    var homeFocusIndex by mutableIntStateOf(0)

    fun navigate(screen: Screen) {
        settingsOpen = false
        appOptions = null
        if (current == screen) return
        stack.remove(screen)
        stack.add(screen)
    }

    /** Returns false when there is nowhere further back to go (already Home). */
    fun back(): Boolean {
        if (appOptions != null) {
            appOptions = null
            return true
        }
        if (settingsOpen) {
            if (settingsPage != SettingsPage.Main) settingsPage = SettingsPage.Main else settingsOpen = false
            return true
        }
        if (stack.size > 1) {
            stack.removeAt(stack.lastIndex)
            return true
        }
        return false
    }

    fun home() {
        settingsOpen = false
        appOptions = null
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
        homeFocusRequest++
    }

    fun openSettings(page: SettingsPage = SettingsPage.Main) {
        settingsPage = page
        settingsOpen = true
    }

    fun closeSettings() {
        settingsOpen = false
    }

    fun toast(text: String) {
        message = text
        messageId++
    }

    fun clearMessage(id: Int) {
        if (id == messageId) message = null
    }
}

val LocalNavigator = staticCompositionLocalOf<Navigator> { error("Navigator not provided") }
