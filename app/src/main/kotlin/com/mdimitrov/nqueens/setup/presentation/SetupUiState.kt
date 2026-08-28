package com.mdimitrov.nqueens.setup.presentation

import com.mdimitrov.nqueens.LARGEST_PLAYABLE_BOARD
import com.mdimitrov.nqueens.domain.MIN_BOARD_SIZE

internal data class SetupUiState(val size: Int) {
    val canGrow: Boolean get() = size < LARGEST_PLAYABLE_BOARD
    val canShrink: Boolean get() = size > MIN_BOARD_SIZE
}
