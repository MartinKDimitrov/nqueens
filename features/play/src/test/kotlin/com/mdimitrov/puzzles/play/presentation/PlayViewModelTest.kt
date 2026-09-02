package com.mdimitrov.puzzles.play.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.mdimitrov.puzzles.boardlogic.Cell
import com.mdimitrov.puzzles.boardlogic.CellStatus
import com.mdimitrov.puzzles.boardlogic.GameAction
import com.mdimitrov.puzzles.boardlogic.Line
import com.mdimitrov.puzzles.boardlogic.LineKind
import com.mdimitrov.puzzles.boardlogic.LineRules
import com.mdimitrov.puzzles.nqueens.Queens
import com.mdimitrov.puzzles.puzzletype.Puzzle
import com.mdimitrov.puzzles.puzzletype.Puzzles
import com.mdimitrov.puzzles.solves.RecordSolve
import com.mdimitrov.puzzles.solves.SolvedBoard
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

class PlayViewModelTest {
    private val clock = StandardTestDispatcher()

    // What the shell provides in production: a scope that outlives any one screen. It runs
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
        val viewModel = gameOf(puzzle = Queens.copy(rules = rowsOnly))

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
        // A puzzle whose key is not the queens' one: the key the record carries has to come from
        // the puzzle being played rather than from the only one there usually is.
        val another = Queens.copy(key = "another-puzzle")
        val records = FakeRecords()
        val viewModel = gameOf(puzzle = another, records = records)

        clock.scheduler.advanceTimeBy(2.5.seconds)
        solveThe(viewModel)
        clock.scheduler.runCurrent()

