package com.mdimitrov.puzzles.scores.domain

/** Wall-clock time. It is an interface so that what stamps a record can be driven from a test. */
internal fun interface Clock {
    fun millis(): Long
}
