package com.mdimitrov.nqueens.history.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mdimitrov.nqueens.R
import com.mdimitrov.nqueens.history.domain.Solve
import com.mdimitrov.nqueens.theme.NQueensTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val DAY = 86_400_000L

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScoresScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @BeforeTest
    fun readEveryDateInOneTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @AfterTest
    fun giveTheTimeZoneBack() {
        TimeZone.setDefault(null)
    }

    @Test
    fun `the boards are listed by size, the fastest first, with the day each was solved`() {
        scoresOf(
            solve(size = 8, seconds = 84, finishedAt = 0L),
            solve(size = 8, seconds = 72, finishedAt = DAY),
            solve(size = 4, seconds = 6, finishedAt = DAY * 2),
        )

        compose.onNodeWithText("2 solves").assertIsDisplayed()
        compose.onNodeWithText("1 solve").assertIsDisplayed()
        compose.onNodeWithText("Jan 1").assertIsDisplayed()

        val small = compose.onNodeWithText("4 × 4").getUnclippedBoundsInRoot()
        val large = compose.onNodeWithText("8 × 8").getUnclippedBoundsInRoot()
        assertTrue(small.top < large.top, "the smallest board is listed first")

        val fastest = compose.onNodeWithText("01:12").getUnclippedBoundsInRoot()
        val slower = compose.onNodeWithText("01:24").getUnclippedBoundsInRoot()
        assertTrue(fastest.top < slower.top, "the fastest solve is listed first in its card")
    }

    @Test
    fun `a row is deleted on its own`() {
        var deleted: Long? = null
        scoresOf(solve(size = 8, seconds = 84, id = 7), onDelete = { deleted = it })

        compose.onNodeWithContentDescription("Delete the 01:24 solve on 8 × 8").performClick()

        assertEquals(7L, deleted)
    }

    @Test
    fun `clearing everything is asked about first, and cancelling clears nothing`() {
        var cleared = false
        scoresOf(solve(seconds = 84), onClearAll = { cleared = true })

        compose.onNodeWithText("Clear all").performClick()
        compose.onNodeWithText("Clear all scores?").assertIsDisplayed()
        assertFalse(cleared)

        compose.onNodeWithText("Cancel").performClick()
        assertFalse(cleared)

        compose.onNodeWithText("Clear all").performClick()
        compose.onNodeWithText("Clear").performClick()
        assertTrue(cleared)
    }

    @Test
    fun `with nothing solved the screen says so and offers nothing to clear`() {
        scoresOf()

        compose.onNodeWithText("Nothing solved yet.").assertIsDisplayed()
        compose.onNodeWithText("Clear all").assertDoesNotExist()
        compose.onNodeWithText("New game").assertIsDisplayed()
    }

    @Test
    fun `a new game leaves the list`() {
        var left = false
        scoresOf(solve(seconds = 84), onNewGame = { left = true })

        compose.onNodeWithText("New game").performClick()

        assertTrue(left)
    }

    private fun solve(
        size: Int = 8,
        seconds: Int = 60,
        finishedAt: Long = 0L,
        id: Long = 1,
    ) = Solve(
        size = size,
        variant = R.string.variant_queens,
        seconds = seconds,
        finishedAt = finishedAt,
        id = id,
    )

    private fun scoresOf(
        vararg solves: Solve,
        onDelete: (Long) -> Unit = {},
        onClearAll: () -> Unit = {},
        onNewGame: () -> Unit = {},
    ) {
        compose.setContent {
            NQueensTheme {
                ScoresContent(
                    state = ScoresUiState(groupsOf(solves.toList())),
                    onDelete = onDelete,
                    onClearAll = onClearAll,
                    onNewGame = onNewGame,
                )
            }
        }
    }
}
