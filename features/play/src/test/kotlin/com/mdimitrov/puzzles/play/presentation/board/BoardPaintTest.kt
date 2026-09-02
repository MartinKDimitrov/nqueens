package com.mdimitrov.puzzles.play.presentation.board

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.mdimitrov.puzzles.boardlogic.Cell
import com.mdimitrov.puzzles.boardlogic.GameState
import com.mdimitrov.puzzles.boardlogic.snapshotOf
import com.mdimitrov.puzzles.nqueens.NQueensLines
import com.mdimitrov.puzzles.nqueens.Queens
import com.mdimitrov.puzzles.play.presentation.PlayActions
import com.mdimitrov.puzzles.play.presentation.PlayContent
import com.mdimitrov.puzzles.play.presentation.PlayUiState
import com.mdimitrov.puzzles.theme.DarkBoardDark
import com.mdimitrov.puzzles.theme.DarkBoardLight
import com.mdimitrov.puzzles.theme.LightBoardDark
import com.mdimitrov.puzzles.theme.LightBoardLight
import com.mdimitrov.puzzles.theme.LightConflict
import com.mdimitrov.puzzles.theme.LightConflictGlow
import com.mdimitrov.puzzles.theme.LightQueen
import com.mdimitrov.puzzles.theme.PuzzleTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-420dpi")
class BoardPaintTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `the board is painted in the two board colours, alternating`() {
        show()

        assertEquals(LightBoardLight, colourAt("Row 1, column 1, empty"))
        assertEquals(LightBoardDark, colourAt("Row 1, column 2, empty"))
        assertEquals(LightBoardDark, colourAt("Row 2, column 1, empty"))
        assertEquals(LightBoardLight, colourAt("Row 2, column 2, empty"))
    }

    @Test
    fun `a queen is drawn where she stands, and the square under attack is tinted`() {
        show(Cell(0, 0), Cell(0, 3), Cell(3, 2))

        assertEquals(LightQueen, colourAt("Row 4, column 3, queen"))
        assertEquals(LightConflictGlow, colourAt("Row 1, column 1, queen under attack", corner = true))
        assertEquals(LightConflict, colourAt("Row 1, column 1, queen under attack"))
    }

    @Test
    fun `the dark palette reaches the board`() {
        show(dark = true)

        assertEquals(DarkBoardLight, colourAt("Row 1, column 1, empty"))
        assertEquals(DarkBoardDark, colourAt("Row 1, column 2, empty"))
    }

    private fun show(
        vararg queens: Cell,
        dark: Boolean = false,
    ) {
        compose.setContent {
            PuzzleTheme(darkTheme = dark) {
                PlayContent(
                    state =
                        PlayUiState(
                            board = snapshotOf(GameState(size = 4, pieces = queens.toSet()), NQueensLines, target = 4),
                            puzzle = Queens,
                            elapsedSeconds = 0,
                        ),
                    actions = PlayActions(onTap = {}, onReset = {}, onBack = {}, onScores = {}),
                )
            }
        }
    }

    private fun colourAt(
        description: String,
        corner: Boolean = false,
    ): Color {
        val view = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))

        val bounds = compose.onNodeWithContentDescription(description).fetchSemanticsNode().boundsInWindow
        val x = if (corner) bounds.left + 2 else bounds.center.x
        val y = if (corner) bounds.top + 2 else bounds.center.y
        return Color(bitmap.getPixel(x.toInt(), y.toInt()))
    }
}
