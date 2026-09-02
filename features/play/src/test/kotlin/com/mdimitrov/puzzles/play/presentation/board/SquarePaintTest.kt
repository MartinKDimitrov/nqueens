package com.mdimitrov.puzzles.play.presentation.board

import androidx.compose.ui.graphics.Color
import com.mdimitrov.puzzles.boardlogic.CellStatus
import com.mdimitrov.puzzles.theme.BoardColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SquarePaintTest {
    @Test
    fun `an empty square is painted by where it sits, and a queen does not change that`() {
        assertEquals(colors.boardLight, squareBackground(CellStatus.EMPTY, light = true, colors))
        assertEquals(colors.boardDark, squareBackground(CellStatus.EMPTY, light = false, colors))
        assertEquals(colors.boardLight, squareBackground(CellStatus.PIECE, light = true, colors))
        assertEquals(colors.boardDark, squareBackground(CellStatus.PIECE, light = false, colors))
    }

    @Test
    fun `a square under attack is marked the same way wherever it sits`() {
        assertEquals(colors.conflictGlow, squareBackground(CellStatus.PIECE_CONFLICT, light = true, colors))
        assertEquals(colors.conflictGlow, squareBackground(CellStatus.PIECE_CONFLICT, light = false, colors))
    }

    @Test
    fun `the queen is drawn only where she stands, and reddened when she is attacked`() {
        assertNull(pieceTint(CellStatus.EMPTY, colors))
        assertEquals(colors.queen, pieceTint(CellStatus.PIECE, colors))
        assertEquals(colors.conflict, pieceTint(CellStatus.PIECE_CONFLICT, colors))
    }

    private val colors =
        BoardColors(
            boardLight = Color(0xFF000001),
            boardDark = Color(0xFF000002),
            queen = Color(0xFF000003),
            conflict = Color(0xFF000004),
            conflictGlow = Color(0xFF000005),
            hint = Color(0xFF000006),
            border = Color(0xFF000007),
            surfaceAlt = Color(0xFF000008),
            onSurfaceMuted = Color(0xFF000009),
            success = Color(0xFF1F9D6B),
        )
}
