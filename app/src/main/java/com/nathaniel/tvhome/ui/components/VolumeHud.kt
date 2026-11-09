package com.nathaniel.tvhome.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text

@Composable
fun VolumeHud(percent: Int?) {
    AnimatedVisibility(
        visible = percent != null,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(tween(120)) + scaleIn(tween(220, easing = FastOutSlowInEasing), initialScale = 0.94f),
        exit = fadeOut(tween(260)) + scaleOut(tween(220, easing = FastOutSlowInEasing), targetScale = 0.97f),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            VolumePill(percent ?: 0)
        }
    }
}

@Composable
private fun VolumePill(percent: Int) {
    val shape = CircleShape
    val targetFraction = (percent.coerceIn(0, 100) / 100f)
    val fraction by animateFloatAsState(targetFraction, tween(220, easing = FastOutSlowInEasing), label = "volumeFill")

    GlassSurface(
        shape = shape,
        modifier = Modifier.width(520.dp).height(104.dp),
        elevation = 22.dp,
        background = Color(0xFF17191B).copy(alpha = 0.46f),
        borderAlpha = 0.28f,
        strongBlur = true,
        shadowAlpha = 0.22f,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().clip(shape)) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.1f)),
                        ),
                        shape,
                    ),
            )
            if (fraction > 0.01f) {
                val thumbOffset = (maxWidth * fraction - 24.dp).coerceIn(0.dp, maxWidth - 48.dp)
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = thumbOffset)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                )
            }
            Row(
                Modifier.align(Alignment.CenterStart).padding(start = 36.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(16.dp))
                Text(
                    "$percent",
                    style = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontFamily = com.nathaniel.tvhome.ui.theme.InterText,
                        fontSize = 25.sp,
                    ),
                )
            }
        }
    }
}
