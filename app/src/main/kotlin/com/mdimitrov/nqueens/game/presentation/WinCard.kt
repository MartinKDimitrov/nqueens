package com.mdimitrov.nqueens.game.presentation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mdimitrov.nqueens.R
import com.mdimitrov.nqueens.format.formatElapsed
import com.mdimitrov.nqueens.theme.Elevation
import com.mdimitrov.nqueens.theme.NQueensTheme
import com.mdimitrov.nqueens.theme.Radii
import com.mdimitrov.nqueens.theme.Spacing
import com.mdimitrov.nqueens.theme.TouchTarget

private const val SCRIM_ALPHA = 0.55f
private const val BADGE_ALPHA = 0.15f
private val BADGE_SIZE = 92.dp
private val BADGE_GLYPH = 48.dp
private val CARD_WIDTH = 360.dp

/** The solved board under the scrim, the celebration over it, the card over both. */
@Composable
internal fun WinCard(
    state: GameUiState,
    onPlayAgain: () -> Unit,
    onScores: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The card arrives on its own, so it is also felt on its own — once. The composition is
    // rebuilt by a rotation and by the trip to the records and back, while the win it announces
    // happened only the first time, so the flag outlives the composition rather than the board.
    val haptics = LocalHapticFeedback.current
    val sounds = LocalSounds.current
    var announced by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!announced) {
            announced = true
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            sounds.play(GameSound.WIN)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = SCRIM_ALPHA)),
        )

        Celebration(modifier = Modifier.fillMaxSize())

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.lg),
            contentAlignment = Alignment.Center,
        ) {
            WinBody(state = state, onPlayAgain = onPlayAgain, onScores = onScores)
        }
    }
}

@Composable
private fun WinBody(
    state: GameUiState,
    onPlayAgain: () -> Unit,
    onScores: () -> Unit,
) {
    val variantName = stringResource(state.variant.name)
    val summary = stringResource(R.string.game_board_summary, state.board.size, variantName)
    val announcement =
        stringResource(
            R.string.game_solved_announcement,
            summary,
            formatElapsed(state.elapsedSeconds),
        )

    Column(
        modifier =
            Modifier
                .widthIn(max = CARD_WIDTH)
                .fillMaxWidth()
                .shadow(Elevation.high, RoundedCornerShape(Radii.lg))
                .clip(RoundedCornerShape(Radii.lg))
                .background(MaterialTheme.colorScheme.surface)
                .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PieceBadge(piece = state.variant.piece)

        Text(
            text = stringResource(R.string.game_solved),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
            // Nobody touched anything to bring this card here, so it announces itself — and says
            // the whole result rather than only the word that is drawn.
            modifier =
                Modifier.padding(top = Spacing.lg).semantics {
                    heading()
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = announcement
                },
        )
        Text(
            text = summary,
            style = MaterialTheme.typography.labelSmall,
            color = NQueensTheme.board.onSurfaceMuted,
            modifier = Modifier.padding(top = Spacing.xs),
        )

        FinishingTime(seconds = state.elapsedSeconds)
        NewBest(seconds = state.elapsedSeconds, previousBest = state.previousBestSeconds)

        WinActions(onPlayAgain = onPlayAgain, onScores = onScores)
    }
}

/** Another board, or the records. Both are drawn small and take the whole of a finger. */
@Composable
private fun WinActions(
    onPlayAgain: () -> Unit,
    onScores: () -> Unit,
) {
    Button(
        onClick = onPlayAgain,
        shape = RoundedCornerShape(Radii.md),
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.lg).heightIn(min = TouchTarget),
    ) {
        Text(
            text = stringResource(R.string.game_play_again),
            style = MaterialTheme.typography.labelLarge,
        )
    }
    TextButton(
        onClick = onScores,
        modifier = Modifier.padding(top = Spacing.xs).heightIn(min = TouchTarget),
    ) {
        Text(
            text = stringResource(R.string.game_view_scores),
            style = MaterialTheme.typography.labelLarge,
            color = NQueensTheme.board.onSurfaceMuted,
        )
    }
}

@Composable
private fun PieceBadge(
    @DrawableRes piece: Int,
) {
    val success = NQueensTheme.board.success

    Box(
        modifier =
            Modifier
                .size(BADGE_SIZE)
                .clip(CircleShape)
                .background(success.copy(alpha = BADGE_ALPHA)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(piece),
            contentDescription = null,
            tint = success,
            modifier = Modifier.size(BADGE_GLYPH),
        )
    }
}

@Composable
private fun FinishingTime(seconds: Int) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = Spacing.lg)
                .clip(RoundedCornerShape(Radii.md))
                .background(NQueensTheme.board.surfaceAlt)
                .padding(vertical = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.game_finishing_time),
            style = MaterialTheme.typography.labelSmall,
            color = NQueensTheme.board.onSurfaceMuted,
        )
        Text(
            text = formatElapsed(seconds),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = Spacing.xs),
        )
    }
}

@Composable
private fun NewBest(
    seconds: Int,
    previousBest: Int?,
) {
    // Only a board finished faster than the one before it says so: a tie is not a new best.
    val betterBy = previousBest?.minus(seconds)?.takeIf { it > 0 } ?: return

    Text(
        text = stringResource(R.string.game_new_best, betterBy),
        style = MaterialTheme.typography.labelLarge,
        color = NQueensTheme.board.success,
        modifier = Modifier.padding(top = Spacing.sm),
    )
}
