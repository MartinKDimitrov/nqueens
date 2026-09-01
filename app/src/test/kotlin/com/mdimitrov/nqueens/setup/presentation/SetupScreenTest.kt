package com.mdimitrov.nqueens.setup.presentation

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.mdimitrov.nqueens.R
import com.mdimitrov.nqueens.domain.MIN_BOARD_SIZE
import com.mdimitrov.nqueens.puzzle.LARGEST_PLAYABLE_BOARD
import com.mdimitrov.nqueens.puzzle.Queens
import com.mdimitrov.nqueens.setup.domain.DEFAULT_BOARD_SIZE
import com.mdimitrov.nqueens.theme.NQueensTheme
import com.mdimitrov.nqueens.theme.TouchTarget
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
        setupAt(MIN_BOARD_SIZE, onGrow = { grew = true }, onShrink = { shrank = true })

        compose.onNodeWithText("4 × 4").assertExists()

        compose.onNodeWithContentDescription("Larger board").performClick()
        assertTrue(grew, "growing the board")
        assertFalse(shrank, "shrinking the board")
    }

    @Test
    fun `the smallest board cannot be made smaller`() {
        setupAt(MIN_BOARD_SIZE)

        compose.onNodeWithContentDescription("Smaller board").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Larger board").assertHasClickAction()
    }

    @Test
    fun `the largest board cannot be made larger`() {
        setupAt(LARGEST_PLAYABLE_BOARD)

        compose.onNodeWithContentDescription("Larger board").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Smaller board").assertHasClickAction()
    }

    @Test
    fun `starting carries the size that is on screen`() {
        var started: Int? = null
        setupAt(size = 5, onStart = { started = it })

        compose.onNodeWithText("Start").performClick()

        assertEquals(5, started)
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-420dpi")
    fun `on a screen wider than it is tall, Start can still be reached`() {
        setupAt(LARGEST_PLAYABLE_BOARD)

        compose.onNodeWithText("Start").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `the preview says which board it is showing`() {
        setupAt(size = LARGEST_PLAYABLE_BOARD)

        compose.onNodeWithContentDescription(
            "Board preview, $LARGEST_PLAYABLE_BOARD by $LARGEST_PLAYABLE_BOARD",
        ).assertExists()
    }

    @Test
    fun `the screen speaks the words the variant gives it`() {
        val other = Queens.copy(text = Queens.text.copy(subtitle = R.string.game_back))
        compose.setContent {
            NQueensTheme {
                SetupContent(
                    state = SetupUiState(DEFAULT_BOARD_SIZE, other),
                    actions =
                        SetupActions(
                            onShrink = {},
                            onGrow = {},
                            onStart = {},
                            onScores = {},
                        ),
                )
            }
        }

        compose.onNodeWithText("Back to setup").assertExists()
    }

    @Test
    @Config(qualifiers = "w280dp-h891dp-420dpi", fontScale = 2f)
    fun `the board can still be grown when the type and the display are at their largest`() {
        var grown = 0
        setupAt(size = DEFAULT_BOARD_SIZE, onGrow = { grown++ })

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
        setupAt(size = LARGEST_PLAYABLE_BOARD)

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

    private fun setupAt(
        size: Int,
        onShrink: () -> Unit = {},
        onGrow: () -> Unit = {},
        onStart: (Int) -> Unit = {},
    ) {
        compose.setContent {
            NQueensTheme {
                SetupContent(
                    state = SetupUiState(size, Queens),
                    actions =
                        SetupActions(
                            onShrink = onShrink,
                            onGrow = onGrow,
                            onStart = onStart,
                            onScores = {},
                        ),
                )
            }
        }
    }
}
