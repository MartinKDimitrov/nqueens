package com.mdimitrov.nqueens.game.presentation

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
private const val TURNS = 1.5f

private const val LANDING_MILLIS = 260
private const val SHAKE_MILLIS = 300

/** How far a queen swings out of her square when she flinches. */
internal val ShakeWidth = 3.dp

/**
 * How big a queen is on her way down, from the moment she is placed to the moment she settles.
 *
 * She arrives at 70% of her size and overshoots to 114% a third of the way through, which is what
 * reads as weight; a piece that only grows to its size reads as a fade. The overshoot is damped
 * by the square of what is left, so she is exactly her own size at the end rather than near it.
 */
internal fun landingAt(progress: Float): Float {
    val left = SETTLED - progress.coerceIn(START, SETTLED)

    return LANDED - (LANDED - ARRIVING) * cos(2f * PI.toFloat() * TURNS * (SETTLED - left)) * left * left
}

/**
 * How far a queen is thrown sideways while she flinches, as a fraction of [ShakeWidth].
 *
 * Three passes — right, left, right — each smaller than the last, ending where she started. The
 * damping is what stops it reading as a vibration: 0.83, then 0.5, then 0.17.
 */
internal fun shakeAt(progress: Float): Float {
    val t = progress.coerceIn(START, SETTLED)

    return sin(2f * PI.toFloat() * TURNS * t) * (SETTLED - t)
}

/** A queen's landing, run once when she arrives. One who was already standing does not land. */
@Composable
internal fun landingOf(standing: Boolean): Float {
    // A queen already standing when the board is composed starts settled, so animating her to
    // where she already is passes without a frame: only one who arrives afterward lands.
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

/** A queen's flinch, run on the move that puts her under attack rather than while it lasts. */
@Composable
internal fun flinchOf(attacked: Boolean): Float {
    val flinch = remember { Animatable(SETTLED) }
    // A queen drawn already under attack did not just come under it, so the first composition
    // passes without a shake.
    var was by remember { mutableStateOf(attacked) }

    LaunchedEffect(attacked) {
        if (attacked && !was) {
            flinch.snapTo(START)
            flinch.animateTo(SETTLED, tween(SHAKE_MILLIS, easing = LinearEasing))
        }
        was = attacked
    }

    return shakeAt(flinch.value)
}
