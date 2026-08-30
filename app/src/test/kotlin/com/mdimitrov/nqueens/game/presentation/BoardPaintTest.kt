package com.mdimitrov.nqueens.game.presentation

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.mdimitrov.nqueens.domain.Cell
import com.mdimitrov.nqueens.domain.GameState
import com.mdimitrov.nqueens.domain.NQueensLines
import com.mdimitrov.nqueens.domain.snapshotOf
import com.mdimitrov.nqueens.puzzle.Queens
import com.mdimitrov.nqueens.theme.DarkBoardDark
import com.mdimitrov.nqueens.theme.DarkBoardLight
import com.mdimitrov.nqueens.theme.LightBoardDark
import com.mdimitrov.nqueens.theme.LightBoardLight
import com.mdimitrov.nqueens.theme.LightConflict
import com.mdimitrov.nqueens.theme.LightConflictGlow
import com.mdimitrov.nqueens.theme.LightQueen
import com.mdimitrov.nqueens.theme.NQueensTheme
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
            NQueensTheme(darkTheme = dark) {
                GameContent(
                    state =
                        GameUiState(
                            board = snapshotOf(GameState(size = 4, pieces = queens.toSet()), NQueensLines),
                            variant = Queens,
                            elapsedSeconds = 0,
                        ),
                    onTap = {},
                    onReset = {},
                    onBack = {},
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
