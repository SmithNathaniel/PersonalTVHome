package com.nathaniel.tvhome.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.nathaniel.tvhome.AppContainer
import com.nathaniel.tvhome.apps.FavouriteApps
import com.nathaniel.tvhome.system.SystemSettings
import com.nathaniel.tvhome.ui.navigation.LocalNavigator
import com.nathaniel.tvhome.ui.navigation.Navigator
import kotlinx.coroutines.launch

val LocalContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }

/** User actions shared by every screen, with friendly feedback when something isn't possible. */
@Stable
class LauncherActions(private val container: AppContainer, private val navigator: Navigator) {
    private val scope get() = container.scope
    private val prefs get() = container.preferences

    fun launch(packageName: String) {
        if (container.apps.launch(packageName)) return
        if (!container.apps.isInstalled(packageName)) {
            val name = FavouriteApps.nameFor(packageName) ?: packageName
            if (!container.apps.openStoreListing(packageName)) navigator.toast("$name isn't installed on this TV")
        } else {
            navigator.toast("This app can't be opened from a launcher")
        }
    }

    fun openSystem(target: SystemSettings.Target) {
        if (!container.systemSettings.open(target)) navigator.toast("${target.label} isn't available on this device")
    }

    fun openAppInfo(packageName: String) {
        if (!container.systemSettings.openAppInfo(packageName)) navigator.toast("App info isn't available")
    }

    fun enableNowPlaying() {
        if (!container.systemSettings.open(SystemSettings.Target.NotificationAccess)) {
            navigator.toast("Allow notification access for Personal TV Home in system settings")
        }
    }

    fun dock(): List<String> = prefs.preferences.value.dockApps.orEmpty()

    fun addToDock(packageName: String) = scope.launch {
        val current = dock()
        if (packageName in current) return@launch
        if (current.size >= MAX_DOCK) {
            navigator.toast("The dock holds up to $MAX_DOCK apps")
            return@launch
        }
        prefs.setDockApps(current + packageName)
    }

    fun removeFromDock(packageName: String) = scope.launch {
        val current = dock()
        if (current.size <= MIN_DOCK) {
            navigator.toast("Keep at least $MIN_DOCK apps in the dock")
            return@launch
        }
        prefs.setDockApps(current - packageName)
    }

    fun moveInDock(packageName: String, delta: Int) = scope.launch {
        val current = dock().toMutableList()
        val i = current.indexOf(packageName)
        val j = i + delta
        if (i < 0 || j !in current.indices) return@launch
        current.add(j, current.removeAt(i))
        prefs.setDockApps(current)
    }

    companion object {
        const val MAX_DOCK = 7
        const val MIN_DOCK = 1
    }
}

val LocalActions = staticCompositionLocalOf<LauncherActions> { error("LauncherActions not provided") }

@Composable
fun rememberLauncherActions(container: AppContainer): LauncherActions {
    val navigator = LocalNavigator.current
    return remember(container, navigator) { LauncherActions(container, navigator) }
}
