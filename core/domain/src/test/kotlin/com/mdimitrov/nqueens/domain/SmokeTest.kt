package com.mdimitrov.nqueens.domain

import kotlin.test.Test
import kotlin.test.assertTrue

/** Sanity check that the domain module's test toolchain is wired before real tests arrive. */
class SmokeTest {
    @Test
    fun toolchainRuns() {
        assertTrue(true)
    }
}
