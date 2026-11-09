package com.nathaniel.tvhome.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.nathaniel.tvhome.apps.FavouriteApps
import com.nathaniel.tvhome.apps.LauncherApp
import com.nathaniel.tvhome.ui.components.AppIcon
import com.nathaniel.tvhome.ui.components.FocusSpring
import com.nathaniel.tvhome.ui.components.GlassIconButton
import com.nathaniel.tvhome.ui.components.GlassSurface
import com.nathaniel.tvhome.ui.components.tvClickable
import com.nathaniel.tvhome.ui.theme.GlassSpec
import com.nathaniel.tvhome.ui.theme.Scenic
import kotlin.math.sin

data class DockEntry(val packageName: String, val app: LauncherApp?) {
    val name: String get() = app?.label ?: FavouriteApps.nameFor(packageName) ?: packageName
}

/**
 * One compact floating glass dock. Focused icons grow via graphicsLayer only, so the dock
 * keeps its size and neighbouring icons never move.
 */
@Composable
fun AppDock(
    entries: List<DockEntry>,
    iconSize: Int,
    gap: Int,
    focusRequesters: List<FocusRequester>,
    focusedIndex: Int,
    onFocusIndex: (Int) -> Unit,
    onOpen: (DockEntry) -> Unit,
    onOptions: (DockEntry) -> Unit,
    onAllApps: () -> Unit,
    festive: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Invisible counterweight keeps the dock itself perfectly centred despite the All Apps control.
            Spacer(Modifier.width(ALL_APPS_SIZE.dp + ALL_APPS_GAP.dp))
            Box {
                GlassSurface(
                    shape = RoundedCornerShape(20.dp),
                    elevation = 18.dp,
                    background = GlassSpec.DockBackground,
                    borderAlpha = GlassSpec.DockBorderAlpha,
                    strongBlur = true,
                    shadowAlpha = GlassSpec.DockShadowAlpha,
                ) {
                    Row(
                        Modifier.padding(horizontal = (gap + 1).dp, vertical = (iconSize * 0.227f).dp),
                        horizontalArrangement = Arrangement.spacedBy(gap.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        entries.forEachIndexed { index, entry ->
                            DockIcon(
                                entry = entry,
                                size = iconSize,
                                festive = festive,
                                modifier = Modifier
                                    .focusRequester(focusRequesters[index])
                                    .onFocusChanged { if (it.isFocused) onFocusIndex(index) },
                                onClick = { onOpen(entry) },
                                onLongClick = { onOptions(entry) },
                            )
                        }
                    }
                }
                if (festive) {
                    ChristmasGarland(
                        Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-15).dp)
                            .width((entries.size * iconSize + (entries.size - 1) * gap + 2 * (gap + 1)).dp)
                            .height(20.dp),
                    )
                }
            }
            Spacer(Modifier.width(ALL_APPS_GAP.dp))
            GlassIconButton(
                icon = Icons.Rounded.Apps,
                contentDescription = "All apps",
                onClick = onAllApps,
                size = ALL_APPS_SIZE.dp,
                background = GlassSpec.DockBackground,
                borderAlpha = GlassSpec.DockBorderAlpha,
                strongBlur = true,
                shadowAlpha = GlassSpec.DockShadowAlpha,
                modifier = Modifier.onFocusChanged { if (it.isFocused) onFocusIndex(ALL_APPS_INDEX) },
            )
        }
        Spacer(Modifier.height(13.dp))
        // Discreet caption naming the focused app; no permanent text labels in the dock.
        AnimatedContent(
            targetState = when (focusedIndex) {
                ALL_APPS_INDEX -> "All apps"
                in entries.indices -> entries[focusedIndex].name
                else -> ""
            },
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
            label = "dockCaption",
        ) { caption ->
            Text(caption, style = Scenic.Caption.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Scenic.TextPrimary, shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 1f), 8f)))
        }
    }
}

@Composable
private fun DockIcon(
    entry: DockEntry,
    size: Int,
    festive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.16f else 1f, FocusSpring, label = "dockScale")
    val highlight by animateFloatAsState(if (focused) 1f else 0f, tween(220), label = "dockHighlight")
    val shape = RoundedCornerShape((size * 0.24f).dp)
    Box(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = -6.dp.toPx() * highlight
            }
            .shadow((13 * highlight).dp, shape, clip = false, ambientColor = Color.White.copy(alpha = 0.6f), spotColor = Color.Black)
            .border(1.5.dp, Color.White.copy(alpha = 0.18f + 0.62f * highlight), shape)
            .tvClickable(interaction, onLongClick, onClick),
    ) {
        AppIcon(entry.app, entry.name, size.dp)
        if (festive) ChristmasHat(Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-7).dp).size(22.dp))
    }
}

@Composable
internal fun ChristmasHat(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val hat = Path().apply {
            moveTo(size.width * 0.12f, size.height * 0.7f)
            lineTo(size.width * 0.83f, size.height * 0.55f)
            lineTo(size.width * 0.64f, size.height * 0.12f)
            quadraticTo(size.width * 0.33f, size.height * 0.2f, size.width * 0.12f, size.height * 0.7f)
            close()
        }
        drawPath(hat, Color(0xFFD94343))
        drawRoundRect(
            Color.White,
            topLeft = Offset(size.width * 0.08f, size.height * 0.64f),
            size = Size(size.width * 0.82f, size.height * 0.2f),
            cornerRadius = CornerRadius(size.height * 0.1f),
        )
        drawCircle(Color.White, radius = size.minDimension * 0.12f, center = Offset(size.width * 0.64f, size.height * 0.1f))
    }
}

@Composable
private fun ChristmasGarland(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val cord = Path()
        val pointCount = 40
        repeat(pointCount + 1) { index ->
            val fraction = index.toFloat() / pointCount
            val x = size.width * fraction
            val y = size.height * (0.16f + 0.54f * sin(fraction * Math.PI).toFloat())
            if (index == 0) cord.moveTo(x, y) else cord.lineTo(x, y)
        }
        drawPath(cord, Color(0xFF456540), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        val bulbs = listOf(Color(0xFFFFD978), Color(0xFFE85B53), Color(0xFFEAF2CC))
        repeat(17) { index ->
            val fraction = index / 16f
            val x = size.width * fraction
            val y = size.height * (0.16f + 0.54f * sin(fraction * Math.PI).toFloat())
            drawCircle(bulbs[index % bulbs.size].copy(alpha = 0.24f), radius = 7.dp.toPx(), center = Offset(x, y))
            drawCircle(bulbs[index % bulbs.size], radius = 3.dp.toPx(), center = Offset(x, y))
        }
    }
}

const val ALL_APPS_INDEX = -2
private const val ALL_APPS_SIZE = 44
private const val ALL_APPS_GAP = 11
