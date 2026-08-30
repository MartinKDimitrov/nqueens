package com.mdimitrov.nqueens.domain

/** What kind of line two squares can share. */
public enum class LineKind { ROW, COLUMN, DESCENDING_DIAGONAL, ASCENDING_DIAGONAL }

/** One line of attack: all squares with the same [kind] and [index] threaten one another. */
public data class Line(
    public val kind: LineKind,
    public val index: Int,
)

/** Rules expressed structurally, as the lines a square belongs to. */
public fun interface LineRules {
    public fun linesThrough(cell: Cell): Set<Line>
}

/** The four lines through a piece's square. */
public val NQueensLines: LineRules =
    LineRules { cell ->
        setOf(
            Line(LineKind.ROW, cell.row),
            Line(LineKind.COLUMN, cell.col),
            Line(LineKind.DESCENDING_DIAGONAL, cell.descendingDiagonal),
            Line(LineKind.ASCENDING_DIAGONAL, cell.ascendingDiagonal),
        )
    }
