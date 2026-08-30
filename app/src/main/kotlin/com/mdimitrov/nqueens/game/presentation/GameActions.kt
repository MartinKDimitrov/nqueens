package com.mdimitrov.nqueens.game.presentation

import com.mdimitrov.nqueens.domain.Cell

/** Everything the game screen asks of the app around it. */
internal data class GameActions(
    val onTap: (Cell) -> Unit,
    val onReset: () -> Unit,
    val onBack: () -> Unit,
    val onScores: () -> Unit,
)
