package com.mdimitrov.puzzles.setup.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mdimitrov.puzzles.puzzletype.Puzzle
import com.mdimitrov.puzzles.settings.ThemeChoice
import com.mdimitrov.puzzles.setup.R
import com.mdimitrov.puzzles.theme.HairlineBorder
import com.mdimitrov.puzzles.theme.PuzzleTheme
import com.mdimitrov.puzzles.theme.Radii
import com.mdimitrov.puzzles.theme.Spacing
import com.mdimitrov.puzzles.theme.TouchTarget

private val StepperHeight = 68.dp
private val StepperButtonSide = 40.dp

private val PuzzleRowHeight = 56.dp
private val StartButtonHeight = 60.dp
private val PieceIconSide = 22.dp
private val TitleLetterSpacing = 1.5.sp
private const val DISABLED_ALPHA = 0.4f

@Composable
internal fun SetupScreen(
    onStart: (Puzzle, Int) -> Unit,
    onScores: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SetupViewModel = hiltViewModel(),
) {
    SetupContent(
        state = viewModel.uiState,
        actions =
            SetupActions(
                onShrink = viewModel::shrink,
                onGrow = viewModel::grow,
                onChoose = viewModel::choose,
                onStart = onStart,
                onScores = onScores,
                onTheme = viewModel::chooseTheme,
            ),
        modifier = modifier,
    )
}

@Composable
internal fun SetupContent(
    state: SetupUiState,
    actions: SetupActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.xl),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(state.puzzle.name),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = TitleLetterSpacing,
                modifier = Modifier.weight(1f),
            )
            ThemeButton(theme = state.theme, onTheme = actions.onTheme)
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = stringResource(state.puzzle.text.subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = PuzzleTheme.board.onSurfaceMuted,
        )

        Spacer(Modifier.height(Spacing.lg))
        BoardThumbnail(
            boardSize = state.size,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(Modifier.height(Spacing.xl))
        SectionLabel(stringResource(R.string.setup_board_size_label))
        Spacer(Modifier.height(Spacing.sm))
        BoardSizeStepper(state = state, onShrink = actions.onShrink, onGrow = actions.onGrow)

        Spacer(Modifier.height(Spacing.lg))
        SectionLabel(stringResource(R.string.setup_puzzle_label))
        Spacer(Modifier.height(Spacing.sm))
        // With one puzzle installed the row says which it is; with more it is how you choose.
        state.installed.forEach { puzzle ->
            PuzzleRow(
                puzzle = puzzle,
                chosen = puzzle.key == state.puzzle.key,
                onChoose = actions.onChoose.takeIf { state.installed.size > 1 },
            )
            Spacer(Modifier.height(Spacing.sm))
        }

        Spacer(Modifier.height(Spacing.xl))
        Choices(state = state, actions = actions)
    }
}

@Composable
private fun Choices(
    state: SetupUiState,
    actions: SetupActions,
) {
    Button(
        onClick = { actions.onStart(state.puzzle, state.size) },
        modifier = Modifier.fillMaxWidth().height(StartButtonHeight),
        shape = RoundedCornerShape(Radii.md),
    ) {
        Text(
            text = stringResource(R.string.setup_start),
            style = MaterialTheme.typography.labelLarge,
        )
    }

    OutlinedButton(
        onClick = actions.onScores,
        modifier =
            Modifier
                .padding(top = Spacing.md)
                .fillMaxWidth()
                .heightIn(min = TouchTarget),
        shape = RoundedCornerShape(Radii.md),
        border = BorderStroke(HairlineBorder, PuzzleTheme.board.border),
    ) {
        Text(
            text = stringResource(R.string.setup_scores),
            style = MaterialTheme.typography.labelLarge,
            color = PuzzleTheme.board.onSurfaceMuted,
        )
    }
}

/**
 * Which palette to draw in, as one button rather than three.
 *
 * Beside the title rather than under the last button: the screen scrolls, and a setting a player
 * has to go looking for below the fold is one they will not find. It says which palette it is in
 * rather than what pressing it does, because that is what someone coming back needs to read —
 * short on the button, and the whole sentence to a screen reader.
 */
