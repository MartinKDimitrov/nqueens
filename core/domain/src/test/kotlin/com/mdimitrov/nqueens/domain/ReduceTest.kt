package com.mdimitrov.nqueens.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GameStateTest {
    @Test
    fun `a board smaller than four is refused, because it has no solution`() {
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
}

class ReduceTest {
    private data class Transition(
        val describes: String,
        val from: GameState,
        val action: GameAction,
        val expected: GameState,
    )

    @Test
    fun `every action moves the board where it should`() {
        val empty = GameState(size = 4)
        val one = GameState(size = 4, queens = setOf(Cell(0, 0)))
        val two = GameState(size = 4, queens = setOf(Cell(0, 0), Cell(1, 2)))

        val transitions =
            listOf(
                Transition(
                    "placing on an empty square",
                    empty,
                    GameAction.Toggle(Cell(0, 0)),
                    one,
                ),
                Transition(
                    "tapping a queen takes it back",
                    one,
                    GameAction.Toggle(Cell(0, 0)),
                    empty,
                ),
                Transition(
                    "placing a second queen leaves the first alone",
                    one,
                    GameAction.Toggle(Cell(1, 2)),
                    two,
                ),
                Transition(
                    "a queen under attack is placed, not refused",
                    one,
                    GameAction.Toggle(Cell(0, 3)),
                    GameState(size = 4, queens = setOf(Cell(0, 0), Cell(0, 3))),
                ),
                Transition(
                    "reset clears the board but keeps its size",
                    two,
                    GameAction.Reset,
                    empty,
                ),
                Transition(
                    "a new game replaces the board",
                    two,
                    GameAction.NewGame(size = 6),
                    GameState(size = 6),
                ),
            )

        for (case in transitions) {
            assertEquals(case.expected, reduce(case.from, case.action), case.describes)
        }
    }

    @Test
    fun `reducing never changes the state it was given`() {
        val before = GameState(size = 4, queens = setOf(Cell(0, 0)))
        val snapshot = before.copy()
        reduce(before, GameAction.Toggle(Cell(2, 2)))
        assertEquals(snapshot, before)
    }
}