        assertEquals(SolvedBoard(another.key, size = 4, seconds = 2), records.added.single())
    }

    @Test
    fun `a solved board disturbed and solved again is still one record`() {
        val records = FakeRecords()
        val viewModel = gameOf(records = records)
        solveThe(viewModel)
        clock.scheduler.runCurrent()

        viewModel.onAction(GameAction.Toggle(Cell(0, 1)))
        viewModel.onAction(GameAction.Toggle(Cell(0, 1)))
        clock.scheduler.runCurrent()

        assertEquals(1, records.added.size)
    }

    @Test
    fun `a board played again after a reset is a record of its own`() {
        val records = FakeRecords()
        val viewModel = gameOf(records = records)
        solveThe(viewModel)
        clock.scheduler.runCurrent()

        viewModel.onAction(GameAction.Reset)
        solveThe(viewModel)
        clock.scheduler.runCurrent()

        assertEquals(2, records.added.size)
    }

    @Test
    fun `the card is told the best time from before this board was solved`() {
        val records = FakeRecords()
        records.seed(size = 4, seconds = 90)
        records.seed(size = 8, seconds = 20)
        val viewModel = gameOf(records = records)

        solveThe(viewModel)
        clock.scheduler.runCurrent()

        // The board just finished is 4x4 and took no time at all: the 90 is the earlier 4x4,
        // and the faster 8x8 belongs to another board.
        assertEquals(90, viewModel.uiState.previousBestSeconds)

        viewModel.onAction(GameAction.Reset)
        assertNull(viewModel.uiState.previousBestSeconds)
    }

    @Test
    fun `a board of another puzzle is not the best time this one is compared against`() {
        val records = FakeRecords()
        records.seed(size = 4, seconds = 5, puzzle = "another-puzzle")
        val viewModel = gameOf(records = records)

        solveThe(viewModel)
        clock.scheduler.runCurrent()

        assertNull(viewModel.uiState.previousBestSeconds)
    }

    @Test
    fun `a board played again while the table answers does not inherit the finished game's best`() {
        val records = FakeRecords()
        records.seed(size = 4, seconds = 90)
        val viewModel = gameOf(records = records)
        solveThe(viewModel)

        viewModel.onAction(GameAction.Reset)
        clock.scheduler.runCurrent()

        assertNull(viewModel.uiState.previousBestSeconds)
        assertEquals(1, records.added.size, "the board just solved is written down")
    }

    @Test
    fun `a board nobody has solved is not written down`() {
        val records = FakeRecords()
        val viewModel = gameOf(records = records)

        // A full board of queens attacking each other: nothing left to place, nothing solved.
        listOf(Cell(0, 0), Cell(1, 1), Cell(2, 2), Cell(3, 3))
            .forEach { viewModel.onAction(GameAction.Toggle(it)) }
        clock.scheduler.advanceTimeBy(2.5.seconds)
        clock.scheduler.runCurrent()

        assertEquals(0, viewModel.uiState.board.piecesLeft)
        assertFalse(viewModel.uiState.board.isSolved)
        assertTrue(records.added.isEmpty())
    }

    @Test
    fun `a table that throws never reaches the handler that ends the process`() {
        val escaped = mutableListOf<Throwable>()
        val before = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, failure -> escaped += failure }

        try {
            val viewModel = gameOf(records = RefusingRecords())
            solveThe(viewModel)
            clock.scheduler.runCurrent()
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(before)
        }

        assertTrue(escaped.isEmpty(), "a refused write reached the uncaught handler: $escaped")
    }

    @Test
    fun `a record kept without an answer leaves the card claiming none`() {
        val records = FakeRecords(answering = false)
        val viewModel = gameOf(records = records)

        solveThe(viewModel)
        clock.scheduler.runCurrent()

        assertEquals(1, records.added.size, "the board is written down all the same")
        assertNull(viewModel.uiState.previousBestSeconds, "and the card claims no record")
    }

    @Test
    fun `a refused write costs the record, not the game, and the next game may still be written`() {
        val table = RefusingRecords()
        table.seed(size = 4, seconds = 90)
        val viewModel = gameOf(records = table)

        solveThe(viewModel)
        clock.scheduler.runCurrent()

        assertTrue(viewModel.uiState.board.isSolved, "the game survives a storage failure")
        assertTrue(table.added.isEmpty(), "and this board was not written down")
        assertNull(viewModel.uiState.previousBestSeconds, "so the card claims no record")

        table.refusing = false
        viewModel.onAction(GameAction.Reset)
        solveThe(viewModel)
        clock.scheduler.runCurrent()

        assertEquals(1, table.added.size, "and the next game is written")
    }

    @Test
    fun `a board solved and left at once is still written down`() {
        val records = FakeRecords()
        val viewModel = gameOf(records = records)
        // A destination holds its view model in a store; popping the destination clears it, which
        // is what cancels the scope the screen's own work runs in.
        val store = ViewModelStore()
        ViewModelProvider(store, factoryFor(viewModel))[PlayViewModel::class.java]

        solveThe(viewModel)
        store.clear()
        clock.scheduler.advanceUntilIdle()

        assertEquals(1, records.added.size, "the player left before the table answered and lost the board")
    }

    @Test
    fun `a board is not written down a second time while the table is still answering`() {
        // The clock is still running when the last queen lands: the tick that arrives before the
        // table has answered asks the same question the queen did, and the answer has to be no.
        // Without that, one game leaves two rows and the scores show a board solved twice.
        val records = FakeRecords()
        val viewModel = gameOf(records = records)

        solveThe(viewModel)
        viewModel.onAction(GameAction.Tick)
        clock.scheduler.runCurrent()

        assertEquals(1, records.added.size, "one finished board left more than one row")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `the clock starts over with the board rather than part way through a second`() {
        // Reset zeroes the count; it has to zero the tick as well. A board begun nine tenths of
        // the way through a second would otherwise count its first second in a tenth of one, and
        // every time it records is short by whatever was left over.
        val viewModel = gameOf()
        clock.scheduler.advanceTimeBy(900)

        viewModel.onAction(GameAction.Reset)
        clock.scheduler.advanceTimeBy(900)

        assertEquals(0, viewModel.uiState.elapsedSeconds, "the new board inherited the old one's part-second")
        clock.scheduler.advanceTimeBy(200)
        assertEquals(1, viewModel.uiState.elapsedSeconds, "and then it counts as it always did")
    }

    private fun factoryFor(viewModel: PlayViewModel) =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = viewModel as T
        }

    private fun gameOf(
        size: Int = 4,
        puzzle: Puzzle = Queens,
        records: RecordSolve = FakeRecords(),
    ) = PlayViewModel(
        puzzles = Puzzles(setOf(puzzle)),
        records = records,
        writes = writes,
        savedStateHandle =
            SavedStateHandle(
                mapOf(PUZZLE_ARGUMENT to puzzle.key, SIZE_ARGUMENT to size),
            ),
    )

    private fun solveThe(viewModel: PlayViewModel) =
        listOf(Cell(0, 1), Cell(1, 3), Cell(2, 0), Cell(3, 2))
            .forEach { viewModel.onAction(GameAction.Toggle(it)) }
}

/**
 * A recorder that keeps what it is given and answers with the best it already held, the way the
 * scores feature does. With [answering] false it keeps the board and says nothing, which is what
 * a table that refuses to be read looks like from here.
 */
private class FakeRecords(
    private val answering: Boolean = true,
) : RecordSolve {
    val added = mutableListOf<SolvedBoard>()
    private val best = mutableMapOf<Pair<String, Int>, Int>()

    fun seed(
        size: Int,
        seconds: Int,
        puzzle: String = Queens.key,
    ) {
        best.merge(puzzle to size, seconds, ::minOf)
    }

    override suspend fun record(board: SolvedBoard): Int? {
        val before = best[board.puzzle to board.size]
        added += board
        best.merge(board.puzzle to board.size, board.seconds, ::minOf)

        return if (answering) before else null
    }
}

/** A recorder that refuses, the way a full disk does, until it is told to stop. */
private class RefusingRecords : RecordSolve {
    private val rows = FakeRecords()
    var refusing = true

    val added: List<SolvedBoard> get() = rows.added

    fun seed(
        size: Int,
        seconds: Int,
    ) = rows.seed(size = size, seconds = seconds)

    override suspend fun record(board: SolvedBoard): Int? {
        if (refusing) error("the disk is full")

        return rows.record(board)
    }
}
