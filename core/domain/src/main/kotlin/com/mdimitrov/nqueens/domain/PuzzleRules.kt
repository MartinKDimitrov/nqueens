package com.mdimitrov.nqueens.domain

public fun interface PuzzleRules {
    public fun attacks(
        a: Cell,
        b: Cell,
    ): Boolean
}

public val NQueens: PuzzleRules =
    PuzzleRules { a, b ->
        a.row == b.row ||
            a.col == b.col ||
            a.descendingDiagonal == b.descendingDiagonal ||
            a.ascendingDiagonal == b.ascendingDiagonal
    }
