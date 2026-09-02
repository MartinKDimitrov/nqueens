package com.mdimitrov.puzzles.play.presentation.board

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val AT_REST = 1f
private const val TOLERANCE = 0.001f

// Far enough into the three hundred the shake takes for the queen to be well off centre.
private const val MID_SWING = 64L

@RunWith(RobolectricTestRunner::class)
class SquareMotionAnimationTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `a queen who arrives lands, and one already standing does not`() {
        val standing = mutableStateOf(true)
        val sizes = mutableListOf<Float>()
        compose.setContent { sizes += landingOf(standing.value) }
        compose.waitForIdle()

        assertEquals(listOf(AT_REST), sizes.distinct(), "a queen already standing landed again")

        sizes.clear()
        compose.runOnIdle { standing.value = false }
        compose.runOnIdle { standing.value = true }
        compose.waitForIdle()

        assertTrue(sizes.max() > AT_REST, "she never overshot her own size: ${sizes.distinct()}")
        assertEquals(AT_REST, sizes.last(), TOLERANCE, "she did not settle at her own size")
    }

    @Test
    fun `a flinch cut short by the trouble ending leaves the queen where she stands`() {
        val attacked = mutableStateOf(false)
        val throws = mutableListOf<Float>()

        compose.setContent {
            throws += flinchOf(attacked.value)
            // The trouble is made and eased from inside the composition's own clock, so the
            // shake is cancelled mid-swing the way a fast pair of taps cancels it.
            LaunchedEffect(Unit) {
                attacked.value = true
                delay(MID_SWING)
                attacked.value = false
            }
        }
        compose.waitForIdle()

        assertTrue(throws.any { abs(it) > TOLERANCE }, "she never moved: ${throws.distinct()}")
        assertTrue(abs(throws.last()) < TOLERANCE, "she was left ${throws.last()} out of her square")
    }

    @Test
    fun `a queen flinches on the move that attacks her, not while the attack lasts`() {
        val attacked = mutableStateOf(true)
        val throws = mutableListOf<Float>()
        compose.setContent { throws += flinchOf(attacked.value) }
        compose.waitForIdle()

        assertTrue(throws.all { abs(it) < TOLERANCE }, "a queen drawn already under attack flinched")

        throws.clear()
        compose.runOnIdle { attacked.value = false }
        compose.runOnIdle { attacked.value = true }
        compose.waitForIdle()

        assertTrue(throws.any { it > TOLERANCE }, "she never swung one way: ${throws.distinct()}")
        assertTrue(throws.any { it < -TOLERANCE }, "she never swung the other: ${throws.distinct()}")
        assertTrue(abs(throws.last()) < TOLERANCE, "she did not come back to where she stands")
    }
}
