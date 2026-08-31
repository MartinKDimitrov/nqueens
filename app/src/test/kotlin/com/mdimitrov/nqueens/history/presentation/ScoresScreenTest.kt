package com.mdimitrov.nqueens.history.presentation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.mdimitrov.nqueens.history.domain.Solve
import com.mdimitrov.nqueens.theme.NQueensTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.TimeZone
import kotlin.math.abs
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val DAY = 86_400_000L
private const val HOUR = 3_600_000L
private val SAME_ROW = 8.dp
private val TOUCH_TARGET = 48.dp

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
            solve(size = 8, seconds = 84, finishedAt = 0L, id = 1),
            solve(size = 8, seconds = 72, finishedAt = DAY, id = 2),
            solve(size = 4, seconds = 6, finishedAt = DAY * 2, id = 3),
        )

        compose.onNodeWithText("2 solves").assertIsDisplayed()
        compose.onNodeWithText("1 solve").assertIsDisplayed()
        compose.onNodeWithText("Jan 1, 12:00", substring = true).assertExists()

        val small = compose.onNodeWithText("4 × 4").getUnclippedBoundsInRoot()
        val large = compose.onNodeWithText("8 × 8").getUnclippedBoundsInRoot()
        assertTrue(small.top < large.top, "the smallest board is listed first")

        val fastest = compose.onNodeWithText("01:12", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val slower = compose.onNodeWithText("01:24", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(fastest.top < slower.top, "the fastest solve is listed first in its card")
    }

    @Test
    fun `a row is deleted on its own`() {
        var deleted: Long? = null
        scoresOf(solve(size = 8, seconds = 84, id = 7), onDelete = { deleted = it })

        compose
            .onNodeWithContentDescription("Delete the 01:24 solve from Jan 1, 12:00", substring = true)
            .performClick()

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
    fun `a screen whose table has not answered yet claims nothing about it`() {
        showing(ScoresUiState())

        compose.onNodeWithText("Nothing solved yet.").assertDoesNotExist()
        compose.onNodeWithText("Your records could not be read.").assertDoesNotExist()
        compose.onNodeWithText("New game").assertIsDisplayed()
    }

    @Test
    fun `a table that cannot be read says that, not that nothing was solved`() {
        showing(ScoresUiState(answered = true, readable = false))

        compose.onNodeWithText("Your records could not be read.").assertIsDisplayed()
        compose.onNodeWithText("Nothing solved yet.").assertDoesNotExist()
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

    @Test
    fun `two equally fast solves of one board are told apart by when they were finished`() {
        var deleted: Long? = null
        scoresOf(
            solve(size = 4, seconds = 6, finishedAt = 0L, id = 1),
            solve(size = 4, seconds = 6, finishedAt = HOUR, id = 2),
            onDelete = { deleted = it },
        )

        compose.onNodeWithContentDescription("Delete the 00:06 solve from Jan 1, 1:00", substring = true).performClick()

        assertEquals(2L, deleted, "each label deletes the row it belongs to")
    }

    @Test
    fun `a label keeps naming its own row when a neighbour is deleted`() {
        var deleted: Long? = null
        scoresOf(
            solve(size = 4, seconds = 10, finishedAt = 0L, id = 1),
            solve(size = 4, seconds = 20, finishedAt = HOUR, id = 2),
            onDelete = { deleted = it },
        )
        val label = "Delete the 00:20 solve from Jan 1, 1:00"

        // Its rank is 2 here and would be 1 once the faster row goes; the label does not move.
        compose.onNodeWithContentDescription(label, substring = true).performClick()

        assertEquals(2L, deleted)
    }

    @Test
    fun `the way off the screen is on it however long the table is`() {
        val many =
            (4..12).flatMap { size ->
                (1..5).map { solve(size = size, seconds = it * 10, id = (size * 10 + it).toLong()) }
            }
        scoresOf(*many.toTypedArray())

        compose.onNodeWithText("New game").assertIsDisplayed()
    }

    @Test
    fun `the fastest row wears the first rank`() {
        scoresOf(
            solve(size = 8, seconds = 84, finishedAt = 0L, id = 1),
            solve(size = 8, seconds = 72, finishedAt = DAY, id = 2),
        )

        assertTrue(sameRow("1", "01:12"), "rank 1 belongs to the fastest row")
        assertTrue(sameRow("2", "01:24"), "rank 2 belongs to the one after it")
    }

    @Test
    fun `every solve of a board is listed, and every one can be deleted`() {
        val many = (1..8).map { solve(size = 4, seconds = it * 10, finishedAt = it * DAY, id = it.toLong()) }
        scoresOf(*many.toTypedArray())

        compose.onNodeWithText("8 solves").assertIsDisplayed()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("01:20"))
        compose.onNodeWithText("01:20").assertIsDisplayed()
        assertEquals(8, compose.onAllNodesWithContentDescription("Delete", substring = true).fetchSemanticsNodes().size)
    }

    @Test
    @Config(qualifiers = "w891dp-h240dp-420dpi")
    fun `on a window too short for the list the screen scrolls rather than hide it`() {
        scoresOf(solve(size = 8, seconds = 84, finishedAt = 0L))

        compose.onNode(hasScrollAction()).performScrollToNode(hasText("01:24"))
        compose.onNodeWithText("01:24").assertIsDisplayed()
        compose.onNodeWithText("New game").assertIsDisplayed()
    }

    /** Two labels drawn in the same row of a card share a middle, whatever their type sizes. */
    private fun sameRow(
        one: String,
        other: String,
    ): Boolean {
        val a = compose.onNodeWithText(one, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val b = compose.onNodeWithText(other, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val apart = ((a.top + a.bottom) / 2 - (b.top + b.bottom) / 2).value

        return abs(apart) < SAME_ROW.value
    }

    private fun solve(
        size: Int = 8,
        seconds: Int = 60,
        finishedAt: Long = 0L,
        id: Long = 1,
    ) = Solve(
        size = size,
        variant = "queens",
        seconds = seconds,
        finishedAt = finishedAt,
        id = id,
    )

    @Test
    @Config(qualifiers = "w320dp-h568dp-420dpi")
    fun `at the largest font the solve time is still drawn`() {
        compose.setContent {
            NQueensTheme {
                CompositionLocalProvider(
                    LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f),
                ) {
                    ScoresContent(
                        state =
                            ScoresUiState(
                                groups = groupsOf(listOf(solve(size = 4, seconds = 84, id = 1))),
                                answered = true,
                            ),
                        onDelete = {},
                        onClearAll = {},
                        onNewGame = {},
                    )
                }
            }
        }

        val time = compose.onNodeWithText("01:24").getUnclippedBoundsInRoot()
        assertTrue(time.right > time.left, "the solve time was squeezed out of its own row")
    }

    @Test
    fun `clearing everything stays within reach however long the table is`() {
        val many =
            (4..12).flatMap { size ->
                (1..5).map { solve(size = size, seconds = it * 10, id = (size * 10 + it).toLong()) }
            }
        scoresOf(*many.toTypedArray())

        compose.onNode(hasScrollAction()).performScrollToNode(hasText("12 × 12"))

        compose.onNodeWithText("Clear all").assertIsDisplayed()
    }

    @Test
    fun `two solves of one board a few seconds apart still get their own labels`() {
        scoresOf(
            solve(size = 4, seconds = 6, finishedAt = 10_000L, id = 1),
            solve(size = 4, seconds = 6, finishedAt = 30_000L, id = 2),
        )

        val labels =
            compose
                .onAllNodesWithContentDescription("Delete", substring = true)
                .fetchSemanticsNodes()
                .map { it.config[SemanticsProperties.ContentDescription].first() }

        assertEquals(2, labels.toSet().size, "both buttons read the same: $labels")
    }

    @Test
    fun `a row is spoken as one item, and its delete button is the size a finger needs`() {
        scoresOf(solve(size = 8, seconds = 84, finishedAt = 0L, id = 1))

        compose.onNodeWithContentDescription("Rank 1, 01:24, Jan 1", substring = true).assertExists()
        compose
            .onNodeWithContentDescription("Delete", substring = true)
            .assertWidthIsEqualTo(TOUCH_TARGET)
            .assertHeightIsEqualTo(TOUCH_TARGET)

        val row =
            compose
                .onNodeWithContentDescription("Rank 1, 01:24, Jan 1", substring = true)
                .fetchSemanticsNode()
        val time = compose.onNodeWithText("01:24").fetchSemanticsNode()

        assertEquals(row.id, time.id, "the time is read as an item of its own, not as part of the row")
    }

    @Test
    fun `the title is a heading, so a screen reader can jump past it to the list`() {
        scoresOf(solve(size = 8, seconds = 84, id = 1))

        compose.onNodeWithText("Best times").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    private fun showing(state: ScoresUiState) {
        compose.setContent {
            NQueensTheme {
                ScoresContent(state = state, onDelete = {}, onClearAll = {}, onNewGame = {})
            }
        }
    }

    private fun scoresOf(
        vararg solves: Solve,
        onDelete: (Long) -> Unit = {},
        onClearAll: () -> Unit = {},
        onNewGame: () -> Unit = {},
    ) {
        compose.setContent {
            NQueensTheme {
                ScoresContent(
                    state = ScoresUiState(groups = groupsOf(solves.toList()), answered = true),
                    onDelete = onDelete,
                    onClearAll = onClearAll,
                    onNewGame = onNewGame,
                )
            }
        }
    }
}
