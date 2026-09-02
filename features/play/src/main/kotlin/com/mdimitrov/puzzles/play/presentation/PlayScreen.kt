package com.mdimitrov.puzzles.play.presentation

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mdimitrov.puzzles.boardlogic.BoardSnapshot
import com.mdimitrov.puzzles.boardlogic.Cell
import com.mdimitrov.puzzles.boardlogic.GameAction
import com.mdimitrov.puzzles.play.R
import com.mdimitrov.puzzles.play.presentation.board.BoardCard
import com.mdimitrov.puzzles.play.presentation.sound.LocalSounds
import com.mdimitrov.puzzles.play.presentation.sound.PlaySound
import com.mdimitrov.puzzles.play.presentation.sound.rememberSounds
import com.mdimitrov.puzzles.play.presentation.win.Win
import com.mdimitrov.puzzles.play.presentation.win.WinCard
import com.mdimitrov.puzzles.puzzletype.Puzzle
import com.mdimitrov.puzzles.theme.BoardInset
import com.mdimitrov.puzzles.theme.HairlineBorder
import com.mdimitrov.puzzles.theme.PuzzleTheme
import com.mdimitrov.puzzles.theme.Radii
import com.mdimitrov.puzzles.theme.Spacing

private val StatusHeight = 48.dp
private val MarkSize = 18.sp

// The smallest square the board will draw. Below this it stops shrinking and the screen scrolls
// instead, because a square nobody can hit is worse than a board that does not fit at once.
private val MinSquare = 24.dp

// The details only sit beside the board if the message in the status strip has room to be read.
private val MinSidePane = 220.dp

// Padding on both sides of the wide row, plus the gap between the board and the details.
private val WideGutters = Spacing.lg * 3

@Composable
internal fun PlayScreen(
    onBack: () -> Unit,
    onScores: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayViewModel = hiltViewModel(),
) {
    CompositionLocalProvider(LocalSounds provides rememberSounds()) {
        PlayContent(
            state = viewModel.uiState,
            actions =
                PlayActions(
                    onTap = { cell -> viewModel.onAction(GameAction.Toggle(cell)) },
                    onReset = { viewModel.onAction(GameAction.Reset) },
                    onBack = onBack,
                    onScores = onScores,
                ),
            modifier = modifier,
        )
    }
}

// Android Studio's inspection reports this scope as unused although `maxWidth` and `maxHeight`
// are read below; the lint that runs in `check` does not. The annotation is here to keep the
// editor quiet, not to hide a gate finding.
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
internal fun PlayContent(
    state: PlayUiState,
    actions: PlayActions,
    modifier: Modifier = Modifier,
) {
    // A solved board keeps its shape but stops answering: no handler, so a square offers no tap
    // to anyone — a finger or TalkBack alike.
    val taps = if (state.board.isSolved) null else actions.onTap

    val sounds = LocalSounds.current

    // Only the move that creates a new attack is heard: a board that stays in trouble would
    // otherwise sound the alarm again on every move after the first.
    var attackedBefore by remember { mutableIntStateOf(state.board.piecesUnderAttack) }
    LaunchedEffect(state.board.piecesUnderAttack) {
        val attacked = state.board.piecesUnderAttack
        if (attacked > attackedBefore) sounds.play(PlaySound.CONFLICT)
        attackedBefore = attacked
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TopBar(state = state, onReset = actions.onReset, onBack = actions.onBack)

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val floor = MinSquare * state.board.size + BoardInset * 2
                val beside = maxOf(floor, maxHeight - Spacing.lg * 2)

                if (maxWidth > maxHeight && maxWidth - beside - WideGutters >= MinSidePane) {
                    WideBody(state = state, onTap = taps, boardSide = beside)
                } else {
                    TallBody(state = state, onTap = taps, boardSide = maxOf(floor, maxWidth - Spacing.lg * 2))
                }
            }
        }

        if (state.board.isSolved) {
            WinCard(
                win =
                    Win(
                        puzzle = state.puzzle,
                        size = state.board.size,
                        seconds = state.elapsedSeconds,
                        previousBestSeconds = state.previousBestSeconds,
                    ),
                onPlayAgain = actions.onReset,
                onScores = actions.onScores,
            )
        }
    }
}

@Composable
private fun TallBody(
    state: PlayUiState,
    onTap: ((Cell) -> Unit)?,
    boardSide: Dp,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.xl))
        BoardSummary(size = state.board.size, puzzle = state.puzzle.name)

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            BoardCard(
                board = state.board,
                puzzle = state.puzzle,
                onTap = onTap,
                modifier = Modifier.size(boardSide),
            )
        }

        Spacer(Modifier.height(Spacing.lg))
        BoardFeedback(board = state.board, puzzle = state.puzzle)

        Spacer(Modifier.height(Spacing.lg))
    }
}

@Composable
private fun WideBody(
    state: PlayUiState,
    onTap: ((Cell) -> Unit)?,
    boardSide: Dp,
) {
    Row(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        BoardCard(
            board = state.board,
            puzzle = state.puzzle,
            onTap = onTap,
            modifier = Modifier.size(boardSide),
        )

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BoardSummary(state.board.size, state.puzzle.name)

            Spacer(Modifier.height(Spacing.md))

            BoardFeedback(board = state.board, puzzle = state.puzzle)
        }
    }
}

@Composable
private fun BoardSummary(
    size: Int,
    @StringRes puzzle: Int,
) {
    Text(
        text = stringResource(R.string.play_board_summary, size, stringResource(puzzle)),
        style = MaterialTheme.typography.labelSmall,
        color = PuzzleTheme.board.onSurfaceMuted,
    )
}

@Composable
private fun TapHint() {
    Text(
        text = stringResource(R.string.play_tap_hint),
        style = MaterialTheme.typography.labelSmall,
        color = PuzzleTheme.board.onSurfaceMuted,
    )
}

@Composable
private fun StatusStrip(
    conflicts: Int,
    puzzle: Puzzle,
    modifier: Modifier = Modifier,
) {
    val colors = PuzzleTheme.board
    val trouble = conflicts > 0

    val background = if (trouble) colors.conflictGlow else MaterialTheme.colorScheme.surface
    val outline = if (trouble) colors.conflict else colors.border
    val mark = if (trouble) colors.conflict else colors.hint
    val message =
        if (trouble) {
            pluralStringResource(puzzle.text.conflict, conflicts, conflicts)
        } else {
            stringResource(puzzle.text.idle)
        }

    val markSize = with(LocalDensity.current) { MarkSize.toDp() }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = StatusHeight)
                .clip(RoundedCornerShape(Radii.md))
                .background(background)
                .border(HairlineBorder, outline, RoundedCornerShape(Radii.md))
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(markSize).clip(CircleShape).background(mark),
            contentAlignment = Alignment.Center,
        ) {
            if (trouble) {
                Text(
                    text = "!",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
        Spacer(Modifier.width(Spacing.md))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = if (trouble) colors.conflict else colors.onSurfaceMuted,
        )
    }
}

@Composable
private fun BoardFeedback(
    board: BoardSnapshot,
    puzzle: Puzzle,
) {
    StatusStrip(conflicts = board.piecesUnderAttack, puzzle = puzzle)

    Spacer(Modifier.height(Spacing.lg))
    TapHint()
}
