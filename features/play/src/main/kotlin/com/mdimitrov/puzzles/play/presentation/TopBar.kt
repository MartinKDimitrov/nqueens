package com.mdimitrov.puzzles.play.presentation

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import com.mdimitrov.puzzles.format.formatElapsed
import com.mdimitrov.puzzles.play.R
import com.mdimitrov.puzzles.theme.HairlineBorder
import com.mdimitrov.puzzles.theme.PuzzleTheme
import com.mdimitrov.puzzles.theme.Radii
import com.mdimitrov.puzzles.theme.Spacing
import com.mdimitrov.puzzles.theme.TouchTarget

private val GlyphSize = 22.dp
private val PillHeight = 64.dp

// Below this window width the three pills and the back button cannot share a line without the
// clock breaking across two: at 320 dp each pill is about 58 dp wide and the clock needs 62.
// Measured at the default font size — at the largest accessibility fonts the clock takes two
// lines whatever the layout, which the pills' minimum height leaves room for.
private val NarrowBar = 340.dp

// Android Studio reports the scope as unused although `maxWidth` is read on the first line
// inside it; the lint that runs in `check` does not. The annotation keeps the editor quiet, as
// it does on `PlayContent`, rather than hiding a gate finding.
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
internal fun TopBar(
    state: PlayUiState,
    onReset: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PuzzleTheme.board

    Column(modifier = modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        // The width the bar is measured against is the window's, not what is left after its own
        // padding, so the threshold means what it says.
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val narrow = maxWidth < NarrowBar

            Box(modifier = Modifier.padding(Spacing.lg)) {
                if (narrow) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        BackButton(state = state, onBack = onBack)
                        Pills(state = state, onReset = onReset)
                    }
                } else {
                    Row(
                        modifier = Modifier.height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BackButton(state = state, onBack = onBack)
                        Pills(state = state, onReset = onReset)
                    }
                }
            }
        }
        HorizontalDivider(thickness = HairlineBorder, color = colors.border)
    }
}

/**
 * The controls answer only while the board is being played. A solved board is behind the win
 * card, which stops a finger by covering them — but not a screen reader, which would otherwise
 * activate them through the card and throw the player out of the game they just finished.
 */
@Composable
private fun BackButton(
    state: PlayUiState,
    onBack: () -> Unit,
) {
    val colors = PuzzleTheme.board
    val back = stringResource(R.string.play_back)

    Box(
        modifier =
            Modifier
                .size(TouchTarget)
                .clip(RoundedCornerShape(Radii.md))
                .background(MaterialTheme.colorScheme.surface)
                .border(HairlineBorder, colors.border, RoundedCornerShape(Radii.md))
                .clickable(enabled = !state.board.isSolved, onClick = onBack)
                .semantics { contentDescription = back },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.play_ic_arrow_back),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(GlyphSize),
        )
    }
}

@Composable
private fun Pills(
    state: PlayUiState,
    onReset: () -> Unit,
) {
    val colors = PuzzleTheme.board
    val reset = stringResource(R.string.play_reset)

    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Pill(
            label = stringResource(state.puzzle.text.counter),
            value = stringResource(R.string.play_counter_value, state.board.piecesLeft),
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )

        Pill(
            label = stringResource(R.string.play_time),
            value = formatElapsed(state.elapsedSeconds),
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )

        Pill(
            label = reset,
            icon = R.drawable.play_ic_reset,
            background = colors.surfaceAlt,
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(enabled = !state.board.isSolved, onClick = onReset)
                    .semantics { contentDescription = reset },
        )
    }
}

@Composable
private fun Pill(
    label: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    @DrawableRes icon: Int? = null,
    background: Color = MaterialTheme.colorScheme.surface,
) {
    val colors = PuzzleTheme.board

    Column(
        modifier =
            modifier
                .heightIn(min = PillHeight)
                .clip(RoundedCornerShape(Radii.md))
                .background(background)
                .border(HairlineBorder, colors.border, RoundedCornerShape(Radii.md))
                .padding(vertical = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceMuted,
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(GlyphSize),
            )
        }
    }
}
