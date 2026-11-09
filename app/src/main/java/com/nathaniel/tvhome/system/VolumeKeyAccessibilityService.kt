package com.nathaniel.tvhome.system

import android.accessibilityservice.AccessibilityService
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.accessibilityservice.AccessibilityServiceInfo
import android.media.AudioManager

class VolumeKeyAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val direction = when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> AudioManager.ADJUST_RAISE
            KeyEvent.KEYCODE_VOLUME_DOWN -> AudioManager.ADJUST_LOWER
            else -> return false
        }
        if (event.action == KeyEvent.ACTION_DOWN) VolumeHudController.adjust(this, direction)
        return true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (
            event?.packageName?.toString() == "com.android.systemui" &&
            event.className?.toString()?.contains("Volume", ignoreCase = true) == true
        ) {
            VolumeHudController.refresh(this)
        }
    }

    override fun onInterrupt() = Unit
}
