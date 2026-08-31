package com.mdimitrov.nqueens.history

import com.mdimitrov.nqueens.history.domain.Solve
import com.mdimitrov.nqueens.history.domain.SolveRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * A repository that keeps its rows in memory. `best` answers from the rows it holds, so a caller
 * asking for the wrong board — or asking after writing the row it is comparing against — gets a
 * different answer, which is the whole point of the question.
 */
internal class FakeSolves : SolveRepository {
    private val rows = MutableStateFlow(emptyList<Solve>())

    private var lastId = 0L

    val added: List<Solve> get() = rows.value

    /**
     * Seeds the way the table fills: a row keeps the id it is given, one without an id is handed
     * the next, and no id is ever handed out twice.
     */
    fun seed(vararg solves: Solve) {
        rows.value = emptyList()
        lastId = solves.maxOfOrNull { it.id } ?: 0
        solves.forEach { row -> rows.value += if (row.id == 0L) row.copy(id = ++lastId) else row }
    }

    // Newest first, as the table answers, so a test cannot pass here and fail against Room.
    override fun solves(): Flow<List<Solve>> =
        rows.map { list -> list.sortedWith(compareByDescending<Solve> { it.finishedAt }.thenByDescending { it.id }) }

    override suspend fun add(solve: Solve) {
        rows.value += solve.copy(id = ++lastId)
    }

    override suspend fun delete(id: Long) {
        rows.value = rows.value.filterNot { it.id == id }
    }

    override suspend fun clear() {
        rows.value = emptyList()
    }

    override suspend fun best(
        size: Int,
        variant: String,
    ): Int? =
        rows.value
            .filter { it.size == size && it.variant == variant }
            .minOfOrNull { it.seconds }
}
