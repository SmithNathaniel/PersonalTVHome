package com.nathaniel.tvhome.system

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log

/** Opens native Android / Google TV settings instead of recreating them. */
class SystemSettings(private val context: Context) {

    enum class Target(val label: String) {
        Device("Device settings"),
        Display("Display & picture"),
        Sound("Sound"),
        Network("Network & internet"),
        Cast("Google Cast / Chromecast"),
        DateTime("Date & time"),
        Apps("Apps"),
        HomeApp("Default home app"),
        NotificationAccess("Now Playing access"),
    }

    /** Opens [target], falling back to the main settings screen. Returns false if nothing could open. */
    fun open(target: Target): Boolean {
        val candidates = when (target) {
            Target.Device -> listOf(Intent(Settings.ACTION_SETTINGS))
            Target.Display -> listOf(
                Intent(Settings.ACTION_DISPLAY_SETTINGS),
                Intent().setClassName("com.android.tv.settings", "com.android.tv.settings.device.display.DisplayActivity"),
            )
            Target.Sound -> listOf(
                Intent(Settings.ACTION_SOUND_SETTINGS),
                Intent().setClassName("com.android.tv.settings", "com.android.tv.settings.device.sound.SoundActivity"),
            )
            Target.Network -> listOf(Intent(Settings.ACTION_WIFI_SETTINGS), Intent(Settings.ACTION_WIRELESS_SETTINGS))
            Target.Cast -> listOf(
                Intent(Settings.ACTION_CAST_SETTINGS),
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:com.google.android.apps.mediashell")),
            )
            Target.DateTime -> listOf(Intent(Settings.ACTION_DATE_SETTINGS))
            Target.Apps -> listOf(Intent(Settings.ACTION_APPLICATION_SETTINGS), Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS))
            Target.HomeApp -> listOf(Intent(Settings.ACTION_HOME_SETTINGS))
            Target.NotificationAccess -> listOf(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        return startFirst(candidates + Intent(Settings.ACTION_SETTINGS))
    }

    fun openAppInfo(packageName: String): Boolean =
        startFirst(listOf(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))))

    private fun startFirst(intents: List<Intent>): Boolean {
        for (intent in intents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(context.packageManager) == null) continue
            val started = runCatching { context.startActivity(intent) }
                .onFailure { Log.w("SystemSettings", "Cannot open ${intent.action ?: intent.component}", it) }
                .isSuccess
            if (started) return true
        }
        return false
    }
}
