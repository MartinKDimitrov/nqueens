package com.mdimitrov.puzzles.nqueens

import com.mdimitrov.puzzles.boardlogic.Line
import com.mdimitrov.puzzles.boardlogic.LineRules

/**
 * The four lines a queen threatens along: her row, her column and both diagonals.
 *
 * This is the whole of what makes the puzzle N-Queens, and it lives with the puzzle rather than
 * with the family: a piece that threatened along fewer of these lines would be a different game,
 * in a module of its own.
 */
public val NQueensLines: LineRules =
    LineRules { cell ->
        setOf(
            Line(QueenAxis.ROW, cell.row),
            Line(QueenAxis.COLUMN, cell.col),
            Line(QueenAxis.DESCENDING_DIAGONAL, cell.descendingDiagonal),
            Line(QueenAxis.ASCENDING_DIAGONAL, cell.ascendingDiagonal),
        )
    }
