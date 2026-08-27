package com.mdimitrov.nqueens.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BoardSnapshotTest {
    @Test
    fun `an empty board is all empty squares with every queen still to place`() {
        val snapshot = snapshotOf(GameState(size = 4))

        assertEquals(16, snapshot.statuses.size)
        assertTrue(snapshot.statuses.all { it == CellStatus.EMPTY })
        assertEquals(4, snapshot.queensLeft)
        assertFalse(snapshot.isSolved)
    }

    @Test
    fun `a lone queen is marked without conflict and lowers the counter`() {
        val snapshot = snapshotOf(GameState(size = 4, queens = setOf(Cell(1, 2))))

        assertEquals(CellStatus.QUEEN, snapshot.statusAt(row = 1, col = 2))
        assertEquals(3, snapshot.queensLeft)
        assertEquals(1, snapshot.statuses.count { it != CellStatus.EMPTY })
    }

    @Test
    fun `row and column are not transposed`() {
        val snapshot = snapshotOf(GameState(size = 4, queens = setOf(Cell(0, 1))))

        assertEquals(CellStatus.QUEEN, snapshot.statusAt(row = 0, col = 1))
        assertEquals(CellStatus.EMPTY, snapshot.statusAt(row = 1, col = 0))
    }

    @Test
    fun `queens that attack each other are both marked as conflicting`() {
        val snapshot = snapshotOf(GameState(size = 4, queens = setOf(Cell(0, 0), Cell(0, 3))))

        assertEquals(CellStatus.QUEEN_CONFLICT, snapshot.statusAt(row = 0, col = 0))
        assertEquals(CellStatus.QUEEN_CONFLICT, snapshot.statusAt(row = 0, col = 3))
    }

    @Test
    fun `a queen out of reach of the others stays unmarked`() {
        val queens = setOf(Cell(0, 0), Cell(0, 3), Cell(3, 1))
        val snapshot = snapshotOf(GameState(size = 4, queens = queens))

        assertEquals(CellStatus.QUEEN_CONFLICT, snapshot.statusAt(row = 0, col = 0))
        assertEquals(CellStatus.QUEEN_CONFLICT, snapshot.statusAt(row = 0, col = 3))
        assertEquals(CellStatus.QUEEN, snapshot.statusAt(row = 3, col = 1))
    }

    @Test
    fun `a solved board reports itself solved with nothing left to place`() {
        val solution = setOf(Cell(0, 1), Cell(1, 3), Cell(2, 0), Cell(3, 2))
        val snapshot = snapshotOf(GameState(size = 4, queens = solution))

        assertTrue(snapshot.isSolved)
        assertEquals(0, snapshot.queensLeft)
        assertEquals(4, snapshot.statuses.count { it == CellStatus.QUEEN })
        assertFalse(snapshot.statuses.any { it == CellStatus.QUEEN_CONFLICT })
    }

    @Test
    fun `a full board that is not a solution is not solved`() {
        val diagonal = setOf(Cell(0, 0), Cell(1, 1), Cell(2, 2), Cell(3, 3))
        val snapshot = snapshotOf(GameState(size = 4, queens = diagonal))

        assertFalse(snapshot.isSolved)
        assertEquals(0, snapshot.queensLeft)
    }
}
