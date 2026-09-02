package com.mdimitrov.puzzles.nqueens

import com.mdimitrov.puzzles.boardlogic.Line
import com.mdimitrov.puzzles.boardlogic.LineKind
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
            Line(LineKind.ROW, cell.row),
            Line(LineKind.COLUMN, cell.col),
            Line(LineKind.DESCENDING_DIAGONAL, cell.descendingDiagonal),
            Line(LineKind.ASCENDING_DIAGONAL, cell.ascendingDiagonal),
        )
    }
