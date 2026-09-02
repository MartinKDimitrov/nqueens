package com.mdimitrov.puzzles

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mdimitrov.puzzles.nqueens.Queens
import com.mdimitrov.puzzles.setup.domain.DEFAULT_BOARD_SIZE
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-420dpi")
class MainActivityTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun `starting a game opens a board at the size Setup chose`() {
        start()

        compose.onNodeWithContentDescription(emptySquare(DEFAULT_BOARD_SIZE)).assertIsDisplayed()
        compose.onNodeWithContentDescription(emptySquare(DEFAULT_BOARD_SIZE + 1)).assertDoesNotExist()
    }

    @Test
    fun `a smaller board carries its own size across`() {
        repeat(DEFAULT_BOARD_SIZE - Queens.sizes.first) {
            compose.onNodeWithContentDescription("Smaller board").performClick()
        }
        start()

        compose.onNodeWithContentDescription(emptySquare(Queens.sizes.first)).assertIsDisplayed()
        compose.onNodeWithContentDescription(emptySquare(Queens.sizes.first + 1)).assertDoesNotExist()
    }

    @Test
    fun `the board the route opened takes a queen`() {
        start()

        compose.onNodeWithContentDescription("Row 1, column 1, empty").performClick()

        compose.onNodeWithContentDescription("Row 1, column 1, queen").assertIsDisplayed()
    }

    @Test
    fun `the rules the game is given are the ones that mark an attack`() {
        start()

        compose.onNodeWithContentDescription("Row 1, column 1, empty").performClick()
        compose.onNodeWithContentDescription("Row 2, column 2, empty").performClick()
        compose.onNodeWithContentDescription("Row 4, column 3, empty").performClick()

        compose.onNodeWithContentDescription("Row 1, column 1, queen under attack").assertIsDisplayed()
        compose.onNodeWithContentDescription("Row 2, column 2, queen under attack").assertIsDisplayed()
        compose.onNodeWithContentDescription("Row 4, column 3, queen").assertIsDisplayed()
    }

    @Test
    fun `reset gives the board back`() {
        start()
        compose.onNodeWithContentDescription("Row 1, column 1, empty").performClick()
        compose.onNodeWithContentDescription("Row 1, column 1, queen").assertIsDisplayed()

        compose.onNodeWithContentDescription("RESET").performClick()

        compose.onNodeWithContentDescription("Row 1, column 1, empty").assertIsDisplayed()
    }

    @Test
    fun `back leaves the game and Setup is there again`() {
        start()

        compose.onNodeWithContentDescription("Back to setup").performClick()

        compose.onNodeWithText("Start").assertIsDisplayed()
    }

    @Test
    fun `pressing back twice does not empty the screen`() {
        start()

        val back = compose.onNodeWithContentDescription("Back to setup")
        compose.mainClock.autoAdvance = false
        back.performClick()
        back.performClick()
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()

        compose.onNodeWithText("Start").assertIsDisplayed()
    }

    @Test
    fun `the board the player sized is still there after a new game from the records`() {
        // Two ways off the records screen, and they used to disagree: the system's own gesture
        // came back to the board the player had chosen, the button came back to the default. A
        // size chosen four presses at a time is not something to drop without saying so.
        repeat(2) { compose.onNodeWithContentDescription("Larger board").performClick() }
        val chosen = DEFAULT_BOARD_SIZE + 2
        compose.onNodeWithText("$chosen × $chosen").assertIsDisplayed()

        compose.onNodeWithText("Best times").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("New game").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("$chosen × $chosen").assertIsDisplayed()
    }

    @Test
    fun `back from the records returns to the board they were opened from`() {
        // The reason the button exists. "New game" always goes to Setup, which is right for a
        // player who has finished; a player who opened the list from a board is owed the board
        // back, and the only other way to it was a gesture nothing on the screen mentions.
        repeat(DEFAULT_BOARD_SIZE - 4) { compose.onNodeWithContentDescription("Smaller board").performClick() }
        start()
        listOf(1 to 2, 2 to 4, 3 to 1, 4 to 3).forEach { (row, col) ->
            compose.onNodeWithContentDescription("Row $row, column $col, empty").performClick()
        }
        compose.onNodeWithText("View scores").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Best times").assertIsDisplayed()

        compose.onNodeWithContentDescription("Back").performClick()
        compose.waitForIdle()

        compose.onNodeWithContentDescription("Row 1, column 2, queen").assertIsDisplayed()
    }

    private fun start() {
        compose.onNodeWithText("Start").performClick()
        compose.waitForIdle()
    }

    private fun emptySquare(n: Int) = "Row $n, column $n, empty"
}
