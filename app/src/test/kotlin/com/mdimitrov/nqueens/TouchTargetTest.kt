package com.mdimitrov.nqueens

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mdimitrov.nqueens.domain.Cell
import com.mdimitrov.nqueens.domain.GameState
import com.mdimitrov.nqueens.domain.NQueensLines
import com.mdimitrov.nqueens.domain.snapshotOf
import com.mdimitrov.nqueens.game.presentation.GameActions
import com.mdimitrov.nqueens.game.presentation.GameContent
import com.mdimitrov.nqueens.game.presentation.GameUiState
import com.mdimitrov.nqueens.history.domain.Solve
import com.mdimitrov.nqueens.history.presentation.ScoresContent
import com.mdimitrov.nqueens.history.presentation.ScoresUiState
import com.mdimitrov.nqueens.history.presentation.groupsOf
import com.mdimitrov.nqueens.puzzle.Queens
import com.mdimitrov.nqueens.setup.presentation.SetupActions
import com.mdimitrov.nqueens.setup.presentation.SetupContent
import com.mdimitrov.nqueens.setup.presentation.SetupUiState
import com.mdimitrov.nqueens.theme.NQueensTheme
import com.mdimitrov.nqueens.theme.TouchTarget
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertEquals

private const val DENSITY = 2.625f
private const val TOLERANCE = 0.5f

// A board square says where it is, and it is the one control that may be smaller than the target:
// keeping it would put a twelve by twelve board off the screen, so it floors at MinSquare and the
// screen scrolls instead.
private const val A_SQUARE = "Row "

/**
 * Every control the player taps, except a board square, is at least [TouchTarget] to a finger.
 * The rule is claimed in `docs/PROJECT.md` §2; this is what makes the claim checkable, and it
 * covers the controls no test names one by one.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TouchTargetTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `every control on the setup screen is big enough to hit`() {
        showing { SetupContent(SetupUiState(8, Queens), SetupActions({}, {}, {}, {})) }

        assertEquals(emptyList(), tooSmall())
    }

    @Test
    fun `every control on the records screen is big enough to hit, and so is its dialog`() {
        showing {
            ScoresContent(
                state = ScoresUiState(groupsOf(listOf(Solve(8, "queens", 84, 0L, 1))), answered = true),
                onDelete = {},
                onClearAll = {},
                onNewGame = {},
            )
        }

        assertEquals(emptyList(), tooSmall())

        compose.onNodeWithText("Clear all").performClick()
        assertEquals(emptyList(), tooSmall())
    }

    @Test
    fun `every control on a solved board is big enough to hit`() {
        val solved = snapshotOf(GameState(4, setOf(Cell(0, 1), Cell(1, 3), Cell(2, 0), Cell(3, 2))), NQueensLines)
        showing { GameContent(GameUiState(solved, Queens, 84), GameActions({}, {}, {}, {})) }

        assertEquals(emptyList(), tooSmall())
    }

    @Test
    fun `every control around a board being played is big enough to hit`() {
        val board = snapshotOf(GameState(8, setOf(Cell(0, 0))), NQueensLines)
        showing { GameContent(GameUiState(board, Queens, 10), GameActions({}, {}, {}, {})) }

        assertEquals(emptyList(), tooSmall())
    }

    private fun showing(content: @Composable () -> Unit) {
        compose.setContent { NQueensTheme { content() } }
    }

    /** What each control that falls short is called, and by how much, so a failure names itself. */
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

                if (short && !name.startsWith(A_SQUARE)) "$name is ${width}x$height dp" else null
            }
}
