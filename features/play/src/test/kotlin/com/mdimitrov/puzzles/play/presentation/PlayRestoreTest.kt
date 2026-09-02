package com.mdimitrov.puzzles.play.presentation

import android.os.Bundle
import android.os.Parcel
import androidx.lifecycle.SavedStateHandle
import com.mdimitrov.puzzles.boardlogic.Cell
import com.mdimitrov.puzzles.boardlogic.CellStatus
import com.mdimitrov.puzzles.boardlogic.GameAction
import com.mdimitrov.puzzles.nqueens.Queens
import com.mdimitrov.puzzles.puzzletype.Puzzles
import com.mdimitrov.puzzles.solves.RecordSolve
import com.mdimitrov.puzzles.solves.SolvedBoard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val A_BOARD = 4

/**
 * What the system gives back after it has taken the process. The round trip is the real one —
 * `SavedStateHandle` is asked to write itself and a second board is built from what it wrote — so
 * a value this board keeps but a `Bundle` cannot carry fails here rather than on a device.
 *
 * It runs under Robolectric for that reason alone; everything else about this view model is
 * tested without Android in `PlayViewModelTest`.
 */
@RunWith(RobolectricTestRunner::class)
class PlayRestoreTest {
    private val clock = StandardTestDispatcher()
    private val writes = CoroutineScope(clock)

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun useTheTestClock() = Dispatchers.setMain(clock)

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun releaseTheClock() = Dispatchers.resetMain()

    @Test
    fun `a board the system took the process from comes back as it was left`() {
        val handle = routeTo(A_BOARD)
        val before = gameOn(handle)
        before.onAction(GameAction.Toggle(Cell(0, 1)))
        before.onAction(GameAction.Toggle(Cell(1, 3)))
        before.onAction(GameAction.Tick)
        before.onAction(GameAction.Tick)

        val after = gameOn(reborn(handle))

        assertEquals(setOf(Cell(0, 1), Cell(1, 3)), after.piecesOnBoard(), "the pieces the player placed")
        assertEquals(2, after.uiState.elapsedSeconds, "the clock kept what it had counted")
    }

    @Test
    fun `a win already written down is not written again when the board comes back`() {
        val records = CountingRecords()
        val handle = routeTo(A_BOARD)
        val before = gameOn(handle, records)
        solve(before)
        // `runCurrent`, not `advanceUntilIdle`: this board's clock is an endless loop of delays,
        // so advancing until nothing is left would never return.
        clock.scheduler.runCurrent()

        val after = gameOn(reborn(handle), records)
        // Disturbed and put back: the board is solved again, which is what would ask for a second
        // row if the restored board had forgotten that this game already has one.
        after.onAction(GameAction.Toggle(Cell(0, 1)))
        after.onAction(GameAction.Toggle(Cell(0, 1)))
        clock.scheduler.runCurrent()

        assertTrue(after.uiState.board.isSolved, "the board came back solved")
        assertEquals(1, records.written, "the same win was written down twice")
    }

    @Test
    fun `a win the process was taken from mid-write is kept on the next launch`() {
        val records = CountingRecords()
        val handle = routeTo(A_BOARD)
        val dying = CoroutineScope(clock)
        solve(gameOn(handle, records, dying))
        // The system takes the process: the scope the write runs in goes with it.
        dying.cancel()
        clock.scheduler.runCurrent()
        assertEquals(0, records.written, "the row landed anyway, so this proves nothing")

        gameOn(reborn(handle), records)
        clock.scheduler.runCurrent()

        assertEquals(1, records.written, "a win nobody kept was not written on the next launch")
    }

    @Test
    fun `a board saved at another size is not read onto this one`() {
        val eight = routeTo(size = 8)
        val onEight = gameOn(eight)
        onEight.onAction(GameAction.Toggle(Cell(1, 1)))
        onEight.onAction(GameAction.Toggle(Cell(5, 2)))

        // The same saved board, met by a build whose sizes have moved under it.
        val restored = reborn(eight)
        restored[SIZE_ARGUMENT] = A_BOARD
        val onFour = gameOn(restored)

        assertEquals(
            emptySet(),
            onFour.piecesOnBoard(),
            "a square from another board was read onto this one",
        )
    }

