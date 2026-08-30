package com.mdimitrov.nqueens.setup.presentation

import com.mdimitrov.nqueens.domain.MIN_BOARD_SIZE
import com.mdimitrov.nqueens.puzzle.LARGEST_PLAYABLE_BOARD
import com.mdimitrov.nqueens.puzzle.Variant

internal data class SetupUiState(
    val size: Int,
    val variant: Variant,
) {
    val canGrow: Boolean get() = size < LARGEST_PLAYABLE_BOARD
    val canShrink: Boolean get() = size > MIN_BOARD_SIZE
}
