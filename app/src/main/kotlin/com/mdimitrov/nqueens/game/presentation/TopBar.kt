package com.mdimitrov.nqueens.game.presentation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.mdimitrov.nqueens.R
import com.mdimitrov.nqueens.format.formatElapsed
import com.mdimitrov.nqueens.theme.HairlineBorder
import com.mdimitrov.nqueens.theme.NQueensTheme
import com.mdimitrov.nqueens.theme.Radii
import com.mdimitrov.nqueens.theme.Spacing

private val GlyphSize = 22.dp
private val PillHeight = 64.dp
private val BackButtonSide = 48.dp

@Composable
internal fun TopBar(
    state: GameUiState,
    onReset: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NQueensTheme.board
    val back = stringResource(R.string.game_back)

    Column(modifier = modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(BackButtonSide)
                        .clip(RoundedCornerShape(Radii.md))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(HairlineBorder, colors.border, RoundedCornerShape(Radii.md))
                        .clickable(onClick = onBack)
                        .semantics { contentDescription = back },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(GlyphSize),
                )
            }

            Pill(
                label = stringResource(state.variant.text.counter),
                value = stringResource(R.string.game_counter_value, state.board.piecesLeft),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )

            Pill(
                label = stringResource(R.string.game_time),
                value = formatElapsed(state.elapsedSeconds),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )

            val reset = stringResource(R.string.game_reset)

            Pill(
                label = reset,
                icon = R.drawable.ic_reset,
                background = colors.surfaceAlt,
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(onClick = onReset)
                        .semantics { contentDescription = reset },
            )
        }
        HorizontalDivider(thickness = HairlineBorder, color = colors.border)
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
    val colors = NQueensTheme.board

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
