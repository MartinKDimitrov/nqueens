package com.mdimitrov.nqueens.history

import com.mdimitrov.nqueens.history.domain.Solve
import com.mdimitrov.nqueens.history.domain.SolveRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** A repository that keeps its rows in memory and hands out ids the way the table does. */
internal class FakeSolves : SolveRepository {
    private val rows = MutableStateFlow(emptyList<Solve>())

    var fastest: Int? = null

    val added: List<Solve> get() = rows.value

    fun seed(vararg solves: Solve) {
        rows.value = solves.toList()
    }

    override fun solves(): Flow<List<Solve>> = rows

    override suspend fun add(solve: Solve) {
        rows.value += solve.copy(id = rows.value.size + 1L)
    }

    override suspend fun delete(id: Long) {
        rows.value = rows.value.filterNot { it.id == id }
    }

    override suspend fun clear() {
        rows.value = emptyList()
    }

    override suspend fun best(size: Int): Int? = fastest
}
