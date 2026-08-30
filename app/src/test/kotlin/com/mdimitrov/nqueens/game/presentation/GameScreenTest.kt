package com.mdimitrov.nqueens.game.presentation

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.mdimitrov.nqueens.R
import com.mdimitrov.nqueens.domain.Cell
import com.mdimitrov.nqueens.domain.GameState
import com.mdimitrov.nqueens.domain.NQueensLines
import com.mdimitrov.nqueens.domain.snapshotOf
import com.mdimitrov.nqueens.puzzle.LARGEST_PLAYABLE_BOARD
import com.mdimitrov.nqueens.puzzle.Queens
import com.mdimitrov.nqueens.theme.NQueensTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val BOARD_SIZE = 4
private val MIN_SQUARE = 24.dp

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GameScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `every square says where it is and whether a queen stands there`() {
        boardWith(Cell(0, 0), Cell(0, 3))

        compose.onNodeWithContentDescription("Row 1, column 1, queen under attack").assertExists()
        compose.onNodeWithContentDescription("Row 1, column 4, queen under attack").assertExists()
        compose.onNodeWithContentDescription("Row 3, column 2, empty").assertExists()
    }

    @Test
    fun `a queen out of reach of the others is drawn as an ordinary queen`() {
        boardWith(Cell(0, 0), Cell(0, 3), Cell(3, 1))

        compose.onNodeWithContentDescription("Row 4, column 2, queen").assertExists()
    }

    @Test
    fun `tapping a square reports which one was tapped`() {
        var tapped: Cell? = null
        boardWith(onTap = { tapped = it })

        compose.onNodeWithContentDescription("Row 2, column 3, empty").assertHasClickAction()
        compose.onNodeWithContentDescription("Row 2, column 3, empty").performClick()

        assertEquals(Cell(1, 2), tapped)
    }

    @Test
    fun `the counter shows how many queens are still to place`() {
        boardWith(Cell(0, 0), Cell(1, 2))

        compose.onNodeWithText("${BOARD_SIZE - 2}").assertExists()
    }

    @Test
    fun `reset asks for the board to be cleared`() {
        var cleared = false
        boardWith(Cell(0, 0), onReset = { cleared = true })

        compose.onNodeWithContentDescription("RESET").performClick()

        assertTrue(cleared)
    }

    @Test
    fun `the back button asks to leave the game`() {
        var left = false
        boardWith(onBack = { left = true })

        compose.onNodeWithContentDescription("Back to setup").performClick()

        assertTrue(left)
    }

    @Test
    fun `a board with nothing wrong invites the next queen`() {
        boardWith(Cell(0, 0))

        compose.onNodeWithText("Tap a cell to place a queen").assertExists()
        compose.onNodeWithText("!").assertDoesNotExist()
        compose.onNodeWithText("Tap a cell to place · tap again to remove").assertExists()
    }

    @Test
    fun `the strip counts the queens that are under attack`() {
        boardWith(Cell(0, 0), Cell(0, 3), Cell(3, 1))

        compose.onNodeWithText("2 queens attack each other").assertExists()
        compose.onNodeWithText("!").assertExists()
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-420dpi")
    fun `the largest board stays playable on a wide screen`() {
        boardWith(size = LARGEST_PLAYABLE_BOARD)

        compose.onNodeWithContentDescription("Row $LARGEST_PLAYABLE_BOARD, column $LARGEST_PLAYABLE_BOARD, empty")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(MIN_SQUARE)
    }

    @Test
    fun `the board says what it is`() {
        boardWith()

        compose.onNodeWithText("$BOARD_SIZE × $BOARD_SIZE · Queens").assertExists()
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-420dpi")
    fun `on a phone the largest board is whole, hittable, and the details are under it`() {
        boardWith(size = LARGEST_PLAYABLE_BOARD)

        assertHittable(LARGEST_PLAYABLE_BOARD)
        val corner = compose.onNodeWithContentDescription(square(LARGEST_PLAYABLE_BOARD)).getUnclippedBoundsInRoot()
        val strip = compose.onNodeWithText("Tap a cell to place a queen").getUnclippedBoundsInRoot()
        assertTrue(strip.top >= corner.bottom, "the details belong under the board on a tall screen")
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-420dpi")
    fun `on a screen wider than it is tall the details sit beside the board`() {
        boardWith(size = LARGEST_PLAYABLE_BOARD)

        assertHittable(LARGEST_PLAYABLE_BOARD)
        val corner = compose.onNodeWithContentDescription(square(LARGEST_PLAYABLE_BOARD)).getUnclippedBoundsInRoot()
        val strip = compose.onNodeWithText("Tap a cell to place a queen").getUnclippedBoundsInRoot()
        assertTrue(strip.left >= corner.right, "the details belong beside the board on a wide screen")
        compose.onNodeWithText("$LARGEST_PLAYABLE_BOARD × $LARGEST_PLAYABLE_BOARD · Queens").assertExists()
    }

    @Test
    @Config(qualifiers = "w891dp-h240dp-420dpi")
    fun `on a window too short for the board the squares keep their size and the screen scrolls`() {
        boardWith(size = LARGEST_PLAYABLE_BOARD)

        compose.onNodeWithContentDescription(square(1)).assertHeightIsAtLeast(MIN_SQUARE)
        compose.onNodeWithText("Tap a cell to place a queen").performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w411dp-h445dp-420dpi")
    fun `in a split screen window the strip still has room to say what is wrong`() {
        boardWith(Cell(0, 0), Cell(0, 3), size = LARGEST_PLAYABLE_BOARD)

        compose.onNodeWithText("2 queens attack each other").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `the screen speaks the words the variant gives it`() {
        val other =
            Queens.copy(
                name = R.string.game_reset,
                text =
                    Queens.text.copy(
                        counter = R.string.game_back,
                        cell = R.string.setup_larger_board,
                        cellUnderAttack = R.string.setup_smaller_board,
                        idle = R.string.setup_start,
                    ),
            )
        compose.setContent {
            NQueensTheme {
                GameContent(
                    state = GameUiState(uiState(arrayOf(Cell(0, 0), Cell(0, 3), Cell(3, 1)), BOARD_SIZE).board, other),
                    onTap = {},
                    onReset = {},
                    onBack = {},
                )
            }
        }

        compose.onNodeWithText("$BOARD_SIZE × $BOARD_SIZE · RESET").assertExists()
        compose.onNodeWithText("Back to setup").assertExists()
        compose.onAllNodesWithContentDescription("Smaller board").assertCountEquals(2)
        compose.onNodeWithContentDescription("Larger board").assertExists()
    }

    @Test
    fun `the strip speaks the variant's words when nothing is wrong`() {
        val other = Queens.copy(text = Queens.text.copy(idle = R.string.setup_start))
        compose.setContent {
            NQueensTheme {
                GameContent(
                    state = GameUiState(uiState(arrayOf(Cell(0, 0)), BOARD_SIZE).board, other),
                    onTap = {},
                    onReset = {},
                    onBack = {},
                )
            }
        }

        compose.onNodeWithText("Start").assertExists()
    }

    private fun assertHittable(size: Int) {
        compose.onNodeWithContentDescription(square(1)).assertIsDisplayed().assertHeightIsAtLeast(MIN_SQUARE)
        compose.onNodeWithContentDescription(square(size)).assertIsDisplayed().assertHeightIsAtLeast(MIN_SQUARE)
    }

    private fun square(n: Int) = "Row $n, column $n, empty"

    private fun uiState(
        queens: Array<out Cell>,
        size: Int,
    ) = GameUiState(
        board = snapshotOf(GameState(size = size, pieces = queens.toSet()), NQueensLines),
        variant = Queens,
    )

    private fun boardWith(
        vararg queens: Cell,
        size: Int = BOARD_SIZE,
        onTap: (Cell) -> Unit = {},
        onReset: () -> Unit = {},
        onBack: () -> Unit = {},
    ) {
        compose.setContent {
            NQueensTheme {
                GameContent(
                    state = uiState(queens, size),
                    onTap = onTap,
                    onReset = onReset,
                    onBack = onBack,
                )
            }
        }
    }
}
