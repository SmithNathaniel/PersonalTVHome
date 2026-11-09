package com.nathaniel.tvhome.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SettingsApplications
import androidx.compose.material.icons.rounded.SpaceDashboard
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.nathaniel.tvhome.BuildConfig
import com.nathaniel.tvhome.apps.FavouriteApps
import com.nathaniel.tvhome.media.MediaState
import com.nathaniel.tvhome.prefs.WeatherLocation
import com.nathaniel.tvhome.system.SystemSettings
import com.nathaniel.tvhome.ui.LocalActions
import com.nathaniel.tvhome.ui.LocalContainer
import com.nathaniel.tvhome.ui.components.GlassRow
import com.nathaniel.tvhome.ui.components.GlassSurface
import com.nathaniel.tvhome.ui.components.OptionRow
import com.nathaniel.tvhome.ui.components.SectionTitle
import com.nathaniel.tvhome.ui.home.OWNER_NAME
import com.nathaniel.tvhome.ui.navigation.LocalNavigator
import com.nathaniel.tvhome.ui.navigation.Screen
import com.nathaniel.tvhome.ui.navigation.SettingsPage
import com.nathaniel.tvhome.ui.theme.Scenic
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SettingsPanel(modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.current
    val appOptionsOpen = navigator.appOptions != null
    GlassSurface(
        RoundedCornerShape(30.dp),
        tintBoost = 0.22f,
        elevation = 30.dp,
        modifier = modifier
            .width(410.dp)
            .fillMaxHeight()
            .padding(vertical = 18.dp)
            // Keep D-pad focus inside the panel while it is open.
            .focusProperties { exit = { if (appOptionsOpen) FocusRequester.Default else FocusRequester.Cancel } }
            .focusGroup(),
    ) {
        AnimatedContent(
            targetState = navigator.settingsPage,
            transitionSpec = {
                val forward = targetState != SettingsPage.Main
                (fadeIn(tween(260)) + slideInHorizontally(tween(320)) { if (forward) it / 6 else -it / 6 }) togetherWith
                    (fadeOut(tween(180)) + slideOutHorizontally(tween(260)) { if (forward) -it / 6 else it / 6 })
            },
            label = "settingsPage",
        ) { page ->
            Box(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 22.dp)) {
                when (page) {
                    SettingsPage.Main -> MainPage()
                    SettingsPage.Dock -> DockPage()
                    SettingsPage.Weather -> WeatherPage()
                    SettingsPage.Clock -> ClockPage()
                    SettingsPage.Applications -> ApplicationsPage()
                    SettingsPage.About -> AboutPage()
                }
            }
        }
    }
}

@Composable
private fun PageList(title: String, subtitle: String? = null, content: LazyListScope.(first: Modifier) -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { awaitFrame(); runCatching { first.requestFocus() } }
    Column {
        Text(title, style = Scenic.ScreenTitle.copy(fontSize = 28.sp))
        if (subtitle != null) Text(subtitle, style = Scenic.Caption)
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            content(Modifier.focusRequester(first))
        }
    }
}