    @Test
    fun `a board saved at another size leaves nothing of itself behind`() {
        // The first game leaves a record behind, so the restored board can be seen not to inherit it.
        val records = CountingRecords(best = 90)
        val eight = routeTo(size = 8)
        val onEight = gameOn(eight, records)
        onEight.onAction(GameAction.Tick)
        onEight.onAction(GameAction.Tick)
        solveEight(onEight)
        clock.scheduler.runCurrent()

        val restored = reborn(eight)
        restored[SIZE_ARGUMENT] = A_BOARD
        val onFour = gameOn(restored, records)

        assertEquals(emptySet(), onFour.piecesOnBoard(), "a square from another board")
        assertEquals(0, onFour.uiState.elapsedSeconds, "another board's clock")
        assertEquals(null, onFour.uiState.previousBestSeconds, "another board's record")

        solve(onFour)
        clock.scheduler.runCurrent()
        assertEquals(2, records.written, "this board's win was never written down")
    }

    @Test
    fun `the best time the win card compares against comes back with the board`() {
        val records = BestOf(90)
        val handle = routeTo(A_BOARD)
        solve(gameOn(handle, records))
        clock.scheduler.runCurrent()

        val after = gameOn(reborn(handle), records)

        assertEquals(90, after.uiState.previousBestSeconds, "the card lost what it compares against")
    }

    @Test
    fun `a board reset after the process was taken writes a row of its own`() {
        val records = CountingRecords()
        val handle = routeTo(A_BOARD)
        solve(gameOn(handle, records))
        clock.scheduler.runCurrent()

        val after = gameOn(reborn(handle), records)
        after.onAction(GameAction.Reset)
        solve(after)
        clock.scheduler.runCurrent()

        assertEquals(2, records.written, "the game after the reset was not written down")
    }

    /**
     * The handle a system that has reclaimed the process would hand back: everything the board
     * kept, written into a `Parcel` and read out of it again.
     *
     * The `else` branch is the point. What the system carries is a `Bundle`, so a value this
     * board starts keeping that a `Bundle` cannot carry fails here, by name, rather than on a
     * device.
     */
    @Suppress("DEPRECATION") // `Bundle.get` is how a test reads back what it did not write by type.
    private fun reborn(handle: SavedStateHandle): SavedStateHandle {
        val written = Bundle()
        handle.keys().forEach { key ->
            when (val value = handle.get<Any?>(key)) {
                null -> written.putString(key, null)
                is Int -> written.putInt(key, value)
                is Boolean -> written.putBoolean(key, value)
                is String -> written.putString(key, value)
                is IntArray -> written.putIntArray(key, value)
                else -> error("the board kept \"$key\" as a ${value::class.simpleName}, which a Bundle cannot carry")
            }
        }

        val parcel = Parcel.obtain()
        parcel.writeBundle(written)
        parcel.setDataPosition(0)
        val back = checkNotNull(parcel.readBundle(javaClass.classLoader))
        parcel.recycle()

        return SavedStateHandle(back.keySet().associateWith { back.get(it) })
    }

    private fun routeTo(size: Int) = SavedStateHandle(mapOf(PUZZLE_ARGUMENT to Queens.key, SIZE_ARGUMENT to size))

    private fun gameOn(
        handle: SavedStateHandle,
        records: RecordSolve = CountingRecords(),
        writes: CoroutineScope = this.writes,
    ) = PlayViewModel(
        puzzles = Puzzles(setOf(Queens)),
        records = records,
        writes = writes,
        savedStateHandle = handle,
    )

    private fun PlayViewModel.piecesOnBoard(): Set<Cell> =
        uiState.board.statuses.indices
            .filter { uiState.board.statuses[it] != CellStatus.EMPTY }
            .map { Cell(row = it / A_BOARD, col = it % A_BOARD) }
            .toSet()

    /** Eight queens that solve an eight-board, so a win can be won at a size that is not four. */
    private fun solveEight(viewModel: PlayViewModel) =
        listOf(
            Cell(0, 0),
            Cell(1, 4),
            Cell(2, 7),
            Cell(3, 5),
            Cell(4, 2),
            Cell(5, 6),
            Cell(6, 1),
            Cell(7, 3),
        ).forEach { viewModel.onAction(GameAction.Toggle(it)) }

    private fun solve(viewModel: PlayViewModel) =
        listOf(Cell(0, 1), Cell(1, 3), Cell(2, 0), Cell(3, 2))
            .forEach { viewModel.onAction(GameAction.Toggle(it)) }
}

/**
 * Counts what reaches the table and answers with [best], so a test can see both what was written
 * and what the win card was given to compare against.
 */
private class CountingRecords(
    private val best: Int? = null,
) : RecordSolve {
    var written = 0
        private set

    override suspend fun record(board: SolvedBoard): Int? {
        written++

        return best
    }
}

/** A table that already holds a better time, so the win card has something to compare against. */
private class BestOf(
    private val seconds: Int,
) : RecordSolve {
    override suspend fun record(board: SolvedBoard): Int = seconds
}
