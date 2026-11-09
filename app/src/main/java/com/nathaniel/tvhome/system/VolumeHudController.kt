package com.nathaniel.tvhome.system

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object VolumeHudController {
    private val _percent = MutableStateFlow<Int?>(null)
    val percent: StateFlow<Int?> = _percent.asStateFlow()
    private val handler = Handler(Looper.getMainLooper())
    private val hideHud = Runnable { _percent.value = null }

    fun adjust(context: Context, direction: Int) {
        val audio = context.getSystemService(AudioManager::class.java) ?: return
        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)
        publish(audio)
    }

    fun refresh(context: Context) {
        val audio = context.getSystemService(AudioManager::class.java) ?: return
        publish(audio)
    }

    private fun publish(audio: AudioManager) {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val percent = if (max > 0) audio.getStreamVolume(AudioManager.STREAM_MUSIC) * 100 / max else 0
        handler.post {
            _percent.value = percent
            handler.removeCallbacks(hideHud)
            handler.postDelayed(hideHud, HUD_TIMEOUT_MS)
        }
    }

    private const val HUD_TIMEOUT_MS = 1_800L
}
