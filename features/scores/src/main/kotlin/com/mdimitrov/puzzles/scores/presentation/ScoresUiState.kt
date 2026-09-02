package com.mdimitrov.puzzles.scores.presentation

import com.mdimitrov.puzzles.scores.domain.Solve

/** The list as the screen draws it: one card per board size. */
internal data class ScoresUiState(
    val groups: List<ScoreGroup> = emptyList(),
    /** True once the table has answered at all — an empty list before that means nothing yet. */
    val answered: Boolean = false,
    /**
     * False once the table has stopped answering, which is not the same as having nothing in it.
     * It does not come back within a visit to the screen.
     */
    val readable: Boolean = true,
)

internal data class ScoreGroup(
    val size: Int,
    /** Every solve of this size, fastest first. */
    val solves: List<Solve>,
)

/** Smallest board first, and inside each card the fastest solve first. */
internal fun groupsOf(solves: List<Solve>): List<ScoreGroup> =
    solves
        .groupBy { it.size }
        .entries
        .sortedBy { it.key }
        .map { (size, ofThatSize) -> ScoreGroup(size = size, solves = ofThatSize.sortedBy { it.seconds }) }
