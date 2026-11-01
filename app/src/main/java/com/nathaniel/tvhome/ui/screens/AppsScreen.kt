package com.nathaniel.tvhome.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.nathaniel.tvhome.apps.LauncherApp
import com.nathaniel.tvhome.ui.LocalActions
import com.nathaniel.tvhome.ui.LocalContainer
import com.nathaniel.tvhome.ui.components.AppIcon
import com.nathaniel.tvhome.ui.components.EmptyState
import com.nathaniel.tvhome.ui.components.FocusTile
import com.nathaniel.tvhome.ui.components.ScreenScaffold
import com.nathaniel.tvhome.ui.navigation.LocalNavigator
import com.nathaniel.tvhome.ui.theme.Scenic
import kotlinx.coroutines.android.awaitFrame

@Composable
fun AppsScreen() {
    val container = LocalContainer.current
    val apps by container.apps.apps.collectAsStateWithLifecycle()
    val loaded by container.apps.loaded.collectAsStateWithLifecycle()
    ScreenScaffold(
        title = "Apps",
        subtitle = if (loaded) "${apps.size} installed  ·  hold OK or press Menu for options" else "Finding your apps…",
    ) {
        if (loaded && apps.isEmpty()) {
            EmptyState(Icons.Rounded.Apps, "No apps found", "Installed TV apps will appear here.", Modifier.fillMaxSize())
        } else {
            AppGrid(apps)
        }
    }
}

@Composable
fun AppGrid(apps: List<LauncherApp>, columns: Int = 6, autoFocus: Boolean = true) {
    val actions = LocalActions.current
    val navigator = LocalNavigator.current
    val first = remember { FocusRequester() }
    LaunchedEffect(apps.isNotEmpty()) {
        if (autoFocus && apps.isNotEmpty()) {
            awaitFrame()
            runCatching { first.requestFocus() }
        }
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp, start = 4.dp, end = 4.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
            AppGridItem(
                app = app,
                modifier = if (index == 0) Modifier.focusRequester(first) else Modifier,
                onClick = { actions.launch(app.packageName) },
                onLongClick = { navigator.appOptions = app.packageName },
            )
        }
    }
}

@Composable
fun AppGridItem(app: LauncherApp, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        FocusTile(onClick = onClick, onLongClick = onLongClick, shape = RoundedCornerShape(18.dp), focusedScale = 1.12f) {
            AppIcon(app, app.label, 76.dp)
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.padding(horizontal = 2.dp)) {
            Text(
                app.label,
                style = Scenic.Caption.copy(color = Scenic.TextPrimary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}
