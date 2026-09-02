package com.mdimitrov.puzzles.play.presentation.board

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val START = 0f
private const val SETTLED = 1f

private const val ARRIVING = 0.7f
private const val LANDED = 1f
private const val LANDING_TURNS = 1.5f
private const val SHAKE_TURNS = 1.5f

private const val LANDING_MILLIS = 260
private const val SHAKE_MILLIS = 300

/** How far a piece swings out of its square when it flinches. */
internal val ShakeWidth = 3.dp

/**
 * How big a piece is on its way down, from the moment it is placed to the moment it settles.
 *
 * It arrives at 70% of its size and overshoots to 114% a third of the way through, which is what
 * reads as weight; a piece that only grows to its size reads as a fade. The overshoot is damped
 * by the square of what is left, so it is exactly its own size at the end rather than near it.
 */
internal fun landingAt(progress: Float): Float {
    val left = SETTLED - progress.coerceIn(START, SETTLED)

    return LANDED - (LANDED - ARRIVING) * cos(2f * PI.toFloat() * LANDING_TURNS * (SETTLED - left)) * left * left
}

/**
 * How far a piece is thrown sideways while it flinches, as a fraction of [ShakeWidth].
 *
 * Three passes — right, left, right — each smaller than the last, ending where it started. The
 * damping is what stops it reading as a vibration: it reaches 0.84, then -0.51, then 0.19.
 */
internal fun shakeAt(progress: Float): Float {
    val t = progress.coerceIn(START, SETTLED)

    return sin(2f * PI.toFloat() * SHAKE_TURNS * t) * (SETTLED - t)
}

/** A piece's landing, run once when it arrives. One already standing does not land. */
@Composable
internal fun landingOf(standing: Boolean): Float {
    // A piece already standing when the board is composed starts settled, so animating it to
    // where it already is passes without a frame: only one that arrives afterward lands.
    val landing = remember { Animatable(if (standing) SETTLED else START) }

    LaunchedEffect(standing) {
        if (standing) {
            landing.animateTo(SETTLED, tween(LANDING_MILLIS, easing = LinearEasing))
        } else {
            landing.snapTo(START)
        }
    }

    return landingAt(landing.value)
}

/** A piece's flinch, run on the move that puts it under attack rather than while it lasts. */
@Composable
internal fun flinchOf(attacked: Boolean): Float {
    val flinch = remember { Animatable(SETTLED) }
    // A piece drawn already under attack did not just come under it, so the first composition
    // passes without a shake.
    var was by remember { mutableStateOf(attacked) }

    LaunchedEffect(attacked) {
        if (attacked && !was) {
            flinch.snapTo(START)
            flinch.animateTo(SETTLED, tween(SHAKE_MILLIS, easing = LinearEasing))
        } else {
            // An attack eased before the shake ends cancels it mid-swing, and a cancelled
            // Animatable keeps the value it had reached. Without this the piece stays crooked.
            flinch.snapTo(SETTLED)
        }
        was = attacked
    }

    return shakeAt(flinch.value)
}
