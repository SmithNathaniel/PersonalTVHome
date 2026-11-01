package com.nathaniel.tvhome.apps

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.DisplayMetrics
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Discovers launchable applications, renders their icons and launches them. */
class AppRepository(private val context: Context, private val scope: CoroutineScope) {

    private val pm: PackageManager = context.packageManager
    private val _apps = MutableStateFlow<List<LauncherApp>>(emptyList())
    val apps: StateFlow<List<LauncherApp>> = _apps.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private var refreshJob: Job? = null

    init {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(
            context,
            object : BroadcastReceiver() {
                override fun onReceive(c: Context, intent: Intent) = refresh()
            },
            filter,
            ContextCompat.RECEIVER_EXPORTED,
        )
        refresh()
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = scope.launch(Dispatchers.IO) {
            val loadedApps = runCatching { queryApps() }
                .onFailure { Log.w(TAG, "App discovery failed", it) }
                .getOrDefault(_apps.value)
            _apps.value = loadedApps
            _loaded.value = true
        }
    }

    fun find(packageName: String): LauncherApp? = _apps.value.firstOrNull { it.packageName == packageName }

    fun isInstalled(packageName: String): Boolean =
        runCatching { pm.getApplicationInfo(packageName, 0); true }.getOrDefault(false)

    /** Launches an app, preferring its TV (leanback) entry point. */
    fun launch(packageName: String): Boolean {
        val intent = pm.getLeanbackLaunchIntentForPackage(packageName)
            ?: pm.getLaunchIntentForPackage(packageName)
            ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return runCatching { context.startActivity(intent); true }
            .onFailure { Log.w(TAG, "Unable to launch $packageName", it) }
            .getOrDefault(false)
    }

    /** Opens the store listing for an app that is pinned but not installed. */
    fun openStoreListing(packageName: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent); true }.getOrDefault(false)
    }

    private fun queryApps(): List<LauncherApp> {
        val components = LinkedHashMap<String, ComponentName>()
        listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER).forEach { category ->
            val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
            pm.queryIntentActivities(intent, 0).forEach { info ->
                val pkg = info.activityInfo.packageName
                if (pkg != context.packageName && pkg !in components) {
                    components[pkg] = ComponentName(pkg, info.activityInfo.name)
                }
            }
        }
        return components.keys.mapNotNull { pkg ->
            runCatching {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                LauncherApp(
                    packageName = pkg,
                    label = pm.getApplicationLabel(appInfo).toString(),
                    icon = loadIcon(appInfo),
                    banner = loadBanner(appInfo),
                    category = categorise(appInfo),
                )
            }.onFailure { Log.w(TAG, "Skipping $pkg", it) }.getOrNull()
        }.sortedBy { it.label.lowercase() }
    }

    private fun loadIcon(appInfo: ApplicationInfo): ImageBitmap {
        val drawable = runCatching {
            val res = pm.getResourcesForApplication(appInfo)
            if (appInfo.icon != 0) res.getDrawableForDensity(appInfo.icon, DisplayMetrics.DENSITY_XXHIGH, null) else null
        }.getOrNull() ?: pm.getApplicationIcon(appInfo)
        return renderSquare(drawable, ICON_PX)
    }

    private fun loadBanner(appInfo: ApplicationInfo): ImageBitmap? {
        val drawable = runCatching { pm.getApplicationBanner(appInfo) }.getOrNull() ?: return null
        val bitmap = Bitmap.createBitmap(BANNER_W, BANNER_H, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, BANNER_W, BANNER_H)
        drawable.draw(Canvas(bitmap))
        return bitmap.asImageBitmap()
    }

    /** Draws adaptive icons full-bleed (unmasked) so Compose can apply a consistent rounded square. */
    private fun renderSquare(drawable: Drawable, size: Int): ImageBitmap {
        if (drawable is BitmapDrawable && drawable.bitmap.width == size && drawable.bitmap.height == size) {
            return drawable.bitmap.asImageBitmap()
        }
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (drawable is AdaptiveIconDrawable) {
            val inset = size / 4
            listOfNotNull(drawable.background, drawable.foreground).forEach {
                it.setBounds(-inset, -inset, size + inset, size + inset)
                it.draw(canvas)
            }
        } else {
            drawable.setBounds(0, 0, size, size)
            drawable.draw(canvas)
        }
        return bitmap.asImageBitmap()
    }

    private fun categorise(appInfo: ApplicationInfo): AppCategory {
        knownCategories[appInfo.packageName]?.let { return it }
        return when (appInfo.category) {
            ApplicationInfo.CATEGORY_VIDEO, ApplicationInfo.CATEGORY_IMAGE -> AppCategory.Video
            ApplicationInfo.CATEGORY_AUDIO -> AppCategory.Music
            ApplicationInfo.CATEGORY_GAME -> AppCategory.Games
            else -> AppCategory.Other
        }
    }

    private companion object {
        const val TAG = "AppRepository"
        const val ICON_PX = 192
        const val BANNER_W = 400
        const val BANNER_H = 225

        val knownCategories = mapOf(
            "com.netflix.ninja" to AppCategory.Video,
            "com.netflix.mediaclient" to AppCategory.Video,
            "com.google.android.youtube.tv" to AppCategory.Video,
            "com.dstvmobile.android" to AppCategory.Video,
            "com.rumble.battles" to AppCategory.Video,
            "tv.sabcplus.vod" to AppCategory.Video,
            "com.apple.atve.androidtv.appletv" to AppCategory.Video,
            "com.mutv.manutd" to AppCategory.Video,
            "com.africa.elephtvplus" to AppCategory.Video,
            "com.google.android.youtube.tvmusic" to AppCategory.Music,
            "com.soundcloud.android" to AppCategory.Music,
            "com.spotify.tv.android" to AppCategory.Music,
        )
    }
}
