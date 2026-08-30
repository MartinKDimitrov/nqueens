package com.mdimitrov.nqueens.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BoardSnapshotTest {
    @Test
    fun `an empty board is all empty squares with every queen still to place`() {
        val snapshot = snapshotOf(GameState(size = 4), NQueensLines)

        assertEquals(16, snapshot.statuses.size)
        assertTrue(snapshot.statuses.all { it == CellStatus.EMPTY })
        assertEquals(4, snapshot.piecesLeft)
        assertFalse(snapshot.isSolved)
    }

    @Test
    fun `a lone queen is marked without conflict and lowers the counter`() {
        val snapshot = snapshotOf(GameState(size = 4, pieces = setOf(Cell(1, 2))), NQueensLines)

        assertEquals(CellStatus.PIECE, snapshot.statusAt(row = 1, col = 2))
        assertEquals(3, snapshot.piecesLeft)
        assertEquals(1, snapshot.statuses.count { it != CellStatus.EMPTY })
    }

    @Test
    fun `row and column are not transposed`() {
        val snapshot = snapshotOf(GameState(size = 4, pieces = setOf(Cell(0, 1))), NQueensLines)

        assertEquals(CellStatus.PIECE, snapshot.statusAt(row = 0, col = 1))
        assertEquals(CellStatus.EMPTY, snapshot.statusAt(row = 1, col = 0))
    }

    @Test
    fun `queens that attack each other are both marked as conflicting`() {
        val snapshot = snapshotOf(GameState(size = 4, pieces = setOf(Cell(0, 0), Cell(0, 3))), NQueensLines)

        assertEquals(CellStatus.PIECE_CONFLICT, snapshot.statusAt(row = 0, col = 0))
        assertEquals(CellStatus.PIECE_CONFLICT, snapshot.statusAt(row = 0, col = 3))
    }

    @Test
    fun `a queen out of reach of the others is not flagged as conflicting`() {
        val queens = setOf(Cell(0, 0), Cell(0, 3), Cell(3, 1))
        val snapshot = snapshotOf(GameState(size = 4, pieces = queens), NQueensLines)

        assertEquals(CellStatus.PIECE_CONFLICT, snapshot.statusAt(row = 0, col = 0))
        assertEquals(CellStatus.PIECE_CONFLICT, snapshot.statusAt(row = 0, col = 3))
        assertEquals(CellStatus.PIECE, snapshot.statusAt(row = 3, col = 1))
    }

    @Test
    fun `a solved board reports itself solved with nothing left to place`() {
        val solution = setOf(Cell(0, 1), Cell(1, 3), Cell(2, 0), Cell(3, 2))
        val snapshot = snapshotOf(GameState(size = 4, pieces = solution), NQueensLines)

        assertTrue(snapshot.isSolved)
        assertEquals(0, snapshot.piecesLeft)
        assertEquals(4, snapshot.statuses.count { it == CellStatus.PIECE })
        assertFalse(snapshot.statuses.any { it == CellStatus.PIECE_CONFLICT })
    }

    @Test
    fun `a full board that is not a solution is not solved`() {
        val diagonal = setOf(Cell(0, 0), Cell(1, 1), Cell(2, 2), Cell(3, 3))
        val snapshot = snapshotOf(GameState(size = 4, pieces = diagonal), NQueensLines)

        assertFalse(snapshot.isSolved)
        assertEquals(0, snapshot.piecesLeft)
    }

    @Test
    fun `a square off the board is refused, rather than wrapping into the next row`() {
        val snapshot = snapshotOf(GameState(size = 4), NQueensLines)

        for ((row, col) in listOf(0 to 4, 4 to 0, -1 to 0, 0 to -1)) {
            assertFailsWith<IllegalArgumentException>("row $row, col $col") {
                snapshot.statusAt(row = row, col = col)
            }
        }
    }
}
