package com.mdimitrov.puzzles

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mdimitrov.puzzles.settings.ThemeChoice
import com.mdimitrov.puzzles.settings.Themes
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What the app looks like *around* what it draws.
 *
 * From API 35 the system draws edge to edge whether an app asks or not, so the strip behind each
 * bar shows the app's own background and the icons on it are whatever the app last said.
 * Robolectric renders neither the bars nor their icons, so nothing on a workstation can see a dark
 * app with dark icons on it — the combination that leaves them invisible.
 *
 * The palette is set here rather than read from the phone: what the app draws in is the player's
 * answer, not the system's, and a test that asked the system would pass on a fresh install and
 * fail on a device someone has used. It is set through the app's own graph, reached through the
 * activity it injected, because a second graph means a second store opened on one preference file
 * and DataStore refuses that.
 */
@RunWith(AndroidJUnit4::class)
class EdgeToEdgeTest {
    @Test
    fun theBarIconsAreLegibleAgainstTheDarkPalette() = assertIconsMatch(ThemeChoice.DARK)

    @Test
    fun theBarIconsAreLegibleAgainstTheLightPalette() = assertIconsMatch(ThemeChoice.LIGHT)

    @Test
    fun theAppDrawsBehindTheSystemBars() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val insets = ViewCompat.getRootWindowInsets(activity.window.decorView)
                val top = insets?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0

                assertTrue(top > 0, "the window reported no status bar to draw behind")
                assertEquals(
                    0,
                    activity.findViewById<View>(android.R.id.content).paddingTop,
                    "the content is inset for the bar, so the app is not drawing behind it",
                )
            }
        }
    }

    private fun assertIconsMatch(choice: ThemeChoice) {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var themes: Themes
            scenario.onActivity { themes = it.themes }
            runBlocking { themes.choose(choice) }

            // Created again rather than left to update in place, so that what is asserted is what
            // a player sees on the launch after the one they chose in.
            scenario.recreate()
            scenario.onActivity { activity ->
                val bars = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                val light = choice == ThemeChoice.LIGHT

                assertEquals(light, bars.isAppearanceLightStatusBars, "the clock and the battery")
                assertEquals(light, bars.isAppearanceLightNavigationBars, "the gesture handle")
            }
        }
    }
}
