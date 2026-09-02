package com.mdimitrov.puzzles.scores.data

import com.mdimitrov.puzzles.scores.domain.Clock
import com.mdimitrov.puzzles.scores.domain.Solve
import com.mdimitrov.puzzles.scores.domain.SolveRepository
import com.mdimitrov.puzzles.solves.RecordSolve
import com.mdimitrov.puzzles.solves.SolvedBoard
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/**
 * This feature answering for every game that finishes a board: it stamps the moment, keeps
 * the row and hands back what the best was before it.
 */
internal class SolveRecorder
    @Inject
    constructor(
        private val solves: SolveRepository,
        private val clock: Clock,
    ) : RecordSolve {
        @Suppress("SwallowedException", "TooGenericExceptionCaught")
        override suspend fun record(board: SolvedBoard): Int? {
            // A read that fails costs the comparison and nothing else: the row is what matters
            // and is written either way.
            val best =
                try {
                    solves.best(board.size, board.puzzle)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (refused: Exception) {
                    null
                }

            solves.add(
                Solve(
                    size = board.size,
                    puzzle = board.puzzle,
                    seconds = board.seconds,
                    finishedAt = clock.millis(),
                ),
            )

            return best
        }
    }
