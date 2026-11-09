package com.nathaniel.tvhome.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.nathaniel.tvhome.ui.theme.Scenic

/** Shared spring for every focus scale so motion feels consistent. */
val FocusSpring = spring<Float>(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)

/**
 * Remote-friendly focusable: OK/Select triggers [onClick], long-press or the Menu key
 * triggers [onLongClick]. Reports focus through [interactionSource].
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.tvClickable(
    interactionSource: MutableInteractionSource,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = this
    .onPreviewKeyEvent {
        if (onLongClick != null && it.key == Key.Menu && it.type == KeyEventType.KeyUp) {
            onLongClick(); true
        } else false
    }
    .combinedClickable(
        interactionSource = interactionSource,
        indication = null,
        onLongClick = onLongClick,
        onClick = onClick,
    )

/** A focusable glass pill / chip with an optional icon. */
@Composable
fun GlassButton(
    text: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    shape: Shape = RoundedCornerShape(50),
    enabled: Boolean = true,
    selected: Boolean = false,
    contentPadding: Dp = 18.dp,
    height: Dp = 44.dp,
    textSize: TextUnit = TextUnit.Unspecified,
    iconSize: Dp = 20.dp,
    /** Literal background colour, overriding the user's Appearance tint when set. */
    background: Color? = null,
    /** Flat white border at this alpha, overriding the default illuminated gradient edge when set. */
    borderAlpha: Float? = null,
    /** Use the more heavily blurred wallpaper variant behind this surface. */
    strongBlur: Boolean = false,
    shadowAlpha: Float = 0.45f,
    /** Accessible name for the icon, used only when this button has no visible text. */
    iconContentDescription: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, FocusSpring, label = "btnScale")
    val fill by animateColorAsState(
        when {
            focused -> Color.White.copy(alpha = 0.92f)
            selected -> Color.White.copy(alpha = 0.22f)
            else -> Color.Transparent
        },
        tween(180), label = "btnFill",
    )
    val content = if (focused) Scenic.Night else Scenic.TextPrimary.copy(alpha = if (enabled) 1f else 0.4f)
    GlassSurface(
        shape = shape,
        elevation = if (focused) 14.dp else 6.dp,
        background = background,
        borderAlpha = if (focused) 0.9f else borderAlpha,
        strongBlur = strongBlur,
        shadowAlpha = shadowAlpha,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .tvClickable(interaction) { if (enabled) onClick() },
    ) {
        if (text == null) {
            Box(Modifier.size(height).background(fill, shape), contentAlignment = Alignment.Center) {
                if (icon != null) Icon(icon, contentDescription = iconContentDescription, tint = content, modifier = Modifier.size(height * 0.46f))
            }
        } else {
            Row(
                Modifier
                    .height(height)
                    .background(fill, shape)
                    .padding(horizontal = contentPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(iconSize))
                    Spacer(Modifier.width(iconSize * 0.4f))
                }
                Text(text, style = Scenic.Label.copy(color = content, fontSize = if (textSize.isSpecified) textSize else Scenic.Label.fontSize), maxLines = 1)
            }
        }
    }
}

/** Circular glass icon button, e.g. the Settings control. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    background: Color? = null,
    borderAlpha: Float? = null,
    strongBlur: Boolean = false,
    shadowAlpha: Float = 0.45f,
) = GlassButton(
    text = null,
    onClick = onClick,
    modifier = modifier,
    icon = icon,
    shape = CircleShape,
    height = size,
    background = background,
    borderAlpha = borderAlpha,
    strongBlur = strongBlur,
    shadowAlpha = shadowAlpha,
    iconContentDescription = contentDescription,
)

/** Focus-scaled container used by tiles; content stays put while it gently grows and lifts. */
@Composable
fun FocusTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    focusedScale: Float = 1.08f,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) focusedScale else 1f, FocusSpring, label = "tileScale")
    val ring by animateFloatAsState(if (focused) 1f else 0f, tween(200), label = "tileRing")
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (focused) 22.dp else 0.dp, shape, clip = false)
            .border(2.dp, Color.White.copy(alpha = 0.85f * ring), shape)
            .tvClickable(interaction, onLongClick, onClick),
    ) { content(focused) }
}
