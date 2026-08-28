package com.mdimitrov.nqueens.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** The invariants a board must hold whoever builds it. */
class GameStateTest {
    @Test
    fun `a board smaller than four is refused`() {
        assertFailsWith<IllegalArgumentException> { GameState(size = 3) }
    }

    @Test
    fun `a queen off the board is refused`() {
        assertFailsWith<IllegalArgumentException> {
            GameState(size = 4, queens = setOf(Cell(0, 4)))
        }
    }

    @Test
    fun `a new board starts empty`() {
        assertTrue(GameState(size = 8).queens.isEmpty())
    }

    @Test
    fun `an offending queen is named, so a failure says which one`() {
        val thrown =
            assertFailsWith<IllegalArgumentException> {
                GameState(size = 4, queens = setOf(Cell(0, 0), Cell(9, 9)))
            }
        assertTrue(thrown.message.orEmpty().contains("Cell(row=9, col=9)"), thrown.message)
    }

    @Test
    fun `a board far larger than any app would offer is still held`() {
        assertEquals(MAX_BOARD_SIZE, GameState(size = MAX_BOARD_SIZE).size)
    }

    @Test
    fun `a board too large to be drawn is refused`() {
        assertFailsWith<IllegalArgumentException> { GameState(size = MAX_BOARD_SIZE + 1) }
    }
}
