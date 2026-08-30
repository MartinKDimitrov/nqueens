package com.mdimitrov.nqueens.domain

/**
 * Every piece threatened by at least one other.
 *
 * Counts how many pieces occupy each line, then flags those sitting on a line that holds more
 * than one.
 */
public fun conflicts(
    pieces: Set<Cell>,
    rules: LineRules,
): Set<Cell> {
    val linesByPiece = pieces.associateWith(rules::linesThrough)

    val occupancy = HashMap<Line, Int>()
    for (lines in linesByPiece.values) {
        for (line in lines) {
            occupancy[line] = (occupancy[line] ?: 0) + 1
        }
    }

    /** Return all occupied lines*/
    return linesByPiece
        .filterValues { lines -> lines.any { occupancy.getValue(it) > 1 } }
        .keys
}

/**
 * How many pieces are still to be placed to reach [target].
 *
 * The target is the caller's, not the puzzle's: N-Queens and N-Rooks want one piece per row, so
 * they pass the board size, but N-Bishops does not — a board of `n` holds `2n - 2` of them.
 */
public fun piecesLeft(
    pieces: Set<Cell>,
    target: Int,
): Int = (target - pieces.size).coerceAtLeast(0)
