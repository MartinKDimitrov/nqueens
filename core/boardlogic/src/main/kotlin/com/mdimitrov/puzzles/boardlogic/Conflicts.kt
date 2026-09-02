package com.mdimitrov.puzzles.boardlogic

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

    /** The pieces standing on a line more than one of them occupies. */
    return linesByPiece
        .filterValues { lines -> lines.any { occupancy.getValue(it) > 1 } }
        .keys
}

/**
 * How many pieces are still to be placed to reach [target].
 *
 * The target is the caller's, not the puzzle's: the queens want one piece per row and so pass the
 * board size, and a puzzle that wants a different number passes that instead.
 */
public fun piecesLeft(
    pieces: Set<Cell>,
    target: Int,
): Int = (target - pieces.size).coerceAtLeast(0)
