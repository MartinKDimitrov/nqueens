package com.mdimitrov.nqueens.game.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.mdimitrov.nqueens.R
import com.mdimitrov.nqueens.domain.BoardSnapshot
import com.mdimitrov.nqueens.domain.Cell
import com.mdimitrov.nqueens.domain.CellStatus
import com.mdimitrov.nqueens.domain.isLightCell
import com.mdimitrov.nqueens.puzzle.Variant
import com.mdimitrov.nqueens.theme.BoardInset
import com.mdimitrov.nqueens.theme.HairlineBorder
import com.mdimitrov.nqueens.theme.NQueensTheme
import com.mdimitrov.nqueens.theme.Radii

private const val QUEEN_SCALE = 0.7f

@Composable
internal fun BoardCard(
    board: BoardSnapshot,
    variant: Variant,
    onTap: (Cell) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NQueensTheme.board

    Box(
        modifier =
            modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(Radii.md))
                .background(MaterialTheme.colorScheme.surface)
                .border(HairlineBorder, colors.border, RoundedCornerShape(Radii.md))
                .padding(BoardInset),
    ) {
        Board(board = board, onTap = onTap, variant = variant)
    }
}

@Composable
private fun Board(
    board: BoardSnapshot,
    variant: Variant,
    onTap: (Cell) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().aspectRatio(1f)) {
        for (row in 0 until board.size) {
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                for (col in 0 until board.size) {
                    Square(
                        cell = Cell(row, col),
                        status = board.statusAt(row, col),
                        onTap = onTap,
                        modifier = Modifier.weight(1f).fillMaxSize(),
                        variant = variant,
                    )
                }
            }
        }
    }
}

@Composable
private fun Square(
    cell: Cell,
    variant: Variant,
    status: CellStatus,
    onTap: (Cell) -> Unit,
    modifier: Modifier,
) {
    val colors = NQueensTheme.board
    val tint = queenTint(status, colors)

    val description =
        when (status) {
            CellStatus.EMPTY -> stringResource(R.string.game_cell_empty, cell.row + 1, cell.col + 1)
            CellStatus.PIECE -> stringResource(variant.text.cell, cell.row + 1, cell.col + 1)
            CellStatus.PIECE_CONFLICT -> stringResource(variant.text.cellUnderAttack, cell.row + 1, cell.col + 1)
        }

    Box(
        modifier =
            modifier
                .background(squareBackground(status, isLightCell(cell.row, cell.col), colors))
                .clickable { onTap(cell) }
                .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (tint != null) {
            Icon(
                painter = painterResource(variant.piece),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.fillMaxSize(QUEEN_SCALE),
            )
        }
    }
}
