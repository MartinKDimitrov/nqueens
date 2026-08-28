package com.mdimitrov.nqueens.setup.presentation

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mdimitrov.nqueens.LARGEST_PLAYABLE_BOARD
import com.mdimitrov.nqueens.domain.MIN_BOARD_SIZE
import com.mdimitrov.nqueens.theme.NQueensTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-420dpi")
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
        setupAt(size = 8, onStart = { started = it })

        compose.onNodeWithText("Start").performClick()

        assertEquals(8, started)
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
                    state = SetupUiState(size),
                    onShrink = onShrink,
                    onGrow = onGrow,
                    onStart = onStart,
                )
            }
        }
    }
}
