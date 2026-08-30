package com.mdimitrov.nqueens.history.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mdimitrov.nqueens.R
import com.mdimitrov.nqueens.format.formatElapsed
import com.mdimitrov.nqueens.history.domain.Solve
import com.mdimitrov.nqueens.theme.HairlineBorder
import com.mdimitrov.nqueens.theme.NQueensTheme
import com.mdimitrov.nqueens.theme.Radii
import com.mdimitrov.nqueens.theme.Spacing

private val RankSide = 24.dp
private val DeleteSide = 44.dp
private val DeleteGlyph = 18.dp
private val NewGameHeight = 60.dp

@Composable
internal fun ScoresScreen(
    onNewGame: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScoresViewModel = hiltViewModel(),
) {
    ScoresContent(
        state = viewModel.uiState,
        onDelete = viewModel::onDelete,
        onClearAll = viewModel::onClearAll,
        onNewGame = onNewGame,
        modifier = modifier,
    )
}

@Composable
internal fun ScoresContent(
    state: ScoresUiState,
    onDelete: (Long) -> Unit,
    onClearAll: () -> Unit,
    onNewGame: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var asking by remember { mutableStateOf(false) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .safeDrawingPadding()
                .padding(horizontal = Spacing.lg, vertical = Spacing.xl),
    ) {
        Header(canClear = state.groups.isNotEmpty(), onClearAll = { asking = true })

        if (state.groups.isEmpty()) {
            Nothing(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(top = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                items(state.groups, key = { it.size }) { group ->
                    GroupCard(group = group, onDelete = onDelete)
                }
            }
        }

        Button(
            onClick = onNewGame,
            modifier =
                Modifier
                    .padding(top = Spacing.lg)
                    .fillMaxWidth()
                    .heightIn(min = NewGameHeight),
            shape = RoundedCornerShape(Radii.md),
        ) {
            Text(
                text = stringResource(R.string.scores_new_game),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }

    if (asking) {
        ClearAllDialog(
            onConfirm = {
                asking = false
                onClearAll()
            },
            onDismiss = { asking = false },
        )
    }
}

@Composable
private fun Header(
    canClear: Boolean,
    onClearAll: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.scores_title),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.scores_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = NQueensTheme.board.onSurfaceMuted,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }

        if (canClear) {
            TextButton(onClick = onClearAll) {
                Text(
                    text = stringResource(R.string.scores_clear_all),
                    style = MaterialTheme.typography.labelLarge,
                    color = NQueensTheme.board.conflict,
                )
            }
        }
    }
}

@Composable
private fun Nothing(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.scores_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = NQueensTheme.board.onSurfaceMuted,
        )
    }
}

@Composable
private fun GroupCard(
    group: ScoreGroup,
    onDelete: (Long) -> Unit,
) {
    val colors = NQueensTheme.board

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radii.md))
                .background(MaterialTheme.colorScheme.surface)
                .border(HairlineBorder, colors.border, RoundedCornerShape(Radii.md)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.setup_board_size_value, group.size),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = pluralStringResource(R.plurals.scores_solves, group.runs.size, group.runs.size),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceMuted,
            )
        }

        group.runs.forEachIndexed { index, solve ->
            HorizontalDivider(thickness = HairlineBorder, color = colors.border)
            ScoreRow(rank = index + 1, solve = solve, onDelete = onDelete)
        }
    }
}

@Composable
private fun ScoreRow(
    rank: Int,
    solve: Solve,
    onDelete: (Long) -> Unit,
) {
    val colors = NQueensTheme.board
    val time = formatElapsed(solve.seconds)
    val erase = stringResource(R.string.scores_delete, time, solve.size)

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RankBadge(rank = rank)
        Text(
            text = time,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = Spacing.md).weight(1f),
        )
        Text(
            text = formatSolveDate(LocalContext.current, solve.finishedAt),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceMuted,
        )
        Box(
            modifier =
                Modifier
                    .padding(start = Spacing.sm)
                    .size(DeleteSide)
                    .clip(CircleShape)
                    .clickable { onDelete(solve.id) }
                    .semantics { contentDescription = erase },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_trash),
                contentDescription = null,
                tint = colors.conflict,
                modifier = Modifier.size(DeleteGlyph),
            )
        }
    }
}

@Composable
private fun RankBadge(rank: Int) {
    val leader = rank == 1
    val colors = NQueensTheme.board

    Box(
        modifier =
            Modifier
                .sizeIn(minWidth = RankSide, minHeight = RankSide)
                .clip(CircleShape)
                .background(if (leader) MaterialTheme.colorScheme.primary else colors.surfaceAlt)
                .padding(horizontal = Spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.scores_rank, rank),
            style = MaterialTheme.typography.labelSmall,
            color = if (leader) MaterialTheme.colorScheme.onPrimary else colors.onSurfaceMuted,
        )
    }
}

@Composable
private fun ClearAllDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(Radii.lg),
        title = { Text(text = stringResource(R.string.scores_clear_title)) },
        text = { Text(text = stringResource(R.string.scores_clear_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.scores_clear_confirm),
                    color = NQueensTheme.board.conflict,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.scores_cancel))
            }
        },
    )
}
