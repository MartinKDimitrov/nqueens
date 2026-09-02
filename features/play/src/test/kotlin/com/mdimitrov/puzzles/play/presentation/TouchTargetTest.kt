package com.mdimitrov.puzzles.play.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import com.mdimitrov.puzzles.boardlogic.Cell
import com.mdimitrov.puzzles.boardlogic.GameState
import com.mdimitrov.puzzles.boardlogic.snapshotOf
import com.mdimitrov.puzzles.nqueens.NQueensLines
import com.mdimitrov.puzzles.nqueens.Queens
import com.mdimitrov.puzzles.theme.PuzzleTheme
import com.mdimitrov.puzzles.theme.TouchTarget
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertEquals

private const val DENSITY = 2.625f
private const val TOLERANCE = 0.5f
private const val A_BOARD = 8
private const val A_SOLVED_BOARD = 4
private const val WINNING_TIME = 84
private const val A_FEW_SECONDS = 10

/** What every square's name begins with: "Row 3, column 5, empty". */
private const val SQUARE_PREFIX = "Row "

/** The four squares that solve a four-board, so the win card is on screen with its buttons. */
private val solvingFour = setOf(Cell(0, 1), Cell(1, 3), Cell(2, 0), Cell(3, 2))

/**
 * Every control on this screen is at least [TouchTarget] to a finger. The rule is claimed in
 * `docs/PROJECT.md` §2 and each module checks its own screens, by walking them rather than by
 * naming controls one at a time.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TouchTargetTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `every control on a solved board is big enough to hit`() {
        val solved = snapshotOf(GameState(A_SOLVED_BOARD, solvingFour), NQueensLines, target = A_SOLVED_BOARD)

        showing {
            PlayContent(
                state = PlayUiState(solved, Queens, WINNING_TIME),
                actions = PlayActions({}, {}, {}, {}),
            )
        }

        assertEquals(emptyList(), tooSmall())
    }

    @Test
    fun `every control around a board being played is big enough to hit`() {
        val board = snapshotOf(GameState(A_BOARD, setOf(Cell(0, 0))), NQueensLines, target = A_BOARD)

        showing {
            PlayContent(
                state = PlayUiState(board, Queens, A_FEW_SECONDS),
                actions = PlayActions({}, {}, {}, {}),
            )
        }

        assertEquals(emptyList(), tooSmall())
    }

    private fun showing(content: @Composable () -> Unit) {
        compose.setContent { PuzzleTheme { content() } }
    }

    /**
     * What each control that falls short is called, and by how much, so a failure names itself.
     *
     * The board's own squares are not among them. `docs/PROJECT.md` §2 makes them the exception
     * and floors them at 24 dp instead, because a square held to [TouchTarget] would put a
     * 12 x 12 board off the screen; `PlayScreenTest` holds that floor. What is walked here is
     * every control around the board.
     */
    private fun tooSmall(): List<String> =
        compose
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
            .fetchSemanticsNodes()
            .mapNotNull { node ->
                val name =
                    node.config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()
                        ?: node.config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text
                        ?: "an unnamed control"
                val width = node.size.width / DENSITY
                val height = node.size.height / DENSITY
                val short = width < TouchTarget.value - TOLERANCE || height < TouchTarget.value - TOLERANCE

                if (short && !name.startsWith(SQUARE_PREFIX)) "$name is ${width}x$height dp" else null
            }
}
