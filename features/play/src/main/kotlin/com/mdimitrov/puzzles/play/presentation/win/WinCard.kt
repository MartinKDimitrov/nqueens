package com.mdimitrov.puzzles.play.presentation.win

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
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mdimitrov.puzzles.format.formatElapsed
import com.mdimitrov.puzzles.play.R
import com.mdimitrov.puzzles.play.presentation.sound.LocalSounds
import com.mdimitrov.puzzles.play.presentation.sound.PlaySound
import com.mdimitrov.puzzles.puzzletype.Puzzle
import com.mdimitrov.puzzles.theme.Elevation
import com.mdimitrov.puzzles.theme.PuzzleTheme
import com.mdimitrov.puzzles.theme.Radii
import com.mdimitrov.puzzles.theme.Spacing
import com.mdimitrov.puzzles.theme.TouchTarget

private const val SCRIM_ALPHA = 0.55f
private const val BADGE_ALPHA = 0.15f
private val BADGE_SIZE = 92.dp
private val BADGE_GLYPH = 48.dp
private val CARD_WIDTH = 360.dp

/**
 * What the card has to say: which puzzle was solved, on what board, in what time, and against what
 * best.
 *
 * Its own type rather than the screen's whole state, which this package would otherwise have to
 * read from the one that draws it — the single edge pointing back up. `board/` keeps the same rule
 * by a different means: what it draws from is `BoardSnapshot`, which is the domain's and not the
 * screen's.
 */
internal data class Win(
    val puzzle: Puzzle,
    val size: Int,
    val seconds: Int,
    val previousBestSeconds: Int?,
) {
    /** By how much this board beat the one before it, or null when it did not. A tie is not a best. */
    val betterBy: Int? get() = previousBestSeconds?.minus(seconds)?.takeIf { it > 0 }
}

/**
 * Where the card says a result out loud.
 *
 * The window's own reader, always, in the app — the local exists because a test cannot use it: a
 * screen reader has to be switched on for `announceForAccessibility` to do anything, and Compose
 * keeps accessibility machinery running for as long as one is, which no test can switch off again.
 * Two of the board's animation tests never reach a quiet composition after it.
 */
internal fun interface Announcer {
    fun say(text: String)
}

internal val LocalAnnouncer: ProvidableCompositionLocal<Announcer?> = staticCompositionLocalOf { null }

/** The solved board under the scrim, the celebration over it, the card over both. */
@Composable
internal fun WinCard(
    win: Win,
    onPlayAgain: () -> Unit,
    onScores: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The card arrives on its own, so it is also felt on its own — once. The composition is
    // rebuilt by a rotation and by the trip to the records and back, while the win it announces
    // happened only the first time, so the flag outlives the composition rather than the board.
    val haptics = LocalHapticFeedback.current
    val sounds = LocalSounds.current
    val view = LocalView.current
    val announcer = LocalAnnouncer.current ?: remember(view) { Announcer(view::announceForAccessibility) }
    var announced by rememberSaveable { mutableStateOf(false) }

    var toldTheBest by rememberSaveable { mutableStateOf(false) }

    val summary = stringResource(R.string.play_board_summary, win.size, stringResource(win.puzzle.name))
    val announcement = stringResource(R.string.play_solved_announcement, summary, formatElapsed(win.seconds))
    val best = win.betterBy?.let { stringResource(R.string.play_new_best, it) }

    // Said on its own, and after the rest: the table is still being asked when this card arrives,
    // so what it beat is not known yet and cannot be part of the sentence above. A sighted player
    // watches the line appear; without this nobody else is told it did.
    LaunchedEffect(best) {
        if (best != null && !toldTheBest) {
            toldTheBest = true
            announcer.say(best)
        }
    }

    LaunchedEffect(Unit) {
        if (!announced) {
            announced = true
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            sounds.play(PlaySound.WIN)
            // Spoken by asking the window rather than by the live region below, which cannot do
            // it: a live region is announced when a node the reader already knows about changes,
            // and this card and its region are born in the same frame as the win they announce.
            // The region stays for the reader that arrives at the card later; this is for the
            // player who has just placed the last piece and would otherwise hear nothing at all
            // while the board goes quiet and every control around it turns itself off.
            announcer.say(announcement)
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
            WinBody(
                win = win,
                summary = summary,
                announcement = announcement,
                onPlayAgain = onPlayAgain,
                onScores = onScores,
            )
        }
    }
}

@Composable
private fun WinBody(
    win: Win,
    summary: String,
    announcement: String,
    onPlayAgain: () -> Unit,
    onScores: () -> Unit,
) {
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
        PieceBadge(piece = win.puzzle.piece)

        Text(
            text = stringResource(R.string.play_solved),
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
            color = PuzzleTheme.board.onSurfaceMuted,
            modifier = Modifier.padding(top = Spacing.xs),
        )

        FinishingTime(seconds = win.seconds)
        NewBest(betterBy = win.betterBy)

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
            text = stringResource(R.string.play_play_again),
            style = MaterialTheme.typography.labelLarge,
        )
    }
    TextButton(
        onClick = onScores,
        modifier = Modifier.padding(top = Spacing.xs).heightIn(min = TouchTarget),
    ) {
        Text(
            text = stringResource(R.string.play_view_scores),
            style = MaterialTheme.typography.labelLarge,
            color = PuzzleTheme.board.onSurfaceMuted,
        )
    }
}

@Composable
private fun PieceBadge(
    @DrawableRes piece: Int,
) {
    val success = PuzzleTheme.board.success

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
                .background(PuzzleTheme.board.surfaceAlt)
                .padding(vertical = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.play_finishing_time),
            style = MaterialTheme.typography.labelSmall,
            color = PuzzleTheme.board.onSurfaceMuted,
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
private fun NewBest(betterBy: Int?) {
    if (betterBy == null) return

    Text(
        text = stringResource(R.string.play_new_best, betterBy),
        style = MaterialTheme.typography.labelLarge,
        color = PuzzleTheme.board.success,
        modifier = Modifier.padding(top = Spacing.sm),
    )
}
