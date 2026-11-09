package com.nathaniel.tvhome.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.nathaniel.tvhome.apps.FavouriteApps
import com.nathaniel.tvhome.ui.LocalActions
import com.nathaniel.tvhome.ui.LocalContainer
import com.nathaniel.tvhome.ui.components.AppIcon
import com.nathaniel.tvhome.ui.components.GlassRow
import com.nathaniel.tvhome.ui.components.GlassSurface
import com.nathaniel.tvhome.ui.navigation.LocalNavigator
import com.nathaniel.tvhome.ui.theme.Scenic
import kotlinx.coroutines.android.awaitFrame

/** Options for a single app: open, dock membership/order and system App info. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppOptionsSheet(packageName: String, modifier: Modifier = Modifier) {
    val container = LocalContainer.current
    val navigator = LocalNavigator.current
    val actions = LocalActions.current
    val prefs by container.preferences.preferences.collectAsStateWithLifecycle()
    val apps by container.apps.apps.collectAsStateWithLifecycle()
    val app = apps.firstOrNull { it.packageName == packageName }
    val name = app?.label ?: FavouriteApps.nameFor(packageName) ?: packageName
    val dock = prefs.dockApps.orEmpty()
    val inDock = packageName in dock
    val first = remember { FocusRequester() }
    var firstRowPlaced by remember(packageName) { mutableStateOf(false) }
    LaunchedEffect(packageName, firstRowPlaced) {
        if (firstRowPlaced) {
            awaitFrame()
            runCatching { first.requestFocus() }
        }
    }
    fun close() { navigator.appOptions = null }

    GlassSurface(
        RoundedCornerShape(28.dp),
        tintBoost = 0.3f,
        elevation = 36.dp,
        modifier = modifier
            .width(400.dp)
            .focusProperties { exit = { FocusRequester.Cancel } }
            .focusGroup(),
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(app, name, 56.dp)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(name, style = Scenic.Title)
                    Text(if (app == null) "Not installed" else packageName, style = Scenic.Caption.copy(color = Scenic.TextTertiary), maxLines = 1)
                }
            }
            Spacer(Modifier.height(10.dp))
            GlassRow(
                if (app == null) "Get app" else "Open",
                { close(); actions.launch(packageName) },
                Modifier
                    .focusRequester(first)
                    .onGloballyPositioned { firstRowPlaced = true },
                Icons.Rounded.PlayArrow,
            )
            if (inDock) {
                GlassRow("Remove from dock", { actions.removeFromDock(packageName); close() }, icon = Icons.Rounded.RemoveCircleOutline)
                if (dock.indexOf(packageName) > 0) GlassRow("Move left in dock", { actions.moveInDock(packageName, -1) }, icon = Icons.AutoMirrored.Rounded.ArrowBack)
                if (dock.indexOf(packageName) < dock.lastIndex) GlassRow("Move right in dock", { actions.moveInDock(packageName, 1) }, icon = Icons.AutoMirrored.Rounded.ArrowForward)
            } else if (app != null) {
                GlassRow("Add to dock", { actions.addToDock(packageName); close() }, icon = Icons.Rounded.AddCircleOutline)
            }
            if (app != null) {
                GlassRow("App info", { close(); actions.openAppInfo(packageName) }, icon = Icons.Rounded.Info)
            }
        }
    }
}
