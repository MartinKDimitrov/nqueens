package com.mdimitrov.nqueens.history.presentation

import com.mdimitrov.nqueens.history.FakeSolves
import com.mdimitrov.nqueens.history.domain.Solve
import com.mdimitrov.nqueens.history.domain.SolveRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class ScoresViewModelTest {
    private val clock = StandardTestDispatcher()

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun useATestClock() {
        Dispatchers.setMain(clock)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun releaseTheClock() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the boards arrive grouped by size, smallest first and fastest first inside`() {
        val solves = FakeSolves()
        solves.seed(
            solve(size = 8, seconds = 84, id = 1),
            solve(size = 4, seconds = 6, id = 2),
            solve(size = 8, seconds = 72, id = 3),
        )
        val viewModel = ScoresViewModel(solves)

        clock.scheduler.runCurrent()

        assertEquals(listOf(4, 8), viewModel.uiState.groups.map { it.size })
        assertEquals(listOf(72, 84), viewModel.uiState.groups.last().solves.map { it.seconds })
    }

    @Test
    fun `deleting a row takes it out of the list and leaves the rest`() {
        val solves = FakeSolves()
        solves.seed(solve(seconds = 84, id = 1), solve(seconds = 72, id = 2))
        val viewModel = ScoresViewModel(solves)
        clock.scheduler.runCurrent()

        viewModel.onDelete(1)
        clock.scheduler.runCurrent()

        assertEquals(listOf(2L), viewModel.uiState.groups.flatMap { it.solves }.map { it.id })
    }

    @Test
    fun `clearing empties the list`() {
        val solves = FakeSolves()
        solves.seed(solve(seconds = 84, id = 1), solve(size = 4, seconds = 6, id = 2))
        val viewModel = ScoresViewModel(solves)
        clock.scheduler.runCurrent()

        viewModel.onClearAll()
        clock.scheduler.runCurrent()

        assertTrue(viewModel.uiState.groups.isEmpty())
    }

    @Test
    fun `a card carries every solve of its size, fastest first`() {
        val solves = FakeSolves()
        solves.seed(*(1..8).map { solve(seconds = it * 10, id = it.toLong()) }.toTypedArray())
        val viewModel = ScoresViewModel(solves)

        clock.scheduler.runCurrent()

        val group = viewModel.uiState.groups.single()
        assertEquals(8, group.solves.size, "nothing is hidden: every one of them can be deleted")
        assertEquals((1..8).map { it * 10 }, group.solves.map { it.seconds })
    }

    @Test
    fun `a table that never comes back says so instead of claiming the records are gone`() {
        val table = FlakySolves(stumbles = 9)
        table.seed(solve(size = 4, seconds = 6, id = 1))
        val viewModel = ScoresViewModel(table)

        clock.scheduler.advanceTimeBy(10.seconds)

        assertTrue(viewModel.uiState.groups.isEmpty())
        assertFalse(viewModel.uiState.readable, "the screen must not read as an empty table")
    }

    @Test
    fun `a table that stumbles once is asked again rather than left for dead`() {
        val table = FlakySolves()
        table.seed(solve(size = 4, seconds = 6, id = 1))
        val viewModel = ScoresViewModel(table)

        clock.scheduler.runCurrent()
        assertTrue(viewModel.uiState.groups.isEmpty(), "the first read failed, so there is nothing yet")

        clock.scheduler.advanceTimeBy(1.5.seconds)
        assertEquals(listOf(4), viewModel.uiState.groups.map { it.size }, "and the second read arrives")
    }

    private fun solve(
        size: Int = 8,
        seconds: Int = 60,
        id: Long = 1,
    ) = Solve(
        size = size,
        variant = "queens",
        seconds = seconds,
        finishedAt = 0L,
        id = id,
    )
}

/** A table that fails the first time it is read, the way a locked database does. */
private class FlakySolves(
    private var stumbles: Int = 1,
    private val rows: FakeSolves = FakeSolves(),
) : SolveRepository by rows {
    fun seed(vararg solves: Solve) = rows.seed(*solves)

    override fun solves(): Flow<List<Solve>> =
        flow {
            if (stumbles-- > 0) error("the table is locked")
            emitAll(rows.solves())
        }
}
