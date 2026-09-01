package com.mdimitrov.nqueens.game.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.mdimitrov.nqueens.domain.Cell
import com.mdimitrov.nqueens.domain.CellStatus
import com.mdimitrov.nqueens.domain.GameAction
import com.mdimitrov.nqueens.domain.Line
import com.mdimitrov.nqueens.domain.LineKind
import com.mdimitrov.nqueens.domain.LineRules
import com.mdimitrov.nqueens.history.FakeSolves
import com.mdimitrov.nqueens.history.domain.Clock
import com.mdimitrov.nqueens.history.domain.Solve
import com.mdimitrov.nqueens.history.domain.SolveRepository
import com.mdimitrov.nqueens.puzzle.Queens
import com.mdimitrov.nqueens.puzzle.Variant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class GameViewModelTest {
    private val clock = StandardTestDispatcher()

    // What the application provides in production: a scope that outlives any one screen. It runs
    // on the same test clock so a record still lands when the clock is advanced.
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
    fun `the board starts empty, at the size the route asked for`() {
        val viewModel = gameOf(size = 6)

        assertEquals(6, viewModel.uiState.board.size)
        assertEquals(6, viewModel.uiState.board.piecesLeft)
        assertTrue(viewModel.uiState.board.statuses.all { it == CellStatus.EMPTY })
    }

    @Test
    fun `tapping an empty square places a queen, and tapping her takes her back`() {
        val viewModel = gameOf()

        viewModel.onAction(GameAction.Toggle(Cell(1, 2)))
        assertEquals(CellStatus.PIECE, viewModel.uiState.board.statusAt(row = 1, col = 2))
        assertEquals(3, viewModel.uiState.board.piecesLeft)

        viewModel.onAction(GameAction.Toggle(Cell(1, 2)))
        assertEquals(CellStatus.EMPTY, viewModel.uiState.board.statusAt(row = 1, col = 2))
        assertEquals(4, viewModel.uiState.board.piecesLeft)
    }

    @Test
    fun `a queen under attack is placed and both are flagged`() {
        val viewModel = gameOf()

        viewModel.onAction(GameAction.Toggle(Cell(0, 0)))
        viewModel.onAction(GameAction.Toggle(Cell(0, 3)))

        assertEquals(CellStatus.PIECE_CONFLICT, viewModel.uiState.board.statusAt(row = 0, col = 0))
        assertEquals(CellStatus.PIECE_CONFLICT, viewModel.uiState.board.statusAt(row = 0, col = 3))
        assertFalse(viewModel.uiState.board.isSolved)
    }

    @Test
    fun `a solution reports the board solved with nothing left to place`() {
        val viewModel = gameOf()

        listOf(Cell(0, 1), Cell(1, 3), Cell(2, 0), Cell(3, 2))
            .forEach { viewModel.onAction(GameAction.Toggle(it)) }

        assertTrue(viewModel.uiState.board.isSolved)
        assertEquals(0, viewModel.uiState.board.piecesLeft)
        assertFalse(viewModel.uiState.board.statuses.any { it == CellStatus.PIECE_CONFLICT })
    }

    @Test
    fun `a tap outside the board changes nothing`() {
        val viewModel = gameOf()
        val before = viewModel.uiState.board

        viewModel.onAction(GameAction.Toggle(Cell(9, 9)))

        assertEquals(before, viewModel.uiState.board)
    }

    @Test
    fun `reset clears the board and gives every queen back`() {
        val viewModel = gameOf()
        viewModel.onAction(GameAction.Toggle(Cell(0, 0)))
        viewModel.onAction(GameAction.Toggle(Cell(1, 1)))

        viewModel.onAction(GameAction.Reset)

        assertEquals(4, viewModel.uiState.board.piecesLeft)
        assertTrue(viewModel.uiState.board.statuses.all { it == CellStatus.EMPTY })
    }

    @Test
    fun `the game plays by the rules it was given, not by the ones it could assume`() {
        val rowsOnly = LineRules { cell -> setOf(Line(LineKind.ROW, cell.row)) }
        val viewModel = gameOf(variant = Queens.copy(rules = rowsOnly))

        viewModel.onAction(GameAction.Toggle(Cell(0, 0)))
        viewModel.onAction(GameAction.Toggle(Cell(1, 0)))

        assertEquals(CellStatus.PIECE, viewModel.uiState.board.statusAt(row = 0, col = 0))
        assertEquals(CellStatus.PIECE, viewModel.uiState.board.statusAt(row = 1, col = 0))
    }

    @Test
    fun `the clock counts a second at a time, and reset starts it over`() {
        val viewModel = gameOf()

        clock.scheduler.advanceTimeBy(3.5.seconds)

        assertEquals(3, viewModel.uiState.elapsedSeconds)

        viewModel.onAction(GameAction.Reset)
        assertEquals(0, viewModel.uiState.elapsedSeconds)
    }

    @Test
    fun `the clock stops on a solved board and runs again after reset`() {
        val viewModel = gameOf()
        listOf(Cell(0, 1), Cell(1, 3), Cell(2, 0), Cell(3, 2))
            .forEach { viewModel.onAction(GameAction.Toggle(it)) }
        assertTrue(viewModel.uiState.board.isSolved)

        clock.scheduler.advanceTimeBy(3.5.seconds)
        assertEquals(0, viewModel.uiState.elapsedSeconds)

        viewModel.onAction(GameAction.Reset)
        assertFalse(viewModel.uiState.board.isSolved)

        clock.scheduler.advanceTimeBy(2.5.seconds)
        assertEquals(2, viewModel.uiState.elapsedSeconds)
    }

    @Test
    fun `a solved board is written down once, with the time it took`() {
        val solves = FakeSolves()
        val viewModel = gameOf(solves = solves, finishedAt = 1_700L)

        clock.scheduler.advanceTimeBy(2.5.seconds)
        solveThe(viewModel)
        clock.scheduler.runCurrent()

        val record = solves.added.single()
        assertEquals(4, record.size)
        assertEquals(Queens.key, record.variant)
        assertEquals(2, record.seconds)
        assertEquals(1_700L, record.finishedAt)
    }

    @Test
    fun `a solved board disturbed and solved again is still one record`() {
        val solves = FakeSolves()
        val viewModel = gameOf(solves = solves)
        solveThe(viewModel)
        clock.scheduler.runCurrent()

        viewModel.onAction(GameAction.Toggle(Cell(0, 1)))
        viewModel.onAction(GameAction.Toggle(Cell(0, 1)))
        clock.scheduler.runCurrent()

        assertEquals(1, solves.added.size)
    }

    @Test
    fun `a board played again after a reset is a record of its own`() {
        val solves = FakeSolves()
        val viewModel = gameOf(solves = solves)
        solveThe(viewModel)
        clock.scheduler.runCurrent()

        viewModel.onAction(GameAction.Reset)
        solveThe(viewModel)
        clock.scheduler.runCurrent()

        assertEquals(2, solves.added.size)
    }

    @Test
    fun `the card is told the best time from before this board was solved`() {
        val solves = FakeSolves()
        solves.seed(solve(size = 4, seconds = 90), solve(size = 8, seconds = 20))
        val viewModel = gameOf(solves = solves)

        solveThe(viewModel)
        clock.scheduler.runCurrent()

        // The board just finished is 4x4 and took no time at all: the 90 is the earlier 4x4,
        // and the faster 8x8 belongs to another board.
        assertEquals(90, viewModel.uiState.previousBestSeconds)

        viewModel.onAction(GameAction.Reset)
        assertNull(viewModel.uiState.previousBestSeconds)
    }

    @Test
    fun `a board of another variant is not the best time this one is compared against`() {
        val solves = FakeSolves()
        solves.seed(solve(size = 4, seconds = 5, variant = "rooks"))
        val viewModel = gameOf(solves = solves)

        solveThe(viewModel)
        clock.scheduler.runCurrent()

        assertNull(viewModel.uiState.previousBestSeconds)
    }

    @Test
    fun `a board played again while the table answers does not inherit the finished game's best`() {
        val solves = FakeSolves()
        solves.seed(solve(size = 4, seconds = 90))
        val viewModel = gameOf(solves = solves)
        solveThe(viewModel)

        viewModel.onAction(GameAction.Reset)
        clock.scheduler.runCurrent()

        assertNull(viewModel.uiState.previousBestSeconds)
        assertEquals(2, solves.added.size, "the earlier record plus this solve: the write survives")
    }

    @Test
    fun `a board nobody has solved is not written down`() {
        val solves = FakeSolves()
        val viewModel = gameOf(solves = solves)

        // A full board of queens attacking each other: nothing left to place, nothing solved.
        listOf(Cell(0, 0), Cell(1, 1), Cell(2, 2), Cell(3, 3))
            .forEach { viewModel.onAction(GameAction.Toggle(it)) }
        clock.scheduler.advanceTimeBy(2.5.seconds)
        clock.scheduler.runCurrent()

        assertEquals(0, viewModel.uiState.board.piecesLeft)
        assertFalse(viewModel.uiState.board.isSolved)
        assertTrue(solves.added.isEmpty())
    }

    @Test
    fun `a table that throws never reaches the handler that ends the process`() {
        val escaped = mutableListOf<Throwable>()
        val before = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, failure -> escaped += failure }

        try {
            val viewModel = gameOf(solves = RefusingWrites())
            solveThe(viewModel)
            clock.scheduler.runCurrent()
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(before)
        }

        assertTrue(escaped.isEmpty(), "a refused write reached the uncaught handler: $escaped")
    }

    @Test
    fun `a refused read costs the comparison, never the record`() {
        val table = RefusingReads()
        val viewModel = gameOf(solves = table)

        solveThe(viewModel)
        clock.scheduler.runCurrent()

        assertEquals(1, table.added.size, "the solve is written down even though the read failed")
        assertNull(viewModel.uiState.previousBestSeconds, "and the card claims no record")
    }

    @Test
    fun `a refused write costs the record, not the game, and the next game may still be written`() {
        val table = RefusingWrites()
        table.seed(solve(size = 4, seconds = 90))
        val viewModel = gameOf(solves = table)

        solveThe(viewModel)
        clock.scheduler.runCurrent()

        assertTrue(viewModel.uiState.board.isSolved, "the game survives a storage failure")
        assertEquals(1, table.added.size, "and this board was not written down")
        assertNull(viewModel.uiState.previousBestSeconds, "so the card claims no record")

        table.refusing = false
        viewModel.onAction(GameAction.Reset)
        solveThe(viewModel)
        clock.scheduler.runCurrent()

        assertEquals(2, table.added.size, "and the next game is written")
    }

    @Test
    fun `a board solved and left at once is still written down`() {
        val solves = FakeSolves()
        val viewModel = gameOf(solves = solves)
        // A destination holds its view model in a store; popping the destination clears it, which
        // is what cancels the scope the screen's own work runs in.
        val store = ViewModelStore()
        ViewModelProvider(store, factoryFor(viewModel))[GameViewModel::class.java]

        solveThe(viewModel)
        store.clear()
        clock.scheduler.advanceUntilIdle()

        assertEquals(1, solves.added.size, "the player left before the table answered and lost the board")
    }

    private fun factoryFor(viewModel: GameViewModel) =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = viewModel as T
        }

    private fun gameOf(
        size: Int = 4,
        variant: Variant = Queens,
        solves: SolveRepository = FakeSolves(),
        finishedAt: Long = 0L,
    ) = GameViewModel(
        variant = variant,
        solves = solves,
        clock = Clock { finishedAt },
        writes = writes,
        savedStateHandle = SavedStateHandle(mapOf(SIZE_ARGUMENT to size)),
    )

    private fun solve(
        size: Int,
        seconds: Int,
        variant: String = Queens.key,
    ) = Solve(size = size, variant = variant, seconds = seconds, finishedAt = 0L)

    private fun solveThe(viewModel: GameViewModel) =
        listOf(Cell(0, 1), Cell(1, 3), Cell(2, 0), Cell(3, 2))
            .forEach { viewModel.onAction(GameAction.Toggle(it)) }
}

/** A table that refuses to be read, the way a locked one does, but takes what it is given. */
private class RefusingReads(
    private val rows: FakeSolves = FakeSolves(),
) : SolveRepository by rows {
    val added: List<Solve> get() = rows.added

    override suspend fun best(
        size: Int,
        variant: String,
    ): Int? = error("the table is locked")
}

/** A table that answers what it is asked and refuses to be written to, the way a full disk does. */
private class RefusingWrites(
    private val rows: FakeSolves = FakeSolves(),
) : SolveRepository by rows {
    var refusing = true

    val added: List<Solve> get() = rows.added

    fun seed(vararg solves: Solve) = rows.seed(*solves)

    override suspend fun add(solve: Solve) {
        if (refusing) error("the disk is full")
        rows.add(solve)
    }
}
