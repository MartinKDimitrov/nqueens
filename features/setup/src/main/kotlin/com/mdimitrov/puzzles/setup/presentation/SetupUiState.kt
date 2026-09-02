package com.mdimitrov.puzzles.setup.presentation

import com.mdimitrov.puzzles.puzzletype.Puzzle
import com.mdimitrov.puzzles.settings.ThemeChoice

internal data class SetupUiState(
    val size: Int,
    val puzzle: Puzzle,
    /** Every puzzle this build was assembled with, so the screen can offer a choice of one. */
    val installed: List<Puzzle> = listOf(puzzle),
    /** Which palette the player has asked for, or null while they have not asked. */
    val theme: ThemeChoice? = null,
) {
    val canGrow: Boolean get() = size < puzzle.sizes.last
    val canShrink: Boolean get() = size > puzzle.sizes.first
}
