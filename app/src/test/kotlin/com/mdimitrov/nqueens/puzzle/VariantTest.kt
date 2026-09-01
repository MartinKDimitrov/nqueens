package com.mdimitrov.nqueens.puzzle

import kotlin.test.Test
import kotlin.test.assertEquals

class VariantTest {
    @Test
    fun `the key a solved board is written under never moves`() {
        // Every row already in the table carries this string, and the best time is asked for a
        // size and a key together. Changing it orphans every board the player has solved, and
        // nothing else in the app would notice (TRADEOFFS D14).
        assertEquals("queens", Queens.key)
    }
}
