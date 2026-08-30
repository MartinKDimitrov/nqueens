package com.mdimitrov.nqueens.game.presentation

import androidx.lifecycle.SavedStateHandle
import com.mdimitrov.nqueens.domain.Cell
import com.mdimitrov.nqueens.domain.CellStatus
import com.mdimitrov.nqueens.domain.GameAction
import com.mdimitrov.nqueens.domain.Line
import com.mdimitrov.nqueens.domain.LineKind
import com.mdimitrov.nqueens.domain.LineRules
import com.mdimitrov.nqueens.puzzle.Queens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameViewModelTest {
    @Test
    fun `the board starts empty, at the size the route asked for`() {
        val viewModel = gameOf(size = 6)

        assertEquals(6, viewModel.uiState.board.size)
        assertEquals(6, viewModel.uiState.board.piecesLeft)
        assertTrue(viewModel.uiState.board.statuses.all { it == CellStatus.EMPTY })
    }

    @Test
    fun `tapping an empty square places a queen, and tapping her takes her back`() {
        val viewModel = gameOf()

        viewModel.onAction(GameAction.Toggle(Cell(1, 2)))
        assertEquals(CellStatus.PIECE, viewModel.uiState.board.statusAt(row = 1, col = 2))
        assertEquals(3, viewModel.uiState.board.piecesLeft)

        viewModel.onAction(GameAction.Toggle(Cell(1, 2)))
        assertEquals(CellStatus.EMPTY, viewModel.uiState.board.statusAt(row = 1, col = 2))
        assertEquals(4, viewModel.uiState.board.piecesLeft)
    }

    @Test
    fun `a queen under attack is placed and both are flagged`() {
        val viewModel = gameOf()

        viewModel.onAction(GameAction.Toggle(Cell(0, 0)))
        viewModel.onAction(GameAction.Toggle(Cell(0, 3)))

        assertEquals(CellStatus.PIECE_CONFLICT, viewModel.uiState.board.statusAt(row = 0, col = 0))
        assertEquals(CellStatus.PIECE_CONFLICT, viewModel.uiState.board.statusAt(row = 0, col = 3))
        assertFalse(viewModel.uiState.board.isSolved)
    }

    @Test
    fun `a solution reports the board solved with nothing left to place`() {
        val viewModel = gameOf()

        listOf(Cell(0, 1), Cell(1, 3), Cell(2, 0), Cell(3, 2))
            .forEach { viewModel.onAction(GameAction.Toggle(it)) }

        assertTrue(viewModel.uiState.board.isSolved)
        assertEquals(0, viewModel.uiState.board.piecesLeft)
        assertFalse(viewModel.uiState.board.statuses.any { it == CellStatus.PIECE_CONFLICT })
    }

    @Test
    fun `a tap outside the board changes nothing`() {
        val viewModel = gameOf()
        val before = viewModel.uiState.board

        viewModel.onAction(GameAction.Toggle(Cell(9, 9)))

        assertEquals(before, viewModel.uiState.board)
    }

    @Test
    fun `reset clears the board and gives every queen back`() {
        val viewModel = gameOf()
        viewModel.onAction(GameAction.Toggle(Cell(0, 0)))
        viewModel.onAction(GameAction.Toggle(Cell(1, 1)))

        viewModel.onAction(GameAction.Reset)

        assertEquals(4, viewModel.uiState.board.piecesLeft)
        assertTrue(viewModel.uiState.board.statuses.all { it == CellStatus.EMPTY })
    }

    @Test
    fun `the game plays by the rules it was given, not by the ones it could assume`() {
        val rowsOnly = LineRules { cell -> setOf(Line(LineKind.ROW, cell.row)) }
        val viewModel = GameViewModel(Queens.copy(rules = rowsOnly), SavedStateHandle(mapOf(SIZE_ARGUMENT to 4)))

        viewModel.onAction(GameAction.Toggle(Cell(0, 0)))
        viewModel.onAction(GameAction.Toggle(Cell(1, 0)))

        assertEquals(CellStatus.PIECE, viewModel.uiState.board.statusAt(row = 0, col = 0))
        assertEquals(CellStatus.PIECE, viewModel.uiState.board.statusAt(row = 1, col = 0))
    }

    private fun gameOf(size: Int = 4) =
        GameViewModel(
            variant = Queens,
            savedStateHandle = SavedStateHandle(mapOf(SIZE_ARGUMENT to size)),
        )
}
