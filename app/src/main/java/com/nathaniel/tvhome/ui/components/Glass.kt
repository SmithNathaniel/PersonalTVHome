package com.nathaniel.tvhome.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.nathaniel.tvhome.ui.theme.LocalGlassStyle
import com.nathaniel.tvhome.wallpaper.Scene
import kotlin.math.roundToInt

/** Holds the scene behind the UI and cross-fades between scenes. */
@Stable
class BackdropState {
    var current by mutableStateOf<Scene?>(null)
        private set
    var previous by mutableStateOf<Scene?>(null)
        private set
    val progress = Animatable(1f)

    suspend fun show(scene: Scene?) {
        if (scene == null || scene == current) return
        if (current == null) {
            current = scene
            progress.snapTo(0f)
            progress.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
            return
        }
        previous = current
        current = scene
        progress.snapTo(0f)
        progress.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        previous = null
    }
}

val LocalBackdrop = staticCompositionLocalOf { BackdropState() }

/** Full-screen scenic wallpaper. */
@Composable
fun ScenicBackground(state: BackdropState, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize().graphicsLayer()) {
        drawRect(Color(0xFF0B1220))
        state.previous?.let { drawScene(it, 1f) }
        state.current?.let { drawScene(it, state.progress.value) }
    }
}

private fun DrawScope.drawScene(scene: Scene, alpha: Float) {
    drawImage(
        scene.image,
        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
        alpha = alpha,
        filterQuality = FilterQuality.Medium,
    )
    // Restrained shading only where text sits: top-left header and the very bottom.
    drawRect(
        Brush.radialGradient(
            listOf(Color.Black.copy(alpha = 0.30f * alpha), Color.Transparent),
            center = Offset(0f, 0f),
            radius = size.width * 0.42f,
        ),
    )
    drawRect(
        Brush.verticalGradient(
            listOf(Color.Transparent, Color.Black.copy(alpha = 0.22f * alpha)),
            startY = size.height * 0.72f,
        ),
    )
}

/**
 * Draws the blurred scene that lies behind this element, aligned to its on-screen position,
 * giving genuine backdrop blur on Android TV versions without RenderEffect.
 */
fun Modifier.glassBackdrop(shape: Shape): Modifier = composed {
    val backdrop = LocalBackdrop.current
    val origin = remember { mutableStateOf(Offset.Zero) }
    val root = remember { mutableStateOf(IntSize.Zero) }
    this
        .onGloballyPositioned {
            origin.value = it.positionInRoot()
            root.value = it.findRootCoordinates().size
        }
        .drawWithCache {
            val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
            onDrawBehind {
                val rootSize = root.value
                if (rootSize.width == 0) return@onDrawBehind
                clipPath(path) {
                    backdrop.previous?.let { drawBackdropSlice(it.glass, origin.value, rootSize, 1f) }
                    backdrop.current?.let { drawBackdropSlice(it.glass, origin.value, rootSize, backdrop.progress.value) }
                }
            }
        }
}

private fun DrawScope.drawBackdropSlice(image: ImageBitmap, origin: Offset, root: IntSize, alpha: Float) {
    val sx = image.width / root.width.toFloat()
    val sy = image.height / root.height.toFloat()
    val srcW = (size.width * sx).roundToInt().coerceIn(1, image.width)
    val srcH = (size.height * sy).roundToInt().coerceIn(1, image.height)
    val srcX = (origin.x * sx).roundToInt().coerceIn(0, image.width - srcW)
    val srcY = (origin.y * sy).roundToInt().coerceIn(0, image.height - srcH)
    drawImage(
        image,
        srcOffset = IntOffset(srcX, srcY),
        srcSize = IntSize(srcW, srcH),
        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
        alpha = alpha,
        filterQuality = FilterQuality.Low,
    )
}

/** Delicate illuminated edge: brighter along the top, fading toward the bottom. */
fun glassEdge(alpha: Float = 1f) = Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.34f * alpha), Color.White.copy(alpha = 0.10f * alpha), Color.White.copy(alpha = 0.05f * alpha)),
)

/** Translucent glass panel: backdrop blur, restrained tint, illuminated edge and soft grounding shadow. */
@Composable
fun GlassSurface(
    shape: Shape,
    modifier: Modifier = Modifier,
    elevation: Dp = 18.dp,
    tintBoost: Float = 0f,
    content: @Composable BoxScope.() -> Unit,
) {
    val style = LocalGlassStyle.current
    Box(
        modifier
            .graphicsLayer()
            .shadow(elevation, shape, clip = false, ambientColor = Color.Black.copy(alpha = 0.45f), spotColor = Color.Black.copy(alpha = 0.55f))
            .glassBackdrop(shape)
            .background(style.tint.copy(alpha = (style.tintAlpha + tintBoost).coerceIn(0.04f, 0.9f)), shape)
            .background(
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.07f), Color.Transparent)),
                shape,
            )
            .border(1.dp, glassEdge(), shape),
        content = content,
    )
}
