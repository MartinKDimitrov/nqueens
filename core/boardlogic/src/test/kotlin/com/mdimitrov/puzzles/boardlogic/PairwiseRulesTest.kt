package com.mdimitrov.puzzles.boardlogic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PairwiseRulesTest {
    @Test
    fun `queens on the same row attack`() {
        assertTrue(FourLinesByPairs.attacks(Cell(3, 1), Cell(3, 6)))
    }

    @Test
    fun `queens on the same column attack`() {
        assertTrue(FourLinesByPairs.attacks(Cell(1, 4), Cell(7, 4)))
    }

    @Test
    fun `queens on the same descending diagonal attack`() {
        assertTrue(FourLinesByPairs.attacks(Cell(2, 0), Cell(5, 3)))
    }

    @Test
    fun `queens on the same ascending diagonal attack`() {
        assertTrue(FourLinesByPairs.attacks(Cell(2, 4), Cell(5, 1)))
    }

    @Test
    fun `queens sharing no line do not attack`() {
        assertFalse(FourLinesByPairs.attacks(Cell(0, 0), Cell(1, 2)))
        assertFalse(FourLinesByPairs.attacks(Cell(4, 4), Cell(6, 5)))
    }

    @Test
    fun `attacking is symmetric`() {
        val a = Cell(2, 3)
        val b = Cell(5, 6)
        val c = Cell(7, 1)
        assertEquals(FourLinesByPairs.attacks(a, b), FourLinesByPairs.attacks(b, a))
        assertEquals(FourLinesByPairs.attacks(a, c), FourLinesByPairs.attacks(c, a))
    }

    @Test
    fun `a cell attacks itself, so callers must exclude the identity pair`() {
        val cell = Cell(4, 4)
        assertTrue(FourLinesByPairs.attacks(cell, cell))
    }
}
