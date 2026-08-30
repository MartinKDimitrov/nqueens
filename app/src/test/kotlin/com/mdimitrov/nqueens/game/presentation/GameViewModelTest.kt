package com.mdimitrov.nqueens.game.presentation

import androidx.lifecycle.SavedStateHandle
import com.mdimitrov.nqueens.domain.Cell
import com.mdimitrov.nqueens.domain.CellStatus
import com.mdimitrov.nqueens.domain.GameAction
import com.mdimitrov.nqueens.domain.Line
import com.mdimitrov.nqueens.domain.LineKind
import com.mdimitrov.nqueens.domain.LineRules
import com.mdimitrov.nqueens.history.FakeSolves
import com.mdimitrov.nqueens.history.domain.Clock
import com.mdimitrov.nqueens.history.domain.SolveRepository
import com.mdimitrov.nqueens.puzzle.Queens
import com.mdimitrov.nqueens.puzzle.Variant
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
        assertEquals(Queens.name, record.variant)
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
        val solves = FakeSolves().apply { fastest = 90 }
        val viewModel = gameOf(solves = solves)

        solveThe(viewModel)
        clock.scheduler.runCurrent()
        assertEquals(90, viewModel.uiState.bestBefore)

        viewModel.onAction(GameAction.Reset)
        assertNull(viewModel.uiState.bestBefore)
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
        savedStateHandle = SavedStateHandle(mapOf(SIZE_ARGUMENT to size)),
    )

    private fun solveThe(viewModel: GameViewModel) =
        listOf(Cell(0, 1), Cell(1, 3), Cell(2, 0), Cell(3, 2))
            .forEach { viewModel.onAction(GameAction.Toggle(it)) }
}
