package com.mdimitrov.puzzles.nqueens

import com.mdimitrov.puzzles.boardlogic.Cell
import com.mdimitrov.puzzles.boardlogic.Line
import kotlin.test.Test
import kotlin.test.assertEquals

private const val A_BOARD = 8

/**
 * A queen attacks the obvious way — same row, same column, same diagonal — rather than as the
 * lines she stands on. The rules under test are written the other way round, so this is a second
 * statement of the same rule and not a restatement of the first.
 */
private fun attacks(
    a: Cell,
    b: Cell,
): Boolean =
    a.row == b.row ||
        a.col == b.col ||
        a.descendingDiagonal == b.descendingDiagonal ||
        a.ascendingDiagonal == b.ascendingDiagonal

class NQueensLinesTest {
    @Test
    fun `a queen stands on her row, her column and both her diagonals`() {
        assertEquals(
            setOf(
                Line(QueenAxis.ROW, 2),
                Line(QueenAxis.COLUMN, 3),
                Line(QueenAxis.DESCENDING_DIAGONAL, -1),
                Line(QueenAxis.ASCENDING_DIAGONAL, 5),
            ),
            NQueensLines.linesThrough(Cell(row = 2, col = 3)),
            "the four lines through a square, by kind and index",
        )
    }

    @Test
    fun `two queens share a line exactly when one attacks the other`() {
        val board = squaresOf(A_BOARD)

        val disagreed =
            board.flatMap { a -> board.map { b -> a to b } }.filter { (a, b) ->
                val shared = NQueensLines.linesThrough(a).intersect(NQueensLines.linesThrough(b)).isNotEmpty()

                shared != attacks(a, b)
            }

        assertEquals(emptyList(), disagreed, "squares the two statements of the rule disagree about")
    }

    @Test
    fun `each kind of line is the only one that answers for its own direction`() {
        val origin = Cell(row = 3, col = 3)
        val alongEach =
            mapOf(
                QueenAxis.ROW to Cell(row = 3, col = 6),
                QueenAxis.COLUMN to Cell(row = 6, col = 3),
                QueenAxis.DESCENDING_DIAGONAL to Cell(row = 5, col = 5),
                QueenAxis.ASCENDING_DIAGONAL to Cell(row = 5, col = 1),
            )

        val missing =
            alongEach.filterNot { (kind, reached) ->
                NQueensLines
                    .linesThrough(origin)
                    .intersect(NQueensLines.linesThrough(reached))
                    .singleOrNull()
                    ?.kind == kind
            }

        assertEquals(emptyMap(), missing, "directions the rules no longer threaten along")
    }

    private fun squaresOf(size: Int): List<Cell> {
        val axis = 0 until size

        return axis.flatMap { row -> axis.map { col -> Cell(row, col) } }
    }
}
