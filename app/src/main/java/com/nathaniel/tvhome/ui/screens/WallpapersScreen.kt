package com.nathaniel.tvhome.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.nathaniel.tvhome.ui.LocalContainer
import com.nathaniel.tvhome.ui.components.FocusTile
import com.nathaniel.tvhome.ui.components.ScreenScaffold
import com.nathaniel.tvhome.ui.navigation.LocalNavigator
import com.nathaniel.tvhome.ui.theme.Scenic
import com.nathaniel.tvhome.wallpaper.Wallpaper
import com.nathaniel.tvhome.wallpaper.WallpaperCatalog
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WallpapersScreen() {
    val container = LocalContainer.current
    val navigator = LocalNavigator.current
    val prefs by container.preferences.preferences.collectAsStateWithLifecycle()
    val failed by container.scenes.failedWallpaper.collectAsStateWithLifecycle()
    val currentId = prefs.wallpaperId.ifEmpty { WallpaperCatalog.default.id }
    var focusedId by remember { mutableStateOf<String?>(null) }

    // Focusing a thumbnail previews it full-screen behind the gallery; leaving restores the choice.
    LaunchedEffect(focusedId) {
        delay(450)
        container.scenes.previewWallpaperId.value = focusedId
    }
    DisposableEffect(Unit) { onDispose { container.scenes.previewWallpaperId.value = null } }
    LaunchedEffect(failed) {
        if (failed != null && failed == focusedId) navigator.toast("Couldn't download this photo — check the internet connection")
    }

    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { awaitFrame(); runCatching { firstFocus.requestFocus() } }
    val initialIndex = remember { WallpaperCatalog.all.indexOfFirst { it.id == currentId }.coerceAtLeast(0) }

    ScreenScaffold(title = "Wallpapers", subtitle = "Focus to preview  ·  OK to set as your scenery") {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(top = 6.dp, bottom = 48.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            itemsIndexed(WallpaperCatalog.all, key = { _, w -> w.id }) { index, wallpaper ->
                WallpaperTile(
                    wallpaper = wallpaper,
                    selected = wallpaper.id == currentId,
                    modifier = Modifier
                        .then(if (index == initialIndex) Modifier.focusRequester(firstFocus) else Modifier)
                        .onFocusChanged { if (it.hasFocus) focusedId = wallpaper.id },
                    onClick = {
                        container.scope.launch { container.preferences.setWallpaper(wallpaper.id) }
                        navigator.toast("${wallpaper.title} set as your scenery")
                    },
                )
            }
        }
    }
}

@Composable
private fun WallpaperTile(wallpaper: Wallpaper, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val container = LocalContainer.current
    val thumbnail by produceState<ImageBitmap?>(null, wallpaper.id) { value = container.wallpapers.loadThumbnail(wallpaper) }
    val shape = RoundedCornerShape(16.dp)
    Column(modifier) {
        FocusTile(onClick = onClick, shape = shape, focusedScale = 1.07f, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(shape).background(Color.White.copy(alpha = 0.08f))) {
                val image = thumbnail
                if (image != null) {
                    Image(image, contentDescription = wallpaper.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Rounded.CloudDownload, contentDescription = null, tint = Scenic.TextTertiary, modifier = Modifier.align(Alignment.Center).size(28.dp))
                }
                if (selected) {
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(8.dp).size(26.dp).clip(CircleShape).background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = "Current", tint = Scenic.Night, modifier = Modifier.size(18.dp))
                    }
                }
                Box(Modifier.fillMaxWidth().height(36.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)))))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(wallpaper.title, style = Scenic.Label)
        Text(wallpaper.credit, style = Scenic.Caption.copy(color = Scenic.TextTertiary))
    }
}
