package com.mdimitrov.puzzles.setup.presentation

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.mdimitrov.puzzles.nqueens.Queens
import com.mdimitrov.puzzles.puzzletype.Puzzle
import com.mdimitrov.puzzles.setup.R
import com.mdimitrov.puzzles.setup.domain.DEFAULT_BOARD_SIZE
import com.mdimitrov.puzzles.theme.PuzzleTheme
import com.mdimitrov.puzzles.theme.TouchTarget
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SetupScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the chosen size is shown and the stepper reports which way it was tapped`() {
        var grew = false
        var shrank = false
        setupAt(
            Queens.sizes.first,
            actions = doingNothing(onGrow = { grew = true }, onShrink = { shrank = true }),
        )

        compose.onNodeWithText("4 × 4").assertExists()

        compose.onNodeWithContentDescription("Larger board").performClick()
        assertTrue(grew, "growing the board")
        assertFalse(shrank, "shrinking the board")
    }

    @Test
    fun `the smallest board cannot be made smaller`() {
        setupAt(Queens.sizes.first)

        compose.onNodeWithContentDescription("Smaller board").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Larger board").assertHasClickAction()
    }

    @Test
    fun `the largest board cannot be made larger`() {
        setupAt(Queens.sizes.last)

        compose.onNodeWithContentDescription("Larger board").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Smaller board").assertHasClickAction()
    }

    @Test
    fun `starting carries the size that is on screen`() {
        var started: Int? = null
        setupAt(size = 5, actions = doingNothing(onStart = { _, size -> started = size }))

        compose.onNodeWithText("Start").performClick()

        assertEquals(5, started)
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-420dpi")
    fun `on a screen wider than it is tall, Start can still be reached`() {
        setupAt(Queens.sizes.last)

        compose.onNodeWithText("Start").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `the preview says which board it is showing`() {
        setupAt(size = Queens.sizes.last)

        compose.onNodeWithContentDescription(
            "Board preview, ${Queens.sizes.last} by ${Queens.sizes.last}",
        ).assertExists()
    }

    @Test
    fun `the screen speaks the words the puzzle gives it`() {
        val other = Queens.copy(text = Queens.text.copy(subtitle = android.R.string.copy))
        compose.setContent {
            PuzzleTheme {
                SetupContent(
                    state = SetupUiState(DEFAULT_BOARD_SIZE, other),
                    actions =
                        SetupActions(
                            onShrink = {},
                            onGrow = {},
                            onChoose = {},
                            onStart = { _, _ -> },
                            onScores = {},
                            onTheme = {},
                        ),
                )
            }
        }

        compose.onNodeWithText("Copy").assertExists()
    }

    @Test
    @Config(qualifiers = "w280dp-h891dp-420dpi", fontScale = 2f)
    fun `the board can still be grown when the type and the display are at their largest`() {
        var grown = 0
        setupAt(size = DEFAULT_BOARD_SIZE, actions = doingNothing(onGrow = { grown++ }))

        compose.onNodeWithContentDescription("Larger board").performClick()

        assertEquals(1, grown)
    }

    @Test
    @Config(qualifiers = "w280dp-h891dp-420dpi")
    fun `the preview stays square on a narrow screen`() {
        setupAt(size = DEFAULT_BOARD_SIZE)

        val bounds =
            compose.onNodeWithContentDescription("Board preview, $DEFAULT_BOARD_SIZE by $DEFAULT_BOARD_SIZE")
                .getUnclippedBoundsInRoot()

        assertEquals(bounds.right - bounds.left, bounds.bottom - bounds.top)
    }

    @Test
    fun `the largest board this app offers is twelve`() {
        setupAt(size = Queens.sizes.last)

        compose.onNodeWithText("12 × 12").assertExists()
    }

    @Test
    fun `the stepper's buttons are the size a finger needs, whatever they are drawn at`() {
        setupAt(size = DEFAULT_BOARD_SIZE)

        for (button in listOf("Smaller board", "Larger board")) {
            compose
                .onNodeWithContentDescription(button)
                .assertWidthIsEqualTo(TouchTarget)
                .assertHeightIsEqualTo(TouchTarget)
        }
    }

    /** The screen with one puzzle installed unless a test says otherwise. */
    private fun setupAt(
        size: Int,
        puzzle: Puzzle = Queens,
        installed: List<Puzzle> = listOf(puzzle),
        actions: SetupActions = doingNothing(),
    ) {
        compose.setContent {
            PuzzleTheme {
                SetupContent(state = SetupUiState(size, puzzle, installed), actions = actions)
            }
        }
    }

    @Test
    fun `the puzzle row is a choice only when there is one to make`() {
        setupAt(size = DEFAULT_BOARD_SIZE)

        // The row is still the puzzle's own line on the screen; what it is not is a choice.
        val rows = compose.onAllNodes(isSelectable())
        rows.assertCountEquals(1)
        rows[0].assertIsNotEnabled()
    }

    private fun doingNothing(
        onShrink: () -> Unit = {},
        onGrow: () -> Unit = {},
        onChoose: (Puzzle) -> Unit = {},
        onStart: (Puzzle, Int) -> Unit = { _, _ -> },
    ) = SetupActions(
        onShrink = onShrink,
        onGrow = onGrow,
        onChoose = onChoose,
        onStart = onStart,
        onScores = {},
        onTheme = {},
    )
}
