package com.mdimitrov.nqueens.game.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val TOLERANCE = 0.001f

class CelebrationTest {
    @Test
    fun `at the start there is nothing to see, and every piece is in the middle`() {
        ConfettiRest.forEach { rest ->
            val piece = confettiAt(progress = 0f, rest = rest)

            assertEquals(0f, piece.scale, TOLERANCE)
            assertEquals(0f, piece.alpha, TOLERANCE)
            assertEquals(0.5f, piece.x, TOLERANCE)
            assertEquals(0.5f, piece.y, TOLERANCE)
        }
    }

    @Test
    fun `by the end each piece is where the design puts it, grown, turned and gone`() {
        val rest = ConfettiRest.first()
        val piece = confettiAt(progress = 1f, rest = rest)

        assertEquals(rest.first, piece.x, TOLERANCE)
        assertEquals(rest.second, piece.y, TOLERANCE)
        assertEquals(1.15f, piece.scale, TOLERANCE)
        assertEquals(180f, piece.rotation, TOLERANCE)
        assertEquals(0f, piece.alpha, TOLERANCE)
    }

    @Test
    fun `the pieces are up and visible early, not at the end of the run`() {
        val piece = confettiAt(progress = 0.17f, rest = ConfettiRest.first())

        assertTrue(piece.scale > 0.9f, "the burst is in by a fifth of the way, was ${piece.scale}")
        assertTrue(piece.alpha > 0.5f, "and visible while it travels, was ${piece.alpha}")
    }

    @Test
    fun `a moment outside the celebration draws its nearest end`() {
        assertEquals(confettiAt(0f, ConfettiRest.first()), confettiAt(-1f, ConfettiRest.first()))
        assertEquals(confettiAt(1f, ConfettiRest.first()), confettiAt(4f, ConfettiRest.first()))
    }

    @Test
    fun `halfway through, a piece is clear of the card and still bright`() {
        val piece = confettiAt(progress = 0.5f, rest = ConfettiRest.first())

        assertTrue(piece.x < 0.25f, "the piece is out from behind the card by half time, was ${piece.x}")
        assertTrue(piece.alpha > 0.5f, "and still bright, was ${piece.alpha}")
    }

    @Test
    fun `the pieces leave in a spread, not in formation`() {
        val first = confettiAt(progress = 0.3f, rest = ConfettiRest.first(), lead = 0f)
        val last = confettiAt(progress = 0.3f, rest = ConfettiRest.first(), lead = 1f)

        assertTrue(first.scale > last.scale, "a piece that leads is ahead of one that follows")
        assertEquals(0f, last.scale, TOLERANCE)
    }
}
