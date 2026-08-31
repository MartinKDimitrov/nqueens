package com.mdimitrov.nqueens.format

import kotlin.test.Test
import kotlin.test.assertEquals

class ElapsedTest {
    @Test
    fun `a board just begun reads as no time at all`() {
        assertEquals("00:00", formatElapsed(0))
    }

    @Test
    fun `seconds and minutes are both padded`() {
        assertEquals("00:07", formatElapsed(7))
        assertEquals("01:00", formatElapsed(60))
        assertEquals("01:24", formatElapsed(84))
    }

    @Test
    fun `an hour keeps counting in minutes rather than starting over`() {
        assertEquals("60:00", formatElapsed(3_600))
        assertEquals("61:01", formatElapsed(3_661))
    }

    @Test
    fun `a time before the start reads as the start, never as a negative`() {
        assertEquals("00:00", formatElapsed(-1))
        assertEquals("00:00", formatElapsed(Int.MIN_VALUE))
    }
}
