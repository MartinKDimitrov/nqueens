package com.mdimitrov.nqueens.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class ReduceTest {
    private data class Transition(
        val description: String,
        val from: GameState,
        val action: GameAction,
        val expected: GameState,
    )

    @Test
    fun `every action moves the board where it should`() {
        val empty = GameState(size = 4)
        val one = GameState(size = 4, pieces = setOf(Cell(0, 0)))
        val two = GameState(size = 4, pieces = setOf(Cell(0, 0), Cell(1, 2)))

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
                    GameState(size = 4, pieces = setOf(Cell(0, 0), Cell(0, 3))),
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
            assertEquals(case.expected, reduce(case.from, case.action), case.description)
        }
    }

    @Test
    fun `an action that cannot be carried out leaves the board alone`() {
        val board = GameState(size = 4, pieces = setOf(Cell(0, 0)))

        assertEquals(
            board,
            reduce(board, GameAction.Toggle(Cell(9, 9))),
            "tap outside the board",
        )

        assertEquals(
            board,
            reduce(board, GameAction.Toggle(Cell(-1, 0))),
            "negative coordinate",
        )

        assertEquals(
            board,
            reduce(board, GameAction.NewGame(size = 2)),
            "board with no solution",
        )

        assertEquals(
            board,
            reduce(board, GameAction.NewGame(size = MAX_BOARD_SIZE + 1)),
            "one above the largest board",
        )

        assertEquals(
            board,
            reduce(board, GameAction.NewGame(size = MIN_BOARD_SIZE - 1)),
            "one below the smallest board",
        )

        assertEquals(
            board,
            reduce(board, GameAction.NewGame(size = Int.MAX_VALUE)),
            "board too large to be drawn",
        )
    }

    @Test
    fun `reducing never changes the state it was given`() {
        val queens = setOf(Cell(0, 0))
        val before = GameState(size = 4, pieces = queens)

        reduce(before, GameAction.Toggle(Cell(2, 2)))
        reduce(before, GameAction.Toggle(Cell(0, 0)))
        reduce(before, GameAction.Reset)

        assertEquals(4, before.size)
        assertEquals(setOf(Cell(0, 0)), before.pieces)
        assertEquals(setOf(Cell(0, 0)), queens, "the set handed in was written through")
    }

    @Test
    fun `a new game can be started at either end of the range`() {
        val board = GameState(size = 8, pieces = setOf(Cell(0, 0)))

        assertEquals(GameState(size = MIN_BOARD_SIZE), reduce(board, GameAction.NewGame(size = MIN_BOARD_SIZE)))
        assertEquals(GameState(size = MAX_BOARD_SIZE), reduce(board, GameAction.NewGame(size = MAX_BOARD_SIZE)))
    }

    @Test
    fun `time advances one tick at a time and starts over with the board`() {
        val board = GameState(size = 4, pieces = setOf(Cell(0, 0)), elapsedSeconds = 41)

        assertEquals(42, reduce(board, GameAction.Tick).elapsedSeconds)
        assertEquals(0, reduce(board, GameAction.Reset).elapsedSeconds)
        assertEquals(41, reduce(board, GameAction.Toggle(Cell(1, 1))).elapsedSeconds)
    }
}
