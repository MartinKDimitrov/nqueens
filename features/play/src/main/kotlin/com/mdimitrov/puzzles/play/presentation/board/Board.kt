package com.mdimitrov.puzzles.play.presentation.board

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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.mdimitrov.puzzles.boardlogic.BoardSnapshot
import com.mdimitrov.puzzles.boardlogic.Cell
import com.mdimitrov.puzzles.boardlogic.CellStatus
import com.mdimitrov.puzzles.boardlogic.isLightCell
import com.mdimitrov.puzzles.play.R
import com.mdimitrov.puzzles.play.presentation.sound.LocalSounds
import com.mdimitrov.puzzles.play.presentation.sound.PlaySound
import com.mdimitrov.puzzles.puzzletype.Puzzle
import com.mdimitrov.puzzles.theme.BoardInset
import com.mdimitrov.puzzles.theme.HairlineBorder
import com.mdimitrov.puzzles.theme.PuzzleTheme
import com.mdimitrov.puzzles.theme.Radii

private const val PIECE_SCALE = 0.7f

@Composable
internal fun BoardCard(
    board: BoardSnapshot,
    puzzle: Puzzle,
    onTap: ((Cell) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = PuzzleTheme.board

    Box(
        modifier =
            modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(Radii.md))
                .background(MaterialTheme.colorScheme.surface)
                .border(HairlineBorder, colors.border, RoundedCornerShape(Radii.md))
                .padding(BoardInset),
    ) {
        Board(board = board, onTap = onTap, puzzle = puzzle)
    }
}

@Composable
private fun Board(
    board: BoardSnapshot,
    puzzle: Puzzle,
    onTap: ((Cell) -> Unit)?,
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
                        puzzle = puzzle,
                    )
                }
            }
        }
    }
}

@Composable
private fun Square(
    cell: Cell,
    puzzle: Puzzle,
    status: CellStatus,
    onTap: ((Cell) -> Unit)?,
    modifier: Modifier,
) {
    val colors = PuzzleTheme.board
    val tint = pieceTint(status, colors)
    val haptics = LocalHapticFeedback.current
    val sounds = LocalSounds.current
    val landing = landingOf(standing = tint != null)
    val flinch = flinchOf(attacked = status == CellStatus.PIECE_CONFLICT)

    val description =
        when (status) {
            CellStatus.EMPTY -> stringResource(R.string.play_cell_empty, cell.row + 1, cell.col + 1)
            CellStatus.PIECE -> stringResource(puzzle.text.cell, cell.row + 1, cell.col + 1)
            CellStatus.PIECE_CONFLICT -> stringResource(puzzle.text.cellUnderAttack, cell.row + 1, cell.col + 1)
        }

    Box(
        modifier =
            modifier
                .background(squareBackground(status, isLightCell(cell.row, cell.col), colors))
                .clickable(enabled = onTap != null) {
                    // A piece answers the finger the way one answers the hand.
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    sounds.play(if (status == CellStatus.EMPTY) PlaySound.PLACE else PlaySound.REMOVE)
                    onTap?.invoke(cell)
                }
                .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (tint != null) {
            Icon(
                painter = painterResource(puzzle.piece),
                contentDescription = null,
                tint = tint,
                modifier =
                    Modifier
                        .fillMaxSize(PIECE_SCALE)
                        .graphicsLayer {
                            scaleX = landing
                            scaleY = landing
                            translationX = flinch * ShakeWidth.toPx()
                        },
            )
        }
    }
}
