package com.mdimitrov.puzzles.play.presentation.board

import androidx.compose.ui.graphics.Color
import com.mdimitrov.puzzles.boardlogic.CellStatus
import com.mdimitrov.puzzles.theme.BoardColors

internal fun squareBackground(
    status: CellStatus,
    light: Boolean,
    colors: BoardColors,
): Color =
    when (status) {
        CellStatus.PIECE_CONFLICT -> colors.conflictGlow
        else -> if (light) colors.boardLight else colors.boardDark
    }

internal fun pieceTint(
    status: CellStatus,
    colors: BoardColors,
): Color? =
    when (status) {
        CellStatus.EMPTY -> null
        CellStatus.PIECE -> colors.queen
        CellStatus.PIECE_CONFLICT -> colors.conflict
    }
