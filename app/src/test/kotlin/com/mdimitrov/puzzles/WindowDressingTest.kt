package com.mdimitrov.puzzles

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Looper
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ActivityScenario
import com.mdimitrov.puzzles.settings.ThemeChoice
import com.mdimitrov.puzzles.settings.Themes
import com.mdimitrov.puzzles.theme.DarkBackground
import com.mdimitrov.puzzles.theme.LightBackground
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * What the activity puts around the app, on the phone that disagrees with the player.
 *
 * Both cases are the palette the player chose against the opposite system mode, because that is
 * the only pairing in which choosing from the phone and choosing from the player give different
 * answers — and the app's own answer is the player's.
 *
 * Pinned to API 28 because that is where the navigation bar is a colour the app has to name.
 * From API 29 the system enforces the contrast itself and the bar is left transparent, so the
 * version that still needs saying is the one that cannot be seen on a modern emulator.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class WindowDressingTest {
    @Test
    @Config(qualifiers = "notnight")
    fun `a dark palette on a light phone is dressed dark`() =
        assertDressedFor(
            choice = ThemeChoice.DARK,
            // The platform's own scrims, from the edge-to-edge guidance: a near-opaque white under
            // dark handles, a translucent near-black under light ones.
            scrim = Color.argb(0x80, 0x1B, 0x1B, 0x1B),
            background = DarkBackground.toArgb(),
        )

    @Test
    @Config(qualifiers = "night")
    fun `a light palette on a dark phone is dressed light`() =
        assertDressedFor(
            choice = ThemeChoice.LIGHT,
            scrim = Color.argb(0xE6, 0xFF, 0xFF, 0xFF),
            background = LightBackground.toArgb(),
        )

    @Test
    @Config(qualifiers = "notnight")
    fun `the palette is in place before the first frame is composed`() {
        // The activity reads the choice before composing rather than only collecting it into the
        // composition. Collected alone, the frame is drawn in the phone's palette and corrected
        // afterwards — invisible to a test that looks at a resumed activity, and the thing a
        // player sees on every rotation. So this one never resumes: it stops after `onCreate`,
        // before anything has been composed, and asks what the window is already wearing.
        chose(ThemeChoice.DARK)

        val activity = Robolectric.buildActivity(MainActivity::class.java).create().get()

        assertEquals(
            DarkBackground.toArgb(),
            (activity.window.decorView.background as? ColorDrawable)?.color,
            "the first frame is drawn in the phone's palette and put right afterwards",
        )
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `a palette chosen while the app is running dresses it without waiting for a new launch`() {
        // The other half: the choice is made from Setup, which does not create the activity again.
        // Read once and never followed, the app keeps the palette it launched in until it is.
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var themes: Themes
            scenario.onActivity { themes = it.themes }

            runBlocking { themes.choose(ThemeChoice.DARK) }
            shadowOf(Looper.getMainLooper()).idle()

            scenario.onActivity { activity ->
                assertEquals(
                    DarkBackground.toArgb(),
                    (activity.window.decorView.background as? ColorDrawable)?.color,
                    "the app kept the palette it launched in",
                )
            }
        }
    }

    @Test
    @Config(qualifiers = "night")
    fun `a player who has never chosen is dressed by the phone`() {
        // The other half of the question, and the one a fresh install asks: with nothing stored,
        // the phone's own mode decides. Both the fallback and the reading of the mode could be
        // inverted without a single test going red, and a first launch on a dark phone would open
        // white.
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(
                    DarkBackground.toArgb(),
                    (activity.window.decorView.background as? ColorDrawable)?.color,
                    "a phone in dark mode was answered with the light palette",
                )
            }
        }
    }

    /** Writes a choice through the app's own graph, the only way to reach the one store. */
    private fun chose(choice: ThemeChoice) {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var themes: Themes
            scenario.onActivity { themes = it.themes }
            runBlocking { themes.choose(choice) }
        }
    }

    // The colour behind the navigation bar is deprecated on the versions that no longer use it,
    // and is still the only thing below API 29 that says what is drawn there.
    @Suppress("DEPRECATION")
    private fun assertDressedFor(
        choice: ThemeChoice,
        scrim: Int,
        background: Int,
    ) {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var themes: Themes
            scenario.onActivity { themes = it.themes }
            runBlocking { themes.choose(choice) }

            // Created again rather than left to update in place, so that what is asserted is the
            // launch after the one the player chose in — the launch that used to flash.
            scenario.recreate()
            scenario.onActivity { activity ->
                assertEquals(
                    scrim,
                    activity.window.navigationBarColor,
                    "the strip behind the navigation bar answers to the phone, not the player",
                )
                assertEquals(
                    background,
                    (activity.window.decorView.background as? ColorDrawable)?.color,
                    "the window behind the app answers to the phone, not the player",
                )
            }
        }
    }
}
