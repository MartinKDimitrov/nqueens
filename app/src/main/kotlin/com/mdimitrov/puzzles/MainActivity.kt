package com.mdimitrov.puzzles

import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import com.mdimitrov.puzzles.puzzletype.Puzzles
import com.mdimitrov.puzzles.settings.ThemeChoice
import com.mdimitrov.puzzles.settings.Themes
import com.mdimitrov.puzzles.theme.DarkBackground
import com.mdimitrov.puzzles.theme.LightBackground
import com.mdimitrov.puzzles.theme.PuzzleTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * What the system paints behind the navigation bar below API 29, where it will not put the app's
 * own background there. Translucent enough to leave the app visible through it, opaque enough for
 * the handle drawn on top to be legible against it. The values are the platform's own.
 */
private val LightScrim = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
private val DarkScrim = Color.argb(0x80, 0x1B, 0x1B, 0x1B)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /**
     * Every puzzle this build was assembled with. The activity holds the list because the
     * navigation graph needs it, and knows nothing of what is in it. What still names a game is
     * the app's own label and its launcher icon, which change when a second puzzle ships.
     */
    @Inject
    lateinit var puzzles: Puzzles

    /**
     * Which palette the player has asked for. Read here rather than inside a screen because it
     * dresses all of them, and because what surrounds the screens — the window behind them and the
     * system's own bars — is the activity's to set and not a composable's.
     */
    @Inject
    lateinit var themes: Themes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Read before the first frame rather than only collected into it. Composing without it
        // means every creation of this activity — a rotation as much as a cold start — draws a
        // frame in the phone's palette before flipping to the player's, and on the combination
        // that disagrees that frame is the unreadable one.
        //
        // `chosen` and not `choice`, because this runs on the thread that draws. The flow asks a
        // busy file again, a second at a time, which is right for a collector that already has a
        // screen up and is three frozen seconds here. A file too damaged to read is replaced on
        // this thread too — one write, once, on a file that is already broken.
        val chosen = runBlocking { themes.chosen() }
        dress(chosen.isDark(resources.configuration.isNight))

        setContent {
            val choice by themes.choice.collectAsState(initial = chosen)
            // No answer yet is the phone's answer, and only a composition can ask it.
            val dark = choice.isDark(isSystemInDarkTheme())

            // Said again on every change, because the player can flip the palette from Setup
            // without the activity being created again.
            SideEffect { dress(dark) }

            PuzzleTheme(darkTheme = dark) {
                PuzzleNavHost(puzzles = puzzles)
            }
        }
    }

    /**
     * Everything the player's choice decides that Compose does not draw: the window behind the
     * app, and the two bars the system draws over it.
     *
     * The window background is what shows in the gap between the launch screen and the first
     * composed frame, and behind the transparent bars afterwards. `enableEdgeToEdge` asks to draw
     * behind those bars and settles what is drawn on them — the icon colour on every version, and
     * below API 29 the scrim behind the navigation bar as well. Left to itself it answers from the
     * phone's own mode, so the player's answer is handed to it instead; otherwise a dark app on a
     * light phone gets dark icons on a dark strip, and neither is visible.
     */
    private fun dress(dark: Boolean) {
        window.setBackgroundDrawable(ColorDrawable((if (dark) DarkBackground else LightBackground).toArgb()))
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
            navigationBarStyle = SystemBarStyle.auto(LightScrim, DarkScrim) { dark },
        )
    }
}

/** The phone's own answer, for the callers that cannot ask a composition for it. */
private val Configuration.isNight: Boolean
    get() = uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

private fun ThemeChoice?.isDark(night: Boolean): Boolean = this?.let { it == ThemeChoice.DARK } ?: night
