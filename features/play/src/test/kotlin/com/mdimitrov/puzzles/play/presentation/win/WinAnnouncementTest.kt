package com.mdimitrov.puzzles.play.presentation.win

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.mdimitrov.puzzles.nqueens.Queens
import com.mdimitrov.puzzles.theme.PuzzleTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * That the win is spoken, not merely marked as speakable.
 *
 * The card carries a polite live region, and a live region is announced when a node the reader
 * already knows about changes. This card and its region are created in the same frame as the win,
 * so the reader has never seen the node and Compose sends nothing for it. Asserting the semantics
 * property — which `PlayScreenTest` does — cannot tell the two apart.
 *
 * What is held here is what the card says and when. What is not held is the last step, the window
 * passing it to the platform: that needs a reader switched on, and Compose keeps accessibility
 * machinery running for as long as one is, which no test can switch off again — two of the board's
 * animation tests never reach a quiet composition afterwards. The production fallback in
 * [LocalAnnouncer] is one expression, and it is the only part of this that a person has to read.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-420dpi")
class WinAnnouncementTest {
    @get:Rule
    val compose = createComposeRule()

    private val spoken = mutableListOf<String>()

    /**
     * Frames are driven by hand. The card runs its celebration for three and a half seconds, and
     * waiting for the composition to go quiet means waiting for that; what these tests need is the
     * frame the card arrives on, not the one it settles on.
     */
    @BeforeTest
    fun driveTheFramesByHand() {
        compose.mainClock.autoAdvance = false
    }

    /**
     * Given back, because it is not this class's to keep: left off, it is off for the tests that
     * run after these, and two of the board's animation tests then wait sixty seconds for a
     * composition that no clock is advancing.
     */
    @AfterTest
    fun letTheClockRunAgain() {
        compose.mainClock.autoAdvance = true
    }

    @Test
    fun `a solved board says the whole result out loud`() {
        showing(Win(puzzle = Queens, size = 4, seconds = 84, previousBestSeconds = null))

        assertEquals(listOf("Solved! 4 × 4 · Queens, in 01:24"), spoken)
    }

    @Test
    fun `a record beaten is said too, when the table has answered`() {
        // The real sequence, not a card built with the answer already in it: the card arrives the
        // moment the last piece lands, and what it beat comes back from the table afterwards. A
        // sighted player watches the line appear. Said only in the sentence above, it would have to
        // be said before it was known.
        var win by mutableStateOf(Win(puzzle = Queens, size = 4, seconds = 84, previousBestSeconds = null))
        showing { win }

        win = win.copy(previousBestSeconds = 114)
        drawTheFrames()

        assertEquals(
            listOf("Solved! 4 × 4 · Queens, in 01:24", "New best — 30s faster than before"),
            spoken,
        )
    }

    private fun showing(win: Win) = showing { win }

    private fun showing(win: () -> Win) {
        compose.setContent {
            CompositionLocalProvider(LocalAnnouncer provides Announcer(spoken::add)) {
                PuzzleTheme(darkTheme = false) {
                    WinCard(win = win(), onPlayAgain = {}, onScores = {})
                }
            }
        }
        drawTheFrames()
    }

    /** Composition, then the effects it launches, then the effects those launch. */
    private fun drawTheFrames() = repeat(3) { compose.mainClock.advanceTimeByFrame() }
}