@Composable
private fun ThemeButton(
    theme: ThemeChoice?,
    onTheme: (ThemeChoice) -> Unit,
) {
    // What is on screen, which is the player's answer or the phone's while they have none. The
    // button names it rather than what pressing it does, because that is what someone coming back
    // to the screen needs to read.
    val inForce = theme ?: if (isSystemInDarkTheme()) ThemeChoice.DARK else ThemeChoice.LIGHT
    val other = if (inForce == ThemeChoice.DARK) ThemeChoice.LIGHT else ThemeChoice.DARK
    val spoken =
        stringResource(
            if (inForce == ThemeChoice.DARK) R.string.setup_theme_dark else R.string.setup_theme_light,
        )

    OutlinedButton(
        onClick = { onTheme(other) },
        modifier = Modifier.heightIn(min = TouchTarget).semantics { contentDescription = spoken },
        shape = RoundedCornerShape(Radii.md),
        border = BorderStroke(HairlineBorder, PuzzleTheme.board.border),
    ) {
        Text(
            text =
                stringResource(
                    if (inForce == ThemeChoice.DARK) {
                        R.string.setup_theme_dark_short
                    } else {
                        R.string.setup_theme_light_short
                    },
                ),
            style = MaterialTheme.typography.labelLarge,
            color = PuzzleTheme.board.onSurfaceMuted,
        )
    }
}

@Composable
private fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = PuzzleTheme.board.onSurfaceMuted,
        modifier = modifier,
    )
}

@Composable
private fun BoardSizeStepper(
    state: SetupUiState,
    onShrink: () -> Unit,
    onGrow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val board = PuzzleTheme.board

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = StepperHeight)
                .clip(RoundedCornerShape(Radii.md))
                .background(MaterialTheme.colorScheme.surface)
                .border(HairlineBorder, board.border, RoundedCornerShape(Radii.md))
                .padding(horizontal = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton(
            symbol = "−",
            description = stringResource(R.string.setup_smaller_board),
            enabled = state.canShrink,
            colors = StepperColors(board.surfaceAlt, MaterialTheme.colorScheme.onSurface),
            onClick = onShrink,
        )
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.setup_board_size_value, state.size),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text =
                    stringResource(
                        R.string.setup_size_range,
                        state.puzzle.sizes.first,
                        state.puzzle.sizes.last,
                    ),
                style = MaterialTheme.typography.labelSmall,
                color = board.onSurfaceMuted,
            )
        }
        StepperButton(
            symbol = "+",
            description = stringResource(R.string.setup_larger_board),
            enabled = state.canGrow,
            colors =
                StepperColors(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.onPrimary,
                ),
            onClick = onGrow,
        )
    }
}

private data class StepperColors(val background: Color, val glyph: Color)

@Composable
private fun StepperButton(
    symbol: String,
    description: String,
    enabled: Boolean,
    colors: StepperColors,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(TouchTarget)
                .clip(RoundedCornerShape(Radii.sm))
                .clickable(enabled = enabled, onClick = onClick)
                .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(StepperButtonSide)
                    .clip(RoundedCornerShape(Radii.sm))
                    .background(dimmedUnless(colors.background, enabled)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = symbol,
                style = MaterialTheme.typography.headlineSmall,
                color = dimmedUnless(colors.glyph, enabled),
            )
        }
    }
}

@Composable
private fun PuzzleRow(
    puzzle: Puzzle,
    chosen: Boolean,
    onChoose: ((Puzzle) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val board = PuzzleTheme.board
    val outline = if (chosen) MaterialTheme.colorScheme.primary else board.border

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = PuzzleRowHeight)
                .clip(RoundedCornerShape(Radii.md))
                .background(MaterialTheme.colorScheme.surface)
                .border(HairlineBorder, outline, RoundedCornerShape(Radii.md))
                .selectable(
                    selected = chosen,
                    enabled = onChoose != null,
                    onClick = { onChoose?.invoke(puzzle) },
                )
                .padding(horizontal = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(puzzle.piece),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(PieceIconSide),
        )
        Spacer(Modifier.width(Spacing.md))
        Text(
            text = stringResource(puzzle.name),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun dimmedUnless(
    color: Color,
    enabled: Boolean,
): Color = if (enabled) color else color.copy(alpha = DISABLED_ALPHA)
