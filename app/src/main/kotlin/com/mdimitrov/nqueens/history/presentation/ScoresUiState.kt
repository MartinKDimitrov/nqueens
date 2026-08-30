package com.mdimitrov.nqueens.history.presentation

import com.mdimitrov.nqueens.history.domain.Solve

/** The list as the screen draws it: one card per board size. */
internal data class ScoresUiState(
    val groups: List<ScoreGroup> = emptyList(),
)

internal data class ScoreGroup(
    val size: Int,
    val runs: List<Solve>,
)

/** Smallest board first, and inside each card the fastest solve first. */
internal fun groupsOf(solves: List<Solve>): List<ScoreGroup> =
    solves
        .groupBy { it.size }
        .entries
        .sortedBy { it.key }
        .map { (size, runs) -> ScoreGroup(size, runs.sortedBy { it.seconds }) }
