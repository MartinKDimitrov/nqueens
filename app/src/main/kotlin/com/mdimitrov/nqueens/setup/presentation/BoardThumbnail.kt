package com.mdimitrov.nqueens.setup.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mdimitrov.nqueens.R
import com.mdimitrov.nqueens.theme.HairlineBorder
import com.mdimitrov.nqueens.theme.NQueensTheme
import com.mdimitrov.nqueens.theme.Radii
import com.mdimitrov.nqueens.theme.Spacing

/** The side of the board card in `design/screens/setup.svg`: 262 on a 390-wide artboard. */
private val ThumbnailSide = 262.dp

@Composable
internal fun BoardThumbnail(
    boardSize: Int,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.setup_board_thumbnail, boardSize)
    val board = NQueensTheme.board

    Column(
        modifier =
            modifier
                .size(ThumbnailSide)
                .clip(RoundedCornerShape(Radii.md))
                .background(MaterialTheme.colorScheme.surface)
                .border(HairlineBorder, board.border, RoundedCornerShape(Radii.md))
                .padding(Spacing.xs)
                .semantics { contentDescription = label },
    ) {
        for (row in 0 until boardSize) {
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                for (col in 0 until boardSize) {
                    val light = (row + col) % 2 == 0
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .background(if (light) board.boardLight else board.boardDark),
                    )
                }
            }
        }
    }
}
