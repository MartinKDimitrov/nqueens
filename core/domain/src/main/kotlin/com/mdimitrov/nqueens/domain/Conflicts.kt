package com.mdimitrov.nqueens.domain

/**
 * Every queen threatened by at least one other.
 *
 * Counts how many queens occupy each line, then flags those sitting on a line that holds more
 * than one.
 */
public fun conflicts(
    queens: Set<Cell>,
    rules: LineRules,
): Set<Cell> {
    val linesByQueen = queens.associateWith(rules::linesThrough)

    val occupancy = HashMap<Line, Int>()
    for (lines in linesByQueen.values) {
        for (line in lines) {
            occupancy[line] = (occupancy[line] ?: 0) + 1
        }
    }

    return linesByQueen
        .filterValues { lines -> lines.any { occupancy.getValue(it) > 1 } }
        .keys
}

/** How many queens are still to be placed on a board. */
public fun queensLeft(
    queens: Set<Cell>,
    size: Int,
): Int = (size - queens.size).coerceAtLeast(0)
