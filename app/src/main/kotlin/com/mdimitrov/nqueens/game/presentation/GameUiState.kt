package com.mdimitrov.nqueens.game.presentation

import com.mdimitrov.nqueens.domain.BoardSnapshot
import com.mdimitrov.nqueens.puzzle.Variant

internal data class GameUiState(
    val board: BoardSnapshot,
    val variant: Variant,
    val elapsedSeconds: Int,
)
