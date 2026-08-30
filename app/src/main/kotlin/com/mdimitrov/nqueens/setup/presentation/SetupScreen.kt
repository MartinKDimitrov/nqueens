package com.mdimitrov.nqueens.setup.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mdimitrov.nqueens.R
import com.mdimitrov.nqueens.domain.MIN_BOARD_SIZE
import com.mdimitrov.nqueens.puzzle.LARGEST_PLAYABLE_BOARD
import com.mdimitrov.nqueens.puzzle.Queens
import com.mdimitrov.nqueens.puzzle.Variant
import com.mdimitrov.nqueens.theme.HairlineBorder
import com.mdimitrov.nqueens.theme.NQueensTheme
import com.mdimitrov.nqueens.theme.Radii
import com.mdimitrov.nqueens.theme.Spacing

private val StepperHeight = 68.dp
private val StepperButtonSide = 40.dp
private val VariantRowHeight = 56.dp
private val StartButtonHeight = 60.dp
private val PieceIconSide = 22.dp
private val TitleLetterSpacing = 1.5.sp
private const val DISABLED_ALPHA = 0.4f

@Composable
internal fun SetupScreen(
    onStart: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SetupViewModel = hiltViewModel(),
) {
    SetupContent(
        state = viewModel.uiState,
        onShrink = viewModel::shrink,
        onGrow = viewModel::grow,
        onStart = onStart,
        modifier = modifier,
    )
}

@Composable
internal fun SetupContent(
    state: SetupUiState,
    onShrink: () -> Unit,
    onGrow: () -> Unit,
    onStart: (Int) -> Unit,
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
        Text(
            text = stringResource(R.string.setup_title),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = TitleLetterSpacing,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = stringResource(state.variant.text.subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = NQueensTheme.board.onSurfaceMuted,
        )

        Spacer(Modifier.height(Spacing.lg))
        BoardThumbnail(
            boardSize = state.size,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(Modifier.height(Spacing.xl))
        SectionLabel(stringResource(R.string.setup_board_size_label))
        Spacer(Modifier.height(Spacing.sm))
        BoardSizeStepper(state = state, onShrink = onShrink, onGrow = onGrow)

        Spacer(Modifier.height(Spacing.lg))
        SectionLabel(stringResource(R.string.setup_variant_label))
        Spacer(Modifier.height(Spacing.sm))
        VariantRow(variant = state.variant)

        Spacer(Modifier.height(Spacing.xl))
        Button(
            onClick = { onStart(state.size) },
            modifier = Modifier.fillMaxWidth().height(StartButtonHeight),
            shape = RoundedCornerShape(Radii.md),
        ) {
            Text(
                text = stringResource(R.string.setup_start),
                style = MaterialTheme.typography.labelLarge,
            )
        }
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
        color = NQueensTheme.board.onSurfaceMuted,
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
    val board = NQueensTheme.board

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
                text = stringResource(R.string.setup_size_range, MIN_BOARD_SIZE, LARGEST_PLAYABLE_BOARD),
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
                .size(StepperButtonSide)
                .clip(RoundedCornerShape(Radii.sm))
                .background(dimmedUnless(colors.background, enabled))
                .clickable(enabled = enabled, onClick = onClick)
                .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.headlineSmall,
            color = dimmedUnless(colors.glyph, enabled),
        )
    }
}

@Composable
private fun VariantRow(
    variant: Variant,
    modifier: Modifier = Modifier,
) {
    val board = NQueensTheme.board

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = VariantRowHeight)
                .clip(RoundedCornerShape(Radii.md))
                .background(MaterialTheme.colorScheme.surface)
                .border(HairlineBorder, board.border, RoundedCornerShape(Radii.md))
                .padding(horizontal = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(variant.piece),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(PieceIconSide),
        )
        Spacer(Modifier.width(Spacing.md))
        Text(
            text = stringResource(variant.name),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun dimmedUnless(
    color: Color,
    enabled: Boolean,
): Color = if (enabled) color else color.copy(alpha = DISABLED_ALPHA)

@Preview(name = "Setup — smallest board", showBackground = true)
@Composable
private fun SetupSmallestPreview() {
    NQueensTheme {
        SetupContent(
            state = SetupUiState(MIN_BOARD_SIZE, Queens),
            onShrink = {},
            onGrow = {},
            onStart = {},
        )
    }
}

@Preview(name = "Setup — largest board, dark", showBackground = true)
@Composable
private fun SetupLargestDarkPreview() {
    NQueensTheme(darkTheme = true) {
        SetupContent(
            state = SetupUiState(LARGEST_PLAYABLE_BOARD, Queens),
            onShrink = {},
            onGrow = {},
            onStart = {},
        )
    }
}