@Composable
private fun MainPage() {
    val container = LocalContainer.current
    val navigator = LocalNavigator.current
    val actions = LocalActions.current
    val media by container.media.state.collectAsStateWithLifecycle()
    val prefs by container.preferences.preferences.collectAsStateWithLifecycle()
    val chevron = Icons.AutoMirrored.Rounded.KeyboardArrowRight
    PageList("Settings", "Personal TV Home") { first ->
        item { SectionTitle("Personalise", Modifier.padding(top = 4.dp)) }
        item { GlassRow("Wallpapers", { navigator.navigate(Screen.Wallpapers) }, first, Icons.Rounded.Wallpaper, trailingIcon = chevron) }
        item { GlassRow("Appearance", { navigator.navigate(Screen.Appearance) }, icon = Icons.Rounded.Palette, trailingIcon = chevron) }
        item { GlassRow("Dock", { navigator.settingsPage = SettingsPage.Dock }, icon = Icons.Rounded.SpaceDashboard, value = "${prefs.dockApps.orEmpty().size} apps", trailingIcon = chevron) }
        item { GlassRow("Applications", { navigator.settingsPage = SettingsPage.Applications }, icon = Icons.Rounded.Apps, trailingIcon = chevron) }
        item { GlassRow("Weather location", { navigator.settingsPage = SettingsPage.Weather }, icon = Icons.Rounded.LocationOn, value = prefs.weatherLocation.name, trailingIcon = chevron) }
        item { GlassRow("Clock", { navigator.settingsPage = SettingsPage.Clock }, icon = Icons.Rounded.AccessTime, value = if (prefs.use24Hour) "24-hour" else "12-hour", trailingIcon = chevron) }
        item {
            GlassRow(
                "Now Playing access",
                actions::enableNowPlaying,
                icon = Icons.Rounded.GraphicEq,
                value = if (media is MediaState.PermissionRequired) "Not allowed" else "Allowed",
            )
        }
        item { SectionTitle("System", Modifier.padding(top = 14.dp)) }
        item { GlassRow("Display & picture", { actions.openSystem(SystemSettings.Target.Display) }, icon = Icons.Rounded.Tv) }
        item { GlassRow("Sound", { actions.openSystem(SystemSettings.Target.Sound) }, icon = Icons.AutoMirrored.Rounded.VolumeUp) }
        item { GlassRow("Network & internet", { actions.openSystem(SystemSettings.Target.Network) }, icon = Icons.Rounded.Wifi) }
        item { GlassRow("Google Cast / Chromecast", { actions.openSystem(SystemSettings.Target.Cast) }, icon = Icons.Rounded.Cast) }
        item { GlassRow("Default home app", { actions.openSystem(SystemSettings.Target.HomeApp) }, icon = Icons.Rounded.Home) }
        item { GlassRow("All device settings", { actions.openSystem(SystemSettings.Target.Device) }, icon = Icons.Rounded.SettingsApplications) }
        item { SectionTitle("About", Modifier.padding(top = 14.dp)) }
        item { GlassRow("About Personal TV Home", { navigator.settingsPage = SettingsPage.About }, icon = Icons.Rounded.Info, trailingIcon = chevron) }
    }
}

@Composable
private fun DockPage() {
    val container = LocalContainer.current
    val navigator = LocalNavigator.current
    val actions = LocalActions.current
    val prefs by container.preferences.preferences.collectAsStateWithLifecycle()
    val apps by container.apps.apps.collectAsStateWithLifecycle()
    val dock = prefs.dockApps.orEmpty()
    PageList("Dock", "OK for options  ·  ◀ ▶ to reorder") { first ->
        items(dock, key = { "dock-$it" }) { pkg ->
            val name = apps.firstOrNull { it.packageName == pkg }?.label ?: FavouriteApps.nameFor(pkg) ?: pkg
            GlassRow(
                title = name,
                onClick = { navigator.appOptions = pkg },
                modifier = if (pkg == dock.first()) first else Modifier,
                value = "${dock.indexOf(pkg) + 1}",
                onHorizontal = { delta -> actions.moveInDock(pkg, delta) },
            )
        }
        item { SectionTitle("Add to dock", Modifier.padding(top = 14.dp)) }
        items(apps.filter { it.packageName !in dock }, key = { "add-${it.packageName}" }) { app ->
            GlassRow(app.label, { actions.addToDock(app.packageName) }, icon = Icons.Rounded.Add)
        }
    }
}

@Composable
private fun ApplicationsPage() {
    val container = LocalContainer.current
    val navigator = LocalNavigator.current
    val actions = LocalActions.current
    val apps by container.apps.apps.collectAsStateWithLifecycle()
    PageList("Applications", "${apps.size} installed  ·  OK for options") { first ->
        item { GlassRow("Browse all apps", { navigator.navigate(Screen.Apps) }, first, Icons.Rounded.Apps, trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight) }
        item { GlassRow("System app settings", { actions.openSystem(SystemSettings.Target.Apps) }, icon = Icons.Rounded.SettingsApplications) }
        item { SectionTitle("Installed", Modifier.padding(top = 14.dp)) }
        items(apps, key = { it.packageName }) { app ->
            GlassRow(app.label, { navigator.appOptions = app.packageName }, trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight)
        }
    }
}

