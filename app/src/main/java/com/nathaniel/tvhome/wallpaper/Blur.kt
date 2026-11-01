package com.nathaniel.tvhome.wallpaper

import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Fast software blur (three box-blur passes ≈ gaussian). Works on every API level, unlike RenderEffect. */
object Blur {

    /** Downscales [source] to [targetWidth] (keeping aspect) and blurs it with [radius] passes. */
    fun blurred(source: Bitmap, radius: Int, targetWidth: Int = 320): Bitmap {
        val ratio = source.height.toFloat() / source.width
        val w = targetWidth
        val h = max(1, (targetWidth * ratio).roundToInt())
        val small = Bitmap.createScaledBitmap(source, w, h, true)
        val src = if (small.config == Bitmap.Config.ARGB_8888) small else small.copy(Bitmap.Config.ARGB_8888, false)
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        val r = radius.coerceIn(1, 40)
        repeat(3) {
            boxHorizontal(pixels, w, h, r)
            boxVertical(pixels, w, h, r)
        }
        return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    }

    private fun boxHorizontal(px: IntArray, w: Int, h: Int, r: Int) {
        val out = IntArray(w)
        val div = r * 2 + 1
        for (y in 0 until h) {
            val row = y * w
            var a = 0; var rr = 0; var g = 0; var b = 0
            for (i in -r..r) {
                val c = px[row + min(w - 1, max(0, i))]
                a += c ushr 24; rr += (c shr 16) and 0xFF; g += (c shr 8) and 0xFF; b += c and 0xFF
            }
            for (x in 0 until w) {
                out[x] = ((a / div) shl 24) or ((rr / div) shl 16) or ((g / div) shl 8) or (b / div)
                val cOut = px[row + max(0, x - r)]
                val cIn = px[row + min(w - 1, x + r + 1)]
                a += (cIn ushr 24) - (cOut ushr 24)
                rr += ((cIn shr 16) and 0xFF) - ((cOut shr 16) and 0xFF)
                g += ((cIn shr 8) and 0xFF) - ((cOut shr 8) and 0xFF)
                b += (cIn and 0xFF) - (cOut and 0xFF)
            }
            System.arraycopy(out, 0, px, row, w)
        }
    }

    private fun boxVertical(px: IntArray, w: Int, h: Int, r: Int) {
        val out = IntArray(h)
        val div = r * 2 + 1
        for (x in 0 until w) {
            var a = 0; var rr = 0; var g = 0; var b = 0
            for (i in -r..r) {
                val c = px[min(h - 1, max(0, i)) * w + x]
                a += c ushr 24; rr += (c shr 16) and 0xFF; g += (c shr 8) and 0xFF; b += c and 0xFF
            }
            for (y in 0 until h) {
                out[y] = ((a / div) shl 24) or ((rr / div) shl 16) or ((g / div) shl 8) or (b / div)
                val cOut = px[max(0, y - r) * w + x]
                val cIn = px[min(h - 1, y + r + 1) * w + x]
                a += (cIn ushr 24) - (cOut ushr 24)
                rr += ((cIn shr 16) and 0xFF) - ((cOut shr 16) and 0xFF)
                g += ((cIn shr 8) and 0xFF) - ((cOut shr 8) and 0xFF)
                b += (cIn and 0xFF) - (cOut and 0xFF)
            }
            for (y in 0 until h) px[y * w + x] = out[y]
        }
    }

    /** Scales and centre-crops [source] so it exactly fills [width] x [height]. */
    fun centerCrop(source: Bitmap, width: Int, height: Int): Bitmap {
        if (source.width == width && source.height == height) return source
        val scale = max(width.toFloat() / source.width, height.toFloat() / source.height)
        val cropW = (width / scale).roundToInt().coerceAtMost(source.width)
        val cropH = (height / scale).roundToInt().coerceAtMost(source.height)
        val x = (source.width - cropW) / 2
        val y = (source.height - cropH) / 2
        val cropped = Bitmap.createBitmap(source, x, y, cropW, cropH)
        return Bitmap.createScaledBitmap(cropped, width, height, true)
    }
}
