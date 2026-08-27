package com.mdimitrov.nqueens.domain

/** What kind of line two squares can share. */
public enum class LineType { ROW, COLUMN, DESCENDING_DIAGONAL, ASCENDING_DIAGONAL }

/** One line of attack: all squares with the same [kind] and [index] threaten one another. */
public data class Line(
    public val kind: LineType,
    public val index: Int,
)

/** Rules expressed structurally, as the lines a square belongs to. **/
public fun interface LineRules {
    public fun linesThrough(cell: Cell): List<Line>
}

/** The four lines through a queen's square. */
public val NQueensLines: LineRules =
    LineRules { cell ->
        listOf(
            Line(LineType.ROW, cell.row),
            Line(LineType.COLUMN, cell.col),
            Line(LineType.DESCENDING_DIAGONAL, cell.descendingDiagonal),
            Line(LineType.ASCENDING_DIAGONAL, cell.ascendingDiagonal),
        )
    }
