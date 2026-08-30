package com.mdimitrov.nqueens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mdimitrov.nqueens.domain.MIN_BOARD_SIZE
import com.mdimitrov.nqueens.setup.domain.DEFAULT_BOARD_SIZE
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
        repeat(DEFAULT_BOARD_SIZE - MIN_BOARD_SIZE) {
            compose.onNodeWithContentDescription("Smaller board").performClick()
        }
        start()

        compose.onNodeWithContentDescription(emptySquare(MIN_BOARD_SIZE)).assertIsDisplayed()
        compose.onNodeWithContentDescription(emptySquare(MIN_BOARD_SIZE + 1)).assertDoesNotExist()
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

    private fun start() {
        compose.onNodeWithText("Start").performClick()
        compose.waitForIdle()
    }

    private fun emptySquare(n: Int) = "Row $n, column $n, empty"
}
