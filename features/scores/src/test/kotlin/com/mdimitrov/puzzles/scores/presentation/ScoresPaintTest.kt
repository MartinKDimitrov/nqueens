package com.mdimitrov.puzzles.scores.presentation

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mdimitrov.puzzles.scores.domain.Solve
import com.mdimitrov.puzzles.theme.LightPrimary
import com.mdimitrov.puzzles.theme.LightSurfaceAlt
import com.mdimitrov.puzzles.theme.PuzzleTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertEquals

// Far enough left of the digit to be off the glyph, close enough to still be inside the badge.
private const val INSIDE_THE_BADGE = 8

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-420dpi")
class ScoresPaintTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `the fastest solve of a board wears the leader's badge and the others do not`() {
        show()

        assertEquals(LightPrimary, badgeBehind("1"))
        assertEquals(LightSurfaceAlt, badgeBehind("2"))
        assertEquals(LightSurfaceAlt, badgeBehind("3"))
    }

    private fun show() {
        val solves =
            listOf(84, 72, 96).mapIndexed { index, seconds ->
                Solve(size = 8, puzzle = "queens", seconds = seconds, finishedAt = index * 1_000L, id = index + 1L)
            }
        compose.setContent {
            PuzzleTheme {
                ScoresContent(
                    state = ScoresUiState(groupsOf(solves)),
                    actions =
                        ScoresActions(
                            onDelete = {},
                            onClearAll = {},
                            onBack = {},
                            onNewGame = {},
                        ),
                )
            }
        }
    }

    private fun badgeBehind(rank: String): Color {
        val view = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))

        val digit = compose.onNodeWithText(rank, useUnmergedTree = true).fetchSemanticsNode().boundsInWindow
        return Color(bitmap.getPixel((digit.left - INSIDE_THE_BADGE).toInt(), digit.center.y.toInt()))
    }
}
