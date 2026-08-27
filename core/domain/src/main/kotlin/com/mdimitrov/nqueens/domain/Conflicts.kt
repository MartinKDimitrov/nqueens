package com.mdimitrov.nqueens.domain

/**
 * Every queen threatened by at least one other.
 *
 * Counts how many queens occupy each line, then flags those sitting on a line that holds more
 * than one.
 */
public fun conflicts(
    queens: Set<Cell>,
    rules: LineRules = NQueensLines,
): Set<Cell> {
    val occupancy = HashMap<Line, Int>()

    for (queen in queens) {
        for (line in rules.linesThrough(queen)) {
            occupancy[line] = (occupancy[line] ?: 0) + 1
        }
    }

    return queens.filterTo(mutableSetOf()) { queen ->
        rules.linesThrough(queen).any { occupancy.getValue(it) > 1 }
    }
}

/** How many queens are still to be placed on a board. */
public fun queensLeft(
    queens: Set<Cell>,
    size: Int,
): Int = size - queens.size

/** Solved when the board holds [size] queens and none of them is threatened. */
public fun isSolved(
    queens: Set<Cell>,
    size: Int,
    rules: LineRules = NQueensLines,
): Boolean = queens.size == size && conflicts(queens, rules).isEmpty()
