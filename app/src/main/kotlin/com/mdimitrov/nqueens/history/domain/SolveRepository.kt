package com.mdimitrov.nqueens.history.domain

import kotlinx.coroutines.flow.Flow

/**
 * Everything the feature needs of the boards it has seen solved. An abandoned game is not a
 * result and is not kept.
 *
 * This is what a view model is given. Where the records come from — a table today, a table and
 * a server tomorrow — is settled behind this interface and changes nothing in front of it.
 */
internal interface SolveRepository {
    /** Newest first. The flow reports the list again whenever what it read changes. */
    fun solves(): Flow<List<Solve>>

    suspend fun add(solve: Solve)

    suspend fun delete(id: Long)

    suspend fun clear()

    /** The fastest solve of a board this size, or null while no board of that size is finished. */
    suspend fun best(size: Int): Int?
}
