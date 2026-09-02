package com.mdimitrov.puzzles.scores.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mdimitrov.puzzles.scores.domain.Solve
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
    fun `every control on the scores screen is big enough to hit, and so is its dialog`() {
        showing {
            ScoresContent(
                state = ScoresUiState(groupsOf(listOf(Solve(8, "queens", 84, 0L, 1))), answered = true),
                actions =
                    ScoresActions(
                        onDelete = {},
                        onClearAll = {},
                        onBack = {},
                        onNewGame = {},
                    ),
            )
        }

        assertEquals(emptyList(), tooSmall())

        compose.onNodeWithText("Clear all").performClick()
        assertEquals(emptyList(), tooSmall())
    }

    private fun showing(content: @Composable () -> Unit) {
        compose.setContent { PuzzleTheme { content() } }
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

                if (short) "$name is ${width}x$height dp" else null
            }
}
