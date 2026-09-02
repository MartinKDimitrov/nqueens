package com.mdimitrov.puzzles.scores.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.mdimitrov.puzzles.scores.FakeSolves
import com.mdimitrov.puzzles.scores.domain.Solve
import com.mdimitrov.puzzles.scores.domain.SolveRepository
import kotlinx.coroutines.CoroutineScope
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

    // What the app provides: a scope that outlives the screen, so a delete a player leaves
    // behind still reaches the table. On the test clock, so a test can see it land.
    private val writes = CoroutineScope(clock)

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
        val viewModel = ScoresViewModel(solves, writes)

        clock.scheduler.runCurrent()

        assertEquals(listOf(4, 8), viewModel.uiState.groups.map { it.size })
        assertEquals(listOf(72, 84), viewModel.uiState.groups.last().solves.map { it.seconds })
    }

    @Test
    fun `an empty table is an answer, not a table that has not answered`() {
        val viewModel = ScoresViewModel(FakeSolves(), writes)

        assertFalse(viewModel.uiState.answered, "nothing is claimed before the table speaks")

        clock.scheduler.runCurrent()

        // The screen shows "Nothing solved yet." only once this is true; without it a player
        // with an empty table waits at a blank list for ever.
        assertTrue(viewModel.uiState.answered)
        assertTrue(viewModel.uiState.groups.isEmpty())
    }

    @Test
    fun `deleting a row takes it out of the list and leaves the rest`() {
        val solves = FakeSolves()
        solves.seed(solve(seconds = 84, id = 1), solve(seconds = 72, id = 2))
        val viewModel = ScoresViewModel(solves, writes)
        clock.scheduler.runCurrent()

        viewModel.onDelete(1)
        clock.scheduler.runCurrent()

        assertEquals(listOf(2L), viewModel.uiState.groups.flatMap { it.solves }.map { it.id })
    }

    @Test
    fun `clearing empties the list`() {
        val solves = FakeSolves()
        solves.seed(solve(seconds = 84, id = 1), solve(size = 4, seconds = 6, id = 2))
        val viewModel = ScoresViewModel(solves, writes)
        clock.scheduler.runCurrent()

        viewModel.onClearAll()
        clock.scheduler.runCurrent()

        assertTrue(viewModel.uiState.groups.isEmpty())
    }

    @Test
    fun `a card carries every solve of its size, fastest first`() {
        val solves = FakeSolves()
        solves.seed(*(1..8).map { solve(seconds = it * 10, id = it.toLong()) }.toTypedArray())
        val viewModel = ScoresViewModel(solves, writes)

        clock.scheduler.runCurrent()

        val group = viewModel.uiState.groups.single()
        assertEquals(8, group.solves.size, "nothing is hidden: every one of them can be deleted")
        assertEquals((1..8).map { it * 10 }, group.solves.map { it.seconds })
    }

    @Test
    fun `a table that never comes back says so instead of claiming the records are gone`() {
        val table = FlakySolves(stumbles = 9)
        table.seed(solve(size = 4, seconds = 6, id = 1))
        val viewModel = ScoresViewModel(table, writes)

        clock.scheduler.advanceTimeBy(10.seconds)

        assertTrue(viewModel.uiState.groups.isEmpty())
        assertFalse(viewModel.uiState.readable, "the screen must not read as an empty table")
    }

    @Test
    fun `a table that stumbles once is asked again rather than left for dead`() {
        val table = FlakySolves()
        table.seed(solve(size = 4, seconds = 6, id = 1))
        val viewModel = ScoresViewModel(table, writes)

        clock.scheduler.runCurrent()
        assertTrue(viewModel.uiState.groups.isEmpty(), "the first read failed, so there is nothing yet")

        clock.scheduler.advanceTimeBy(1.5.seconds)
        assertEquals(listOf(4), viewModel.uiState.groups.map { it.size }, "and the second read arrives")
    }

    @Test
    fun `a row that refuses to be deleted never reaches the handler that ends the process`() {
        val solves = RefusingWrites()
        solves.seed(solve(id = 1))
        val viewModel = ScoresViewModel(solves, writes)
        clock.scheduler.runCurrent()

        val escaped = whileWatchingForEscapes { viewModel.onDelete(1) }

        assertTrue(escaped.isEmpty(), "a refused delete reached the uncaught handler: $escaped")
        assertEquals(listOf(8), viewModel.uiState.groups.map { it.size }, "the screen went down with the row")
        assertTrue(viewModel.uiState.readable, "a refused write is not a table that cannot be read")
    }

    @Test
    fun `a table that refuses to be cleared never reaches the handler that ends the process`() {
        val solves = RefusingWrites()
        solves.seed(solve(id = 1))
        val viewModel = ScoresViewModel(solves, writes)
        clock.scheduler.runCurrent()

        val escaped = whileWatchingForEscapes { viewModel.onClearAll() }

        assertTrue(escaped.isEmpty(), "a refused clearing reached the uncaught handler: $escaped")
        assertEquals(listOf(8), viewModel.uiState.groups.map { it.size }, "the screen went down with the table")
    }

    @Test
    fun `a row deleted as the player leaves is still deleted`() {
        // Swipe and go back in one movement. On this screen's own scope the delete is queued
        // behind the gesture and cancelled by it, and the row the player watched disappear is
        // there again next time they look.
        val solves = FakeSolves()
        solves.seed(solve(seconds = 84, id = 1), solve(seconds = 72, id = 2))
        val viewModel = ScoresViewModel(solves, writes)
        clock.scheduler.runCurrent()

        viewModel.onDelete(1)
        pop(viewModel)
        clock.scheduler.runCurrent()

        assertEquals(listOf(2L), solves.rows.map { it.id }, "the delete went down with the screen that made it")
    }

    @Test
    fun `a table cleared as the player leaves is still cleared`() {
        val solves = FakeSolves()
        solves.seed(solve(seconds = 84, id = 1), solve(size = 4, seconds = 6, id = 2))
        val viewModel = ScoresViewModel(solves, writes)
        clock.scheduler.runCurrent()

        viewModel.onClearAll()
        pop(viewModel)
        clock.scheduler.runCurrent()

        assertTrue(solves.rows.isEmpty(), "the clear went down with the screen that made it")
    }

    /**
     * What the process would have done with an exception the view model let go.
     *
     * Asserting on `uiState` alone cannot tell a swallowed failure from one that escaped the
     * coroutine entirely: both leave the state exactly as it was, and only one of them ends the
     * app.
     */
    private fun whileWatchingForEscapes(write: () -> Unit): List<Throwable> {
        val escaped = mutableListOf<Throwable>()
        val before = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, failure -> escaped += failure }

        try {
            write()
            clock.scheduler.runCurrent()
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(before)
        }

        return escaped
    }

    private fun solve(
        size: Int = 8,
        seconds: Int = 60,
        id: Long = 1,
    ) = Solve(
        size = size,
        puzzle = "queens",
        seconds = seconds,
        finishedAt = 0L,
        id = id,
    )
}

/** A table that refuses its first [stumbles] reads, the way a locked database does. */
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

/** A table that answers reads and refuses every write, the way a full disk does. */
private class RefusingWrites(
    private val rows: FakeSolves = FakeSolves(),
) : SolveRepository by rows {
    fun seed(vararg solves: Solve) = rows.seed(*solves)

    override suspend fun delete(id: Long): Unit = error("the disk is full")

    override suspend fun clear(): Unit = error("the disk is full")
}

/**
 * What the framework does to a view model when its destination is popped: the store it was kept in
 * is cleared, and clearing cancels the scope the view model composes on.
 *
 * There is no shorter way to reach it — cancelling that scope is not something a view model
 * exposes, and a test that reached around it would be testing its own reach.
 */
private fun pop(viewModel: ViewModel) {
    val store = ViewModelStore()
    ViewModelProvider(
        store,
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = viewModel as T
        },
    )[viewModel::class.java]
    store.clear()
}
