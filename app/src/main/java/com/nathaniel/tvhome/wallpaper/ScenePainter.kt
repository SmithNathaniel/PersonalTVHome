package com.nathaniel.tvhome.wallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.core.graphics.ColorUtils
import kotlin.math.pow
import kotlin.random.Random

/** Paints layered mountain landscapes with atmospheric perspective. */
object ScenePainter {

    enum class Palette(
        val title: String,
        val skyTop: Int,
        val skyHorizon: Int,
        val sun: Int,
        val sunX: Float,
        val sunY: Float,
        val far: Int,
        val near: Int,
        val stars: Boolean,
        val seed: Int,
    ) {
        GoldenDusk("Golden Dusk", 0xFF1D2B53.toInt(), 0xFFF2A65A.toInt(), 0xFFFFE2A8.toInt(), 0.66f, 0.50f, 0xFFB9768A.toInt(), 0xFF141A2E.toInt(), false, 7),
        BlueHour("Blue Hour", 0xFF070B1E.toInt(), 0xFF4B6CB7.toInt(), 0xFFB8C9FF.toInt(), 0.28f, 0.42f, 0xFF4A5C93.toInt(), 0xFF0A0F22.toInt(), true, 21),
        AlpineDawn("Alpine Dawn", 0xFF3A6EA5.toInt(), 0xFFFAD6C4.toInt(), 0xFFFFF4E0.toInt(), 0.5f, 0.56f, 0xFF9DB4CF.toInt(), 0xFF1E3346.toInt(), false, 42),
    }

    fun paint(palette: Palette, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
        val horizon = height * 0.62f

        paint.shader = LinearGradient(0f, 0f, 0f, horizon, palette.skyTop, palette.skyHorizon, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        val random = Random(palette.seed)
        if (palette.stars) {
            paint.shader = null
            repeat(220) {
                val y = random.nextFloat().pow(1.6f) * horizon * 0.8f
                paint.color = ColorUtils.setAlphaComponent(0xFFFFFFFF.toInt(), (40 + random.nextInt(170)))
                canvas.drawCircle(random.nextFloat() * width, y, 0.6f + random.nextFloat() * 1.4f, paint)
            }
        }

        val sx = width * palette.sunX
        val sy = height * palette.sunY
        val glow = height * 0.55f
        paint.shader = RadialGradient(sx, sy, glow, intArrayOf(
            ColorUtils.setAlphaComponent(palette.sun, 200),
            ColorUtils.setAlphaComponent(palette.sun, 60),
            ColorUtils.setAlphaComponent(palette.sun, 0),
        ), floatArrayOf(0f, 0.35f, 1f), Shader.TileMode.CLAMP)
        canvas.drawCircle(sx, sy, glow, paint)
        paint.shader = null
        paint.color = palette.sun
        canvas.drawCircle(sx, sy, height * 0.035f, paint)

        val layers = 6
        for (i in 0 until layers) {
            val t = i / (layers - 1f)
            val base = horizon - height * 0.16f + t * height * 0.42f
            val amplitude = height * (0.20f - t * 0.08f)
            val color = ColorUtils.blendARGB(palette.far, palette.near, t.pow(0.8f))
            val path = ridge(width, height, base, amplitude, roughness = 0.52f + t * 0.06f, random = random)
            paint.shader = LinearGradient(0f, base - amplitude, 0f, height.toFloat(),
                color, ColorUtils.blendARGB(color, palette.near, 0.6f), Shader.TileMode.CLAMP)
            canvas.drawPath(path, paint)

            // Mist between ridges for depth.
            if (i < layers - 1) {
                paint.shader = LinearGradient(0f, base - amplitude * 0.2f, 0f, base + amplitude * 0.6f,
                    ColorUtils.setAlphaComponent(palette.skyHorizon, 0),
                    ColorUtils.setAlphaComponent(palette.skyHorizon, (70 * (1 - t)).toInt()), Shader.TileMode.CLAMP)
                canvas.drawRect(0f, base - amplitude * 0.2f, width.toFloat(), base + amplitude * 0.6f, paint)
            }
        }
        return bitmap
    }

    /** Midpoint-displacement ridge line closed to the bottom of the canvas. */
    private fun ridge(width: Int, height: Int, base: Float, amplitude: Float, roughness: Float, random: Random): Path {
        val segments = 256
        val heights = FloatArray(segments + 1)
        heights[0] = random.nextFloat() - 0.5f
        heights[segments] = random.nextFloat() - 0.5f
        var step = segments
        var scale = 1f
        while (step > 1) {
            val half = step / 2
            var i = half
            while (i < segments) {
                heights[i] = (heights[i - half] + heights[i + half]) / 2f + (random.nextFloat() - 0.5f) * scale
                i += step
            }
            scale *= roughness
            step = half
        }
        val path = Path()
        path.moveTo(0f, height.toFloat())
        for (i in 0..segments) {
            path.lineTo(width * i / segments.toFloat(), base - heights[i] * amplitude * 1.6f)
        }
        path.lineTo(width.toFloat(), height.toFloat())
        path.close()
        return path
    }
}
