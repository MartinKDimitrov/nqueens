package com.mdimitrov.nqueens.game.presentation

import androidx.compose.ui.graphics.Color
import com.mdimitrov.nqueens.domain.CellStatus
import com.mdimitrov.nqueens.theme.BoardColors

internal fun squareBackground(
    status: CellStatus,
    light: Boolean,
    colors: BoardColors,
): Color =
    when (status) {
        CellStatus.PIECE_CONFLICT -> colors.conflictGlow
        else -> if (light) colors.boardLight else colors.boardDark
    }

internal fun queenTint(
    status: CellStatus,
    colors: BoardColors,
): Color? =
    when (status) {
        CellStatus.EMPTY -> null
        CellStatus.PIECE -> colors.queen
        CellStatus.PIECE_CONFLICT -> colors.conflict
    }
