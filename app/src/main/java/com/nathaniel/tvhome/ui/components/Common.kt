package com.nathaniel.tvhome.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.nathaniel.tvhome.apps.LauncherApp
import com.nathaniel.tvhome.ui.navigation.LocalNavigator
import com.nathaniel.tvhome.ui.theme.Scenic
import kotlinx.coroutines.delay

/** App icon as a refined rounded square, or a tasteful monogram if the app is missing. */
@Composable
fun AppIcon(app: LauncherApp?, fallbackName: String, size: Dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(size * 0.24f)
    if (app != null) {
        Image(
            bitmap = app.icon,
            contentDescription = app.label,
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.High,
            modifier = modifier.size(size).clip(shape),
        )
    } else {
        Box(
            modifier
                .size(size)
                .clip(shape)
                .background(Brush.linearGradient(listOf(Color(0xFF3A4458), Color(0xFF1C2230)))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                fallbackName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString(""),
                style = Scenic.Title.copy(fontSize = (size.value * 0.32f).sp),
            )
        }
    }
}

/** Common frame for secondary screens: large light title, optional subtitle, content below. */
@Composable
fun ScreenScaffold(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Column(modifier.fillMaxSize().padding(start = 56.dp, end = 56.dp, top = 36.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(title, style = Scenic.ScreenTitle)
                if (subtitle != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(subtitle, style = Scenic.BodySecondary)
                }
            }
            trailing()
        }
        Spacer(Modifier.height(22.dp))
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = Scenic.Section, modifier = modifier.padding(bottom = 12.dp))
}

/** Glass row used throughout Settings and Appearance. */
@Composable
fun GlassRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    value: String? = null,
    trailingIcon: ImageVector? = null,
    onLongClick: (() -> Unit)? = null,
    onHorizontal: ((Int) -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val fill by animateColorAsState(if (focused) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.06f), tween(160), label = "rowFill")
    val scale by animateFloatAsState(if (focused) 1.02f else 1f, FocusSpring, label = "rowScale")
    val fg = if (focused) Scenic.Night else Scenic.TextPrimary
    Row(
        modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp))
            .background(fill)
            .onPreviewKeyEvent { event ->
                if (onHorizontal == null || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> { onHorizontal(-1); true }
                    Key.DirectionRight -> { onHorizontal(1); true }
                    else -> false
                }
            }
            .tvClickable(interaction, onLongClick, onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(14.dp))
        }
        Text(title, style = Scenic.Body.copy(color = fg), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (value != null) {
            Text(value, style = Scenic.Body.copy(color = fg.copy(alpha = 0.7f)), maxLines = 1)
        }
        if (trailingIcon != null) {
            Spacer(Modifier.width(8.dp))
            Icon(trailingIcon, contentDescription = null, tint = fg.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
        }
    }
}

/** Left/right selectable option for enum preferences; OK cycles forward. */
@Composable
fun <T> OptionRow(title: String, options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, icon: ImageVector? = null) {
    val index = options.indexOf(selected).coerceAtLeast(0)
    GlassRow(
        title = title,
        icon = icon,
        value = "‹  ${label(selected)}  ›",
        onClick = { onSelect(options[(index + 1) % options.size]) },
        onHorizontal = { delta -> onSelect(options[(index + delta + options.size) % options.size]) },
    )
}

/** Transient glass message at the bottom of the screen. */
@Composable
fun MessageHost(modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.current
    val message = navigator.message
    val id = navigator.messageId
    LaunchedEffect(id) {
        if (message != null) {
            delay(3200)
            navigator.clearMessage(id)
        }
    }
    Box(modifier.fillMaxSize().padding(bottom = 36.dp), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn(tween(250)) + slideInVertically(tween(300)) { it / 2 },
            exit = fadeOut(tween(250)) + slideOutVertically(tween(300)) { it / 2 },
        ) {
            GlassSurface(RoundedCornerShape(50), tintBoost = 0.2f) {
                Text(message.orEmpty(), style = Scenic.Body, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
            }
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, contentDescription = null, tint = Scenic.TextTertiary, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, style = Scenic.Title)
        Spacer(Modifier.height(6.dp))
        Text(body, style = Scenic.BodySecondary)
    }
}
