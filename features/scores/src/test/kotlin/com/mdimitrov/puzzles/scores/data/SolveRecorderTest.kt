package com.mdimitrov.puzzles.scores.data

import com.mdimitrov.puzzles.scores.FakeSolves
import com.mdimitrov.puzzles.scores.domain.Clock
import com.mdimitrov.puzzles.scores.domain.Solve
import com.mdimitrov.puzzles.scores.domain.SolveRepository
import com.mdimitrov.puzzles.solves.SolvedBoard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val QUEENS = "queens"

class SolveRecorderTest {
    @Test
    fun `a board is written down with the moment it was finished`() =
        runTest {
            val solves = FakeSolves()
            val recorder = SolveRecorder(solves, Clock { 1_700L })

            recorder.record(SolvedBoard(QUEENS, size = 4, seconds = 2))

            val record = solves.rows.single()
            assertEquals(4, record.size)
            assertEquals(QUEENS, record.puzzle)
            assertEquals(2, record.seconds)
            assertEquals(1_700L, record.finishedAt)
        }

    @Test
    fun `the row carries the puzzle it was given, not the one this table happens to hold`() =
        runTest {
            val solves = FakeSolves()
            val recorder = SolveRecorder(solves, Clock { 0L })

            recorder.record(SolvedBoard("another-puzzle", size = 4, seconds = 2))

            assertEquals("another-puzzle", solves.rows.single().puzzle)
        }

    @Test
    fun `the answer is the best from before this board, not counting it`() =
        runTest {
            val solves = FakeSolves()
            solves.seed(Solve(size = 4, puzzle = QUEENS, seconds = 90, finishedAt = 0L))
            val recorder = SolveRecorder(solves, Clock { 0L })

            assertEquals(90, recorder.record(SolvedBoard(QUEENS, size = 4, seconds = 2)))
            assertEquals(2, recorder.record(SolvedBoard(QUEENS, size = 4, seconds = 5)))
        }

    @Test
    fun `a board of another puzzle is not what this one is compared against`() =
        runTest {
            val solves = FakeSolves()
            solves.seed(Solve(size = 4, puzzle = "another-puzzle", seconds = 5, finishedAt = 0L))
            val recorder = SolveRecorder(solves, Clock { 0L })

            assertNull(recorder.record(SolvedBoard(QUEENS, size = 4, seconds = 90)))
        }

    @Test
    fun `a refused read costs the comparison, never the record`() =
        runTest {
            val table = RefusingReads()
            val recorder = SolveRecorder(table, Clock { 0L })

            assertNull(recorder.record(SolvedBoard(QUEENS, size = 4, seconds = 2)))
            assertEquals(1, table.rows.size, "the board is written down even though the read failed")
        }

    @Test
    fun `a read the caller gave up on leaves no row behind`() =
        runTest {
            // Cancellation is not refusal, and telling them apart is what the rethrow is for.
            // Swallowed, it becomes a comparison the recorder failed to make and it writes the row
            // regardless — a board added to the table on behalf of a game whose player is gone.
            val table = InterruptedReads()
            val recorder = SolveRecorder(table, Clock { 0L })

            assertFailsWith<CancellationException> {
                recorder.record(SolvedBoard(QUEENS, size = 4, seconds = 2))
            }
            assertTrue(table.rows.isEmpty(), "a board was written down for a game nobody was waiting on")
        }
}

/** A table whose read is interrupted, the way a cancelled caller interrupts one. */
private class InterruptedReads(
    private val table: FakeSolves = FakeSolves(),
) : SolveRepository by table {
    val rows: List<Solve> get() = table.rows

    override suspend fun best(
        size: Int,
        puzzle: String,
    ): Int? = throw CancellationException("the caller has gone")
}

/** A table that refuses to be read, the way a locked one does, but takes what it is given. */
private class RefusingReads(
    private val table: FakeSolves = FakeSolves(),
) : SolveRepository by table {
    val rows: List<Solve> get() = table.rows

    override suspend fun best(
        size: Int,
        puzzle: String,
    ): Int? = error("the table is locked")
}
