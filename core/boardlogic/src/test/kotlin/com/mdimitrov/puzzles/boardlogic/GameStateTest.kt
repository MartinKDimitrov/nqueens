package com.mdimitrov.puzzles.boardlogic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** The invariants a board must hold whoever builds it. */
class GameStateTest {
    @Test
    fun `a board of no squares at all is refused`() {
        assertFailsWith<IllegalArgumentException> { GameState(size = 0) }
    }

    @Test
    fun `a board of one square is a board, whatever puzzle would bother with it`() {
        assertEquals(1, GameState(size = 1).size)
    }

    @Test
    fun `a queen off the board is refused, in either direction`() {
        // Both edges, because a board is square and an off-by-one is not: a guard written for one
        // of them passes every test that only walks off the side.
        assertFailsWith<IllegalArgumentException>("a column past the last one") {
            GameState(size = 4, pieces = setOf(Cell(0, 4)))
        }
        assertFailsWith<IllegalArgumentException>("a row past the last one") {
            GameState(size = 4, pieces = setOf(Cell(4, 0)))
        }
    }

    @Test
    fun `a new board starts empty`() {
        assertTrue(GameState(size = 8).pieces.isEmpty())
    }

    @Test
    fun `an offending queen is named, so a failure says which one`() {
        val thrown =
            assertFailsWith<IllegalArgumentException> {
                GameState(size = 4, pieces = setOf(Cell(0, 0), Cell(9, 9)))
            }
        assertTrue(thrown.message.orEmpty().contains("Cell(row=9, col=9)"), thrown.message)
    }

    @Test
    fun `a board far larger than any app would offer is still held`() {
        // The literal, not the constant: written in terms of `MAX_BOARD_SIZE` this pair passes for
        // any value it takes, and the reasoning behind the number — roughly a million squares, the
        // point past which a projection is no longer one to build in one go — would go untested.
        assertEquals(1024, MAX_BOARD_SIZE, "the largest board the domain says it will hold")
        assertEquals(1024, GameState(size = 1024).size)
    }

    @Test
    fun `a board too large to be drawn is refused`() {
        assertFailsWith<IllegalArgumentException> { GameState(size = 1025) }
    }

    @Test
    fun `a board cannot have run for a negative time`() {
        assertFailsWith<IllegalArgumentException> { GameState(size = 4, elapsedSeconds = -1) }
    }
}
