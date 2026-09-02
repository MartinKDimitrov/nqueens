package com.mdimitrov.puzzles.play.presentation

import com.mdimitrov.puzzles.boardlogic.Cell

/** Everything the game screen asks of the app around it. */
internal data class PlayActions(
    val onTap: (Cell) -> Unit,
    val onReset: () -> Unit,
    val onBack: () -> Unit,
    val onScores: () -> Unit,
)
