package com.nathaniel.tvhome

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.nathaniel.tvhome.apps.AppCategory
import com.nathaniel.tvhome.apps.LauncherApp
import com.nathaniel.tvhome.media.MediaState
import com.nathaniel.tvhome.media.NowPlaying
import com.nathaniel.tvhome.ui.LocalActions
import com.nathaniel.tvhome.ui.LocalContainer
import com.nathaniel.tvhome.ui.TvHomeRoot
import com.nathaniel.tvhome.ui.components.BackdropState
import com.nathaniel.tvhome.ui.components.LocalBackdrop
import com.nathaniel.tvhome.ui.components.ScenicBackground
import com.nathaniel.tvhome.ui.home.NowPlayingWidget
import com.nathaniel.tvhome.ui.navigation.LocalNavigator
import com.nathaniel.tvhome.ui.navigation.Navigator
import com.nathaniel.tvhome.ui.navigation.Screen
import com.nathaniel.tvhome.ui.rememberLauncherActions
import com.nathaniel.tvhome.ui.screens.AppGrid
import com.nathaniel.tvhome.ui.screens.ReadabilityShade
import com.nathaniel.tvhome.ui.screens.NowPlayingContent
import com.nathaniel.tvhome.ui.theme.PersonalTvTheme
import com.nathaniel.tvhome.wallpaper.Blur
import com.nathaniel.tvhome.wallpaper.Scene
import com.nathaniel.tvhome.wallpaper.ScenePainter
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders key screens to PNG (app/build/outputs/roborazzi) for design review, since
 * many TVs block screen capture. Uses synthetic images only inside these tests.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-xhdpi")
class ScreenshotTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val container get() = ApplicationProvider.getApplicationContext<TvHomeApplication>().container

    private fun paintedScene(): Scene {
        val bmp = ScenePainter.paint(ScenePainter.Palette.GoldenDusk, 1920, 1080)
        return Scene("test", bmp.asImageBitmap(), Blur.blurred(bmp, 7).asImageBitmap())
    }

    @Composable
    private fun Frame(navigator: Navigator = remember { Navigator() }, content: @Composable () -> Unit) {
        val backdrop = remember { BackdropState() }
        val scene = remember { paintedScene() }
        LaunchedEffect(Unit) { backdrop.show(scene) }
        PersonalTvTheme {
            CompositionLocalProvider(
                LocalContainer provides container,
                LocalNavigator provides navigator,
                LocalBackdrop provides backdrop,
            ) {
                CompositionLocalProvider(LocalActions provides rememberLauncherActions(container)) {
                    Box(Modifier.fillMaxSize()) {
                        ScenicBackground(backdrop)
                        content()
                    }
                }
            }
        }
    }

    // The live clock and ambient animations never go idle, so drive the clock manually.
    @org.junit.Before
    fun manualClock() {
        compose.mainClock.autoAdvance = false
    }

    private fun waitForScene() {
        val deadline = System.currentTimeMillis() + 60_000
        while (container.scenes.scene.value == null && System.currentTimeMillis() < deadline) {
            compose.mainClock.advanceTimeBy(100)
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            Thread.sleep(50)
        }
        compose.mainClock.advanceTimeBy(2_000)
    }

    @Test
    fun home() {
        val navigator = Navigator()
        compose.setContent { PersonalTvTheme { TvHomeRoot(container, navigator) } }
        waitForScene()
        compose.onRoot().captureRoboImage("build/screens/01_home.png")
    }

    @Test
    fun settingsPanel() {
        val navigator = Navigator()
        compose.setContent { PersonalTvTheme { TvHomeRoot(container, navigator) } }
        waitForScene()
        navigator.openSettings()
        compose.mainClock.advanceTimeBy(1_500)
        compose.onRoot().captureRoboImage("build/screens/02_settings.png")
    }

    @Test
    fun appearance() {
        val navigator = Navigator()
        compose.setContent { PersonalTvTheme { TvHomeRoot(container, navigator) } }
        waitForScene()
        navigator.navigate(Screen.Appearance)
        compose.mainClock.advanceTimeBy(1_500)
        compose.onRoot().captureRoboImage("build/screens/03_appearance.png")
    }

    @Test
    fun wallpapers() {
        val navigator = Navigator()
        compose.setContent { PersonalTvTheme { TvHomeRoot(container, navigator) } }
        waitForScene()
        navigator.navigate(Screen.Wallpapers)
        compose.mainClock.advanceTimeBy(1_500)
        compose.onRoot().captureRoboImage("build/screens/04_wallpapers.png")
    }

    @Test
    fun nowPlayingWidget() {
        compose.setContent {
            Frame {
                Box(Modifier.fillMaxSize().padding(top = 40.dp, end = 48.dp), contentAlignment = Alignment.TopEnd) {
                    NowPlayingWidget(MediaState.Active(sampleTrack()), {}, {}, {}, {}, {})
                }
            }
        }
        compose.mainClock.advanceTimeBy(1_500)
        compose.onRoot().captureRoboImage("build/screens/05_widget.png")
    }

    @Test
    fun nowPlayingFullScreen() {
        compose.setContent {
            Frame {
                val np = sampleTrack()
                ReadabilityShade()
                NowPlayingContent(np)
            }
        }
        compose.mainClock.advanceTimeBy(1_500)
        compose.onRoot().captureRoboImage("build/screens/06_now_playing.png")
    }

    @Test
    fun appsGrid() {
        compose.setContent {
            Frame {
                Box(Modifier.fillMaxSize().padding(horizontal = 56.dp, vertical = 100.dp)) {
                    AppGrid(sampleApps())
                }
            }
        }
        compose.mainClock.advanceTimeBy(1_500)
        compose.onRoot().captureRoboImage("build/screens/07_apps.png")
    }

    private fun gradientBitmap(w: Int, h: Int, a: Int, b: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(bmp).drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply {
            shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), a, b, Shader.TileMode.CLAMP)
        })
        return bmp
    }

    private fun sampleTrack(): NowPlaying {
        val art = gradientBitmap(512, 512, 0xFFE86A5C.toInt(), 0xFF3B2B7A.toInt())
        return NowPlaying(
            packageName = "com.soundcloud.android", platform = "SoundCloud", title = "Test Track Title",
            artist = "Test Artist", album = "Test Album", artwork = art.asImageBitmap(),
            durationMs = 215_000, positionMs = 83_000, positionUpdatedAt = SystemClock.elapsedRealtime(),
            speed = 1f, isPlaying = true, canPlayPause = true, canSkipNext = true, canSkipPrevious = true,
        )
    }

    private fun sampleApps(): List<LauncherApp> {
        val colors = listOf(0xFFE50914, 0xFFFF5500, 0xFFFF0000, 0xFF0E7C3A, 0xFF1C4E9C, 0xFF6A1B9A, 0xFF00897B, 0xFFF9A825)
        return (1..14).map { i ->
            val c = colors[i % colors.size].toInt()
            LauncherApp("pkg.$i", "Application $i", gradientBitmap(192, 192, c, 0xFF101010.toInt()).asImageBitmap(), null, AppCategory.Other)
        }
    }
}
