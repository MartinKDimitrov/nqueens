package com.mdimitrov.nqueens.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The diagonal identifiers are the arithmetic that makes conflict counting cheap. */
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
