package com.mdimitrov.puzzles.theme

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class DimensTest {
    @Test
    fun `a control is at least 48 dp to a finger`() {
        // Every screen asserts its controls against `TouchTarget` rather than against a number,
        // so without this the suite would prove "at least whatever the constant says" while
        // `docs/PROJECT.md` §2 claims 48 dp. This is where the number itself is held.
        assertEquals(48.dp, TouchTarget, "the floor the accessibility guidelines set")
    }
}
