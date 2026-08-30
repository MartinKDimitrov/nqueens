package com.mdimitrov.nqueens.history.presentation

import com.mdimitrov.nqueens.R
import com.mdimitrov.nqueens.history.FakeSolves
import com.mdimitrov.nqueens.history.domain.Solve
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        assertEquals(listOf(72, 84), viewModel.uiState.groups.last().runs.map { it.seconds })
    }

    @Test
    fun `deleting a row takes it out of the list and leaves the rest`() {
        val solves = FakeSolves()
        solves.seed(solve(seconds = 84, id = 1), solve(seconds = 72, id = 2))
        val viewModel = ScoresViewModel(solves)
        clock.scheduler.runCurrent()

        viewModel.onDelete(1)
        clock.scheduler.runCurrent()

        assertEquals(listOf(2L), viewModel.uiState.groups.flatMap { it.runs }.map { it.id })
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

    private fun solve(
        size: Int = 8,
        seconds: Int = 60,
        id: Long = 1,
    ) = Solve(
        size = size,
        variant = R.string.variant_queens,
        seconds = seconds,
        finishedAt = 0L,
        id = id,
    )
}
