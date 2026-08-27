package com.mdimitrov.nqueens.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NQueensRulesTest {
    @Test
    fun `queens on the same row attack`() {
        assertTrue(NQueens.attacks(Cell(3, 1), Cell(3, 6)))
    }

    @Test
    fun `queens on the same column attack`() {
        assertTrue(NQueens.attacks(Cell(1, 4), Cell(7, 4)))
    }

    @Test
    fun `queens on the same descending diagonal attack`() {
        assertTrue(NQueens.attacks(Cell(2, 0), Cell(5, 3)))
    }

    @Test
    fun `queens on the same ascending diagonal attack`() {
        assertTrue(NQueens.attacks(Cell(2, 4), Cell(5, 1)))
    }

    @Test
    fun `queens sharing no line do not attack`() {
        assertFalse(NQueens.attacks(Cell(0, 0), Cell(1, 2)))
        assertFalse(NQueens.attacks(Cell(4, 4), Cell(6, 5)))
    }

    @Test
    fun `attacking is symmetric`() {
        val a = Cell(2, 3)
        val b = Cell(5, 6)
        val c = Cell(7, 1)
        assertEquals(NQueens.attacks(a, b), NQueens.attacks(b, a))
        assertEquals(NQueens.attacks(a, c), NQueens.attacks(c, a))
    }

    @Test
    fun `a cell attacks itself, so callers must exclude the identity pair`() {
        val cell = Cell(4, 4)
        assertTrue(NQueens.attacks(cell, cell))
    }
}

class CellTest {
    @Test
    fun `cells on one descending diagonal share row minus col`() {
        val diagonal = listOf(Cell(0, 2), Cell(1, 3), Cell(2, 4))
        assertEquals(setOf(-2), diagonal.map { it.descendingDiagonal }.toSet())
    }

    @Test
    fun `cells on one ascending diagonal share row plus col`() {
        val diagonal = listOf(Cell(0, 4), Cell(1, 3), Cell(2, 2))
        assertEquals(setOf(4), diagonal.map { it.ascendingDiagonal }.toSet())
    }

    @Test
    fun `cells are values, so they can key a set of placed queens`() {
        val queens = setOf(Cell(1, 1), Cell(1, 1), Cell(2, 2))
        assertEquals(2, queens.size)
        assertTrue(Cell(1, 1) in queens)
        assertEquals(Cell(3, 3), Cell(1, 1).copy(row = 3, col = 3))
    }
}
