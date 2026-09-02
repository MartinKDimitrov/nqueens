package com.mdimitrov.puzzles.play.presentation.win

import kotlin.math.abs
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
    fun `a piece turns all the way through the flight, not only at the end`() {
        val quarter = confettiAt(progress = 0.25f, rest = ConfettiRest.first())
        val half = confettiAt(progress = 0.5f, rest = ConfettiRest.first())

        assertTrue(quarter.rotation > 0f, "it is already turning early, was ${quarter.rotation}")
        assertTrue(half.rotation > quarter.rotation, "and keeps turning")
        assertTrue(half.rotation < 180f, "the half turn belongs to the end")
    }

    @Test
    fun `a piece covers more ground in the first half of its flight than in the second`() {
        // The piece that leaves first finishes at 0.7 of the whole celebration, so 0.35 is its
        // own halfway. An even journey would cover the same ground either side of it.
        val rest = ConfettiRest.first()
        val start = confettiAt(progress = 0f, rest = rest).x
        val half = confettiAt(progress = 0.35f, rest = rest).x
        val end = confettiAt(progress = 0.7f, rest = rest).x

        assertTrue(
            abs(half - start) > abs(end - half),
            "out fast, settling late: ${abs(half - start)} then ${abs(end - half)}",
        )
    }

    @Test
    fun `a piece grows over the flight rather than arriving grown`() {
        val entered = confettiAt(progress = 0.2f, rest = ConfettiRest.first())
        val late = confettiAt(progress = 0.8f, rest = ConfettiRest.first())

        assertTrue(entered.scale < late.scale, "it is still growing at 0.2: ${entered.scale}")
        assertTrue(entered.scale < 1.05f, "and has not arrived grown")
    }

    @Test
    fun `a piece stays bright most of the way and goes out only at the end`() {
        val rest = ConfettiRest.first()
        val ownFlight = 0.7f

        assertEquals(1f, confettiAt(ownFlight * 0.55f, rest).alpha, TOLERANCE)
        assertTrue(
            confettiAt(ownFlight * 0.8f, rest).alpha < 0.6f,
            "past the turn it is going out, was ${confettiAt(ownFlight * 0.8f, rest).alpha}",
        )
        assertEquals(0f, confettiAt(ownFlight, rest).alpha, TOLERANCE)
    }

    @Test
    fun `the pieces are given their places in the queue, first to last`() {
        val leads = ConfettiRest.indices.map { leadOf(it, ConfettiRest.size) }

        assertEquals(0f, leads.first(), TOLERANCE, "the first piece leaves at once")
        assertEquals(1f, leads.last(), TOLERANCE, "the last one waits the longest")
        assertEquals(leads.sorted(), leads, "and everything between them is in order")
        assertEquals(0f, leadOf(index = 0, pieces = 1), TOLERANCE, "a lone piece leaves at once")
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
