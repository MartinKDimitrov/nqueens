package com.mdimitrov.puzzles.boardlogic

/**
 * A rule stated the obvious way — does this piece attack that one — rather than as the lines a
 * piece stands on. It exists so that `conflicts`, which counts occupancy per line, can be checked
 * against an implementation written from a different idea.
 */
internal fun interface PairwiseRules {
    fun attacks(
        a: Cell,
        b: Cell,
    ): Boolean
}

/** The same rule as [FourLines], pair by pair. The family's tests name no puzzle. */
internal val FourLinesByPairs: PairwiseRules =
    PairwiseRules { a, b ->
        a.row == b.row ||
            a.col == b.col ||
            a.descendingDiagonal == b.descendingDiagonal ||
            a.ascendingDiagonal == b.ascendingDiagonal
    }
