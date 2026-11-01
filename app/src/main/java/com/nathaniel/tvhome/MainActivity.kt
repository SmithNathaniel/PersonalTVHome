package com.nathaniel.tvhome

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.nathaniel.tvhome.ui.TvHomeRoot
import com.nathaniel.tvhome.ui.navigation.Navigator
import com.nathaniel.tvhome.ui.theme.PersonalTvTheme

/** The launcher activity; registered for the HOME category so it can be the default home app. */
class MainActivity : ComponentActivity() {

    private val navigator = Navigator()
    private val container get() = (application as TvHomeApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent {
            PersonalTvTheme {
                TvHomeRoot(container, navigator)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Pressing Home while the launcher is showing returns to the home screen.
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            navigator.home()
        }
    }

    override fun onResume() {
        super.onResume()
        container.media.refresh()
        container.weather.refreshNow()
    }

}