@Composable
private fun ClockPage() {
    val container = LocalContainer.current
    val actions = LocalActions.current
    val prefs by container.preferences.preferences.collectAsStateWithLifecycle()
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { awaitFrame(); runCatching { first.requestFocus() } }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Clock", style = Scenic.ScreenTitle.copy(fontSize = 28.sp))
        Text("Time and date follow the TV’s time zone", style = Scenic.Caption)
        Spacer(Modifier.height(10.dp))
        Box(Modifier.focusRequester(first)) {
            OptionRow("Time format", listOf(true, false), prefs.use24Hour, { if (it) "24-hour" else "12-hour" }, { v -> container.scope.launch { container.preferences.setUse24Hour(v) } }, Icons.Rounded.AccessTime)
        }
        OptionRow("Show seconds", listOf(false, true), prefs.showSeconds, { if (it) "On" else "Off" }, { v -> container.scope.launch { container.preferences.setShowSeconds(v) } }, Icons.Rounded.Timer)
        GlassRow("Date & time (system)", { actions.openSystem(SystemSettings.Target.DateTime) }, icon = Icons.Rounded.DateRange)
    }
}

@Composable
private fun WeatherPage() {
    val container = LocalContainer.current
    val navigator = LocalNavigator.current
    val prefs by container.preferences.preferences.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<WeatherLocation>>(emptyList()) }
    var status by remember { mutableStateOf<String?>(null) }

    fun search() {
        val q = query.trim()
        if (q.length < 2) return
        status = "Searching…"
        scope.launch {
            runCatching { container.weather.searchLocations(q) }
                .onSuccess { results = it; status = if (it.isEmpty()) "No places found" else null }
                .onFailure { status = "Couldn't search — check the internet connection" }
        }
    }

    fun choose(location: WeatherLocation) {
        container.scope.launch { container.preferences.setWeatherLocation(location) }
        navigator.toast("Weather now shows ${location.displayName}")
        navigator.settingsPage = SettingsPage.Main
    }

    PageList("Weather location", "Currently ${prefs.weatherLocation.displayName}") { first ->
        item { CityField(query, { query = it }, ::search, first) }
        item { GlassRow("Search", ::search, icon = Icons.Rounded.Search) }
        status?.let { s -> item { Text(s, style = Scenic.Caption, modifier = Modifier.padding(8.dp)) } }
        items(results, key = { "${it.latitude},${it.longitude}" }) { loc ->
            GlassRow(loc.name, { choose(loc) }, icon = Icons.Rounded.LocationOn, value = loc.region.take(28))
        }
        item { SectionTitle("Default", Modifier.padding(top = 14.dp)) }
        item { GlassRow(WeatherLocation.Johannesburg.displayName, { choose(WeatherLocation.Johannesburg) }, icon = Icons.Rounded.Home) }
    }
}

@Composable
private fun CityField(value: String, onValueChange: (String) -> Unit, onSearch: () -> Unit, modifier: Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .fillMaxWidth()
            .border(2.dp, if (focused) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.18f), shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        if (value.isEmpty()) Text("Type a city, e.g. Cape Town", style = Scenic.Body.copy(color = Scenic.TextTertiary))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = Scenic.Body,
            cursorBrush = SolidColor(Scenic.TextPrimary),
            interactionSource = interaction,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AboutPage() {
    val actions = LocalActions.current
    PageList("About", "Personal TV Home ${BuildConfig.VERSION_NAME}") { first ->
        item {
            Column(Modifier.padding(bottom = 10.dp)) {
                Text("Personalised for $OWNER_NAME", style = Scenic.Title)
                Spacer(Modifier.height(6.dp))
                Text("Scenic Glass — a calm, cinematic home for Android TV.", style = Scenic.BodySecondary)
            }
        }
        item { AboutLine("Device", "${Build.MANUFACTURER} ${Build.MODEL}") }
        item { AboutLine("Android", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})") }
        item { AboutLine("Weather", "Open-Meteo.com") }
        item { AboutLine("Photography", "Unsplash contributors") }
        item { Spacer(Modifier.height(10.dp)) }
        item { GlassRow("Make Personal TV Home the default", { actions.openSystem(SystemSettings.Target.HomeApp) }, first, Icons.Rounded.Home) }
    }
}

@Composable
private fun AboutLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp)) {
        Text(label, style = Scenic.BodySecondary, modifier = Modifier.width(120.dp))
        Text(value, style = Scenic.Body)
    }
}
