package com.mdimitrov.nqueens.game.presentation

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val TOLERANCE = 0.001f
private const val SAMPLES = 1_000

class SquareMotionTest {
    @Test
    fun `a queen arrives smaller than she ends, and ends exactly her own size`() {
        assertEquals(0.7f, landingAt(0f), TOLERANCE)
        assertEquals(1f, landingAt(1f), TOLERANCE)
    }

    @Test
    fun `she overshoots on the way, so the landing has weight`() {
        val sizes = (0..SAMPLES).map { landingAt(it.toFloat() / SAMPLES) }

        assertTrue(sizes.max() > 1.1f, "the largest she gets is ${sizes.max()}")
        assertTrue(sizes.min() >= 0.7f - TOLERANCE, "the smallest she gets is ${sizes.min()}")
    }

    @Test
    fun `a moment either side of the landing is the landing's own end`() {
        assertEquals(landingAt(0f), landingAt(-1f), TOLERANCE)
        assertEquals(landingAt(1f), landingAt(2f), TOLERANCE)
    }

    @Test
    fun `the flinch starts and ends where the queen stands`() {
        assertEquals(0f, shakeAt(0f), TOLERANCE)
        assertEquals(0f, shakeAt(1f), TOLERANCE)
    }

    @Test
    fun `each pass of the flinch is smaller than the one before`() {
        val passes = listOf(1f / 6, 1f / 2, 5f / 6).map { abs(shakeAt(it)) }

        assertTrue(passes[0] > passes[1], "the first pass ${passes[0]} against the second ${passes[1]}")
        assertTrue(passes[1] > passes[2], "the second pass ${passes[1]} against the third ${passes[2]}")
    }

    @Test
    fun `the flinch never leaves its own width, either side of its ends`() {
        val throws = (0..SAMPLES).map { shakeAt(it.toFloat() / SAMPLES) }

        assertTrue(throws.all { abs(it) <= 1f }, "the furthest it goes is ${throws.maxOf { abs(it) }}")
        assertEquals(0f, shakeAt(-1f), TOLERANCE)
        assertEquals(0f, shakeAt(2f), TOLERANCE)
    }
}
