package com.mdimitrov.nqueens.history.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
private val DeleteSide = 48.dp
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
        // The title and the way off the screen both stay put; only the records scroll. Either of
        // them inside the list would be composed away by a long enough table, and "Clear all" is
        // then out of reach of a finger and of a screen reader alike.
        Header(canClear = state.groups.isNotEmpty(), onClearAll = { asking = true })

        LazyColumn(modifier = Modifier.weight(1f).padding(top = Spacing.lg)) {
            if (state.groups.isEmpty()) {
                item(key = "empty") { Nothing(state = state) }
            }

            // A card is drawn a row at a time rather than as one item, so a board solved a
            // hundred times composes what fits on the screen — and every one of those solves
            // keeps a delete button of its own.
            state.groups.forEach { group ->
                item(key = "group ${group.size}") { GroupHeader(group = group) }

                itemsIndexed(group.solves, key = { _, solve -> solve.id }) { index, solve ->
                    ScoreRow(
                        rank = index + 1,
                        solve = solve,
                        last = index == group.solves.lastIndex,
                        onDelete = onDelete,
                    )
                }
            }
        }

        Button(
            onClick = onNewGame,
            modifier =
                Modifier
                    .padding(top = Spacing.md)
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
                modifier = Modifier.semantics { heading() },
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

/**
 * What stands in for the list. Nothing at all until the table has answered: a screen that says
 * "nothing solved yet" while the records are still on their way tells the player something untrue
 * about their own history.
 */
@Composable
private fun Nothing(state: ScoresUiState) {
    if (!state.answered) return

    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(if (state.readable) R.string.scores_empty else R.string.scores_unreadable),
            style = MaterialTheme.typography.bodyLarge,
            color = NQueensTheme.board.onSurfaceMuted,
        )
    }
}

@Composable
private fun GroupHeader(group: ScoreGroup) {
    val colors = NQueensTheme.board

    Row(
        modifier =
            Modifier
                .padding(top = Spacing.md)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = Radii.md, topEnd = Radii.md))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.setup_board_size_value, group.size),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = pluralStringResource(R.plurals.scores_solves, group.solves.size, group.solves.size),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceMuted,
        )
    }
}

@Composable
private fun ScoreRow(
    rank: Int,
    solve: Solve,
    last: Boolean,
    onDelete: (Long) -> Unit,
) {
    val colors = NQueensTheme.board
    val time = formatElapsed(solve.seconds)
    val context = LocalContext.current
    val day = formatSolveDate(context, solve.finishedAt)
    // The moment it was finished is what tells two equally fast solves of one board apart, and
    // unlike the rank it does not change when a neighbour is deleted — which matters for a label
    // a screen reader may be holding when the list under it moves.
    val erase = stringResource(R.string.scores_delete, time, solve.size, formatSolveMoment(context, solve.finishedAt))

    val floor = if (last) Radii.md else 0.dp

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = floor, bottomEnd = floor))
                .background(MaterialTheme.colorScheme.surface),
    ) {
        HorizontalDivider(thickness = HairlineBorder, color = colors.border)

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SolveLine(rank = rank, time = time, day = day, modifier = Modifier.weight(1f))
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
}

/** The rank, the time and the day, spoken as one sentence rather than as three separate items. */
@Composable
private fun SolveLine(
    rank: Int,
    time: String,
    day: String,
    modifier: Modifier = Modifier,
) {
    val spoken = stringResource(R.string.scores_row, stringResource(R.string.scores_rank, rank), time, day)

    Row(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = spoken },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RankBadge(rank = rank)
        Text(
            text = time,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = Spacing.md),
        )
        // The date is what gives way when the row runs out of room: at the largest font on a
        // narrow phone it would otherwise take the width the solve time needs, and the time is
        // what the screen is for.
        Text(
            text = day,
            style = MaterialTheme.typography.labelSmall,
            color = NQueensTheme.board.onSurfaceMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = Spacing.sm).weight(1f),
        )
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
