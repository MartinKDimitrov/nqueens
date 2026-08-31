package com.mdimitrov.nqueens.game.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.mdimitrov.nqueens.theme.NQueensTheme

internal const val CELEBRATION_TAG = "celebration"

// The celebration design/screens/win.svg sketches as a still: a burst that is up within the
// first sixth of a piece's flight and gone by the end. It is a function of one number, so what
// is drawn can be replaced without touching what decides when to draw it (TRADEOFFS D6).
private const val CELEBRATION_MILLIS = 3_400
private const val STAGGER = 0.3f
private const val FADE_FROM = 0.6f
private const val ENTRY_PART = 0.17f
private const val MIDDLE = 0.5f
private const val GROWN_SCALE = 1.15f
private const val HALF_TURN = 180f
private val PieceSide = 14.dp
private val PieceCorner = 2.dp

/**
 * Where the pieces come to rest, in fractions of the screen. The first six are the ones
 * design/screens/win.svg draws; the rest are here because six read as a diagram once they move.
 */
internal val ConfettiRest =
    listOf(
        0.14f to 0.18f,
        0.82f to 0.26f,
        0.22f to 0.76f,
        0.76f to 0.82f,
        0.50f to 0.12f,
        0.38f to 0.88f,
        0.08f to 0.42f,
        0.92f to 0.58f,
        0.30f to 0.06f,
        0.66f to 0.94f,
        0.06f to 0.66f,
        0.94f to 0.34f,
        0.24f to 0.34f,
        0.72f to 0.10f,
        0.18f to 0.94f,
        0.86f to 0.72f,
        0.46f to 0.96f,
        0.58f to 0.04f,
    )

/** One piece of confetti at one moment of the celebration. */
internal data class ConfettiPiece(
    val x: Float,
    val y: Float,
    val scale: Float,
    val alpha: Float,
    val rotation: Float,
)

/**
 * A piece at `progress`, which runs 0..1 over the whole celebration: out from the middle towards
 * where it belongs, growing and turning, and gone by the end. `lead` is 0 for a piece that leaves
 * first and 1 for the last one, so the burst arrives in a spread rather than in formation.
 * Anything outside 0..1 is clamped, so a frame that arrives late draws the end rather than
 * something impossible.
 */
internal fun confettiAt(
    progress: Float,
    rest: Pair<Float, Float>,
    lead: Float = 0f,
): ConfettiPiece {
    val t = ((progress - lead.coerceIn(0f, 1f) * STAGGER) / (1f - STAGGER)).coerceIn(0f, 1f)
    val entry = (t / ENTRY_PART).coerceIn(0f, 1f)
    // Out fast, so the pieces clear the card while they are still bright; the card sits in the
    // middle, and a piece that travels evenly spends its brightest moment behind it.
    val travel = 1f - (1f - t) * (1f - t)
    val fade = ((1f - t) / (1f - FADE_FROM)).coerceAtMost(1f)

    return ConfettiPiece(
        x = MIDDLE + (rest.first - MIDDLE) * travel,
        y = MIDDLE + (rest.second - MIDDLE) * travel,
        scale = entry * (1f + (GROWN_SCALE - 1f) * t),
        alpha = entry * fade,
        rotation = HALF_TURN * t,
    )
}

/**
 * How far behind the first piece the one at `index` leaves: 0 for the first, 1 for the last.
 * A single piece leaves at once rather than dividing by nothing.
 */
internal fun leadOf(
    index: Int,
    pieces: Int,
): Float = if (pieces < 2) 0f else index.toFloat() / (pieces - 1)

@Composable
internal fun Celebration(modifier: Modifier = Modifier) {
    val colors = NQueensTheme.board
    val paints = listOf(colors.hint, MaterialTheme.colorScheme.primary, colors.success)
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(CELEBRATION_MILLIS, easing = LinearOutSlowInEasing),
        )
    }

    Canvas(modifier = modifier.testTag(CELEBRATION_TAG)) {
        ConfettiRest.forEachIndexed { index, rest ->
            val lead = leadOf(index, ConfettiRest.size)
            val piece = confettiAt(progress.value, rest, lead)
            val side = PieceSide.toPx() * piece.scale
            val centre = Offset(piece.x * size.width, piece.y * size.height)

            rotate(degrees = piece.rotation, pivot = centre) {
                drawRoundRect(
                    color = paints[index % paints.size],
                    topLeft = Offset(centre.x - side / 2, centre.y - side / 2),
                    size = Size(side, side),
                    cornerRadius = CornerRadius(PieceCorner.toPx()),
                    alpha = piece.alpha,
                )
            }
        }
    }
}
