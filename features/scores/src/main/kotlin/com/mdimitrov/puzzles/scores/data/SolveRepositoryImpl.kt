package com.mdimitrov.puzzles.scores.data

import com.mdimitrov.puzzles.database.SolveDao
import com.mdimitrov.puzzles.database.SolveRow
import com.mdimitrov.puzzles.scores.domain.Solve
import com.mdimitrov.puzzles.scores.domain.SolveRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// The table is the only source there is so far; a second one joins here, not in the screens.
internal class SolveRepositoryImpl
    @Inject
    constructor(
        private val dao: SolveDao,
    ) : SolveRepository {
        override fun solves(): Flow<List<Solve>> = dao.all().map { rows -> rows.map { it.asSolve() } }

        override suspend fun add(solve: Solve) = dao.add(solve.asRow())

        override suspend fun delete(id: Long) = dao.delete(id)

        override suspend fun clear() = dao.clear()

        override suspend fun best(
            size: Int,
            puzzle: String,
        ): Int? = dao.best(size, puzzle)
    }

private fun SolveRow.asSolve() =
    Solve(
        size = size,
        puzzle = puzzle,
        seconds = seconds,
        finishedAt = finishedAt,
        id = id,
    )

private fun Solve.asRow() =
    SolveRow(
        size = size,
        puzzle = puzzle,
        seconds = seconds,
        finishedAt = finishedAt,
        id = id,
    )
