package com.mdimitrov.nqueens.history.domain

/**
 * One board that was solved: what it was, how long it took and when it was finished.
 *
 * The variant is the puzzle's own key — `Variant.key` — never a resource id (TRADEOFFS D14).
 * An `id` of zero is a solve nobody has written yet; the table hands out the real one.
 */
internal data class Solve(
    val size: Int,
    val variant: String,
    val seconds: Int,
    val finishedAt: Long,
    val id: Long = 0,
)
