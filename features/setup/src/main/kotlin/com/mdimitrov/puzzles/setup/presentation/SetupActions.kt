package com.mdimitrov.puzzles.setup.presentation

import com.mdimitrov.puzzles.puzzletype.Puzzle
import com.mdimitrov.puzzles.settings.ThemeChoice

/** Everything the Setup screen asks of the app around it. */
internal data class SetupActions(
    val onShrink: () -> Unit,
    val onGrow: () -> Unit,
    val onChoose: (Puzzle) -> Unit,
    val onStart: (Puzzle, Int) -> Unit,
    val onScores: () -> Unit,
    val onTheme: (ThemeChoice) -> Unit,
)
