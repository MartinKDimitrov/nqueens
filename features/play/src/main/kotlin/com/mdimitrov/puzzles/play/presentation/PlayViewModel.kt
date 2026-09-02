package com.mdimitrov.puzzles.play.presentation

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdimitrov.puzzles.boardlogic.Cell
import com.mdimitrov.puzzles.boardlogic.GameAction
import com.mdimitrov.puzzles.boardlogic.GameState
import com.mdimitrov.puzzles.boardlogic.reduce
import com.mdimitrov.puzzles.boardlogic.snapshotOf
import com.mdimitrov.puzzles.puzzletype.Puzzle
import com.mdimitrov.puzzles.puzzletype.Puzzles
import com.mdimitrov.puzzles.scope.ApplicationScope
import com.mdimitrov.puzzles.solves.RecordSolve
import com.mdimitrov.puzzles.solves.SolvedBoard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

private val TickInterval = 1.seconds

/**
 * Where the board itself is kept while the process is gone. The route carries which puzzle and how
 * big; these carry what has been played on it, so a board survives the system reclaiming the app
 * rather than starting again from empty.
 *
 * They are plain arrays and numbers rather than one object: what `SavedStateHandle` can carry
 * through a process death is what a `Bundle` can, and a square is an index into the board.
 */
private const val SAVED_SIZE = "board.size"
private const val SAVED_PIECES = "board.pieces"
private const val SAVED_ELAPSED = "board.elapsed"
private const val SAVED_RECORDED = "board.recorded"
private const val SAVED_BEST = "board.best"

@HiltViewModel
internal class PlayViewModel
    @Inject
    constructor(
        puzzles: Puzzles,
        private val records: RecordSolve,
        @ApplicationScope private val writes: CoroutineScope,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        // Which puzzle is the route's answer, not the container's: a build with two games binds
        // both, and this board is one of them.
        private val puzzle: Puzzle =
            checkNotNull(savedStateHandle.get<String>(PUZZLE_ARGUMENT)?.let(puzzles::byKey)) {
                "No puzzle named \"${savedStateHandle.get<String>(PUZZLE_ARGUMENT)}\" is installed; " +
                    "the board is reached through playRoute(puzzle, size)."
            }

        private val size: Int =
            checkNotNull(savedStateHandle.get<Int>(SIZE_ARGUMENT)) {
                "No \"$SIZE_ARGUMENT\" in the route arguments; the board is reached " +
                    "through playRoute(puzzle, size)."
            }

        private val handle = savedStateHandle

        /**
         * Whether what was saved belongs to this board at all.
         *
         * A build whose `Puzzle.sizes` moved can meet a back stack written by the one before it,
         * and then every saved value is another board's: its squares name positions this board
         * does not have, its clock counted a different game, and its "already written down" would
         * silence the win this board is about to produce. One question, asked once, for all four.
         */
        private val saved = handle.get<Int>(SAVED_SIZE) == size

        private var state by mutableStateOf(
            GameState(
                size = size,
                pieces = if (saved) handle.pieces(size) else emptySet(),
                elapsedSeconds = if (saved) handle.get<Int>(SAVED_ELAPSED) ?: 0 else 0,
            ),
        )

        // What the win card compares against: the fastest board of this size and puzzle before
        // this one was finished.
        private var previousBestSeconds by mutableStateOf(if (saved) handle.get<Int>(SAVED_BEST) else null)

        // One game is one record. A solved board disturbed and solved again is still that game;
        // a reset starts another, and `generation` tells a table's late answer which game asked.
        //
        // `recorded` is claimed only once the row is on the table, so it means "written down"
        // rather than "attempted". `writing` is what stops a second attempt inside this process
        // and is deliberately not saved: an attempt the system interrupted is one nobody made.
        private var recorded = saved && handle.get<Boolean>(SAVED_RECORDED) ?: false
        private var writing = false

        // Not saved: it is only ever compared against a write this same instance launched, and
        // an instance the system has just built has none in flight.
        private var generation = 0

        private var ticking: Job? = null

        val uiState: PlayUiState by derivedStateOf {
            PlayUiState(
                snapshotOf(state, puzzle.rules(state.size), puzzle.piecesToSolve(state.size)),
                puzzle,
                state.elapsedSeconds,
                previousBestSeconds,
            )
        }

        init {
            // A board that comes back solved and unwritten is a win the process was taken during.
            // Nothing else would ask for that row: a solved board answers no tap and the clock has
            // stopped, so the only chance to keep it is here.
            if (!recorded && uiState.board.isSolved) write()

            startTheClock()
        }

        /**
         * The clock, begun again from zero rather than left running.
         *
         * A reset that only zeroed the count would leave the tick where it was: a board started
         * nine tenths of the way through a second counts its first second in a tenth of one, and
         * every time this game records is short by whatever was left over.
         */
        private fun startTheClock() {
            ticking?.cancel()
            ticking =
                viewModelScope.launch {
                    while (true) {
                        delay(TickInterval)
                        if (!uiState.board.isSolved) onAction(GameAction.Tick)
                    }
                }
        }

        fun onAction(action: GameAction) {
            state = reduce(state, action)

            if (action is GameAction.Reset) {
                generation++
                recorded = false
                writing = false
                previousBestSeconds = null
                startTheClock()
            }
            if (!recorded && !writing && uiState.board.isSolved) write()

            remember()
        }

        /**
         * Hands the finished board to the table, once per game.
         *
         * The board and the game are read here rather than inside the coroutine: what is written
         * down is the board as it was finished, whatever the dispatcher does with the launch.
         */
        private fun write() {
            writing = true
            val solved = state
            val game = generation

            writes.launch { record(solved, game) }
        }

        /**
         * The board as the system would have to give it back. Written on every move rather than
         * when the process is about to go, because what is asked for at that moment is whatever
         * the handle already holds.
         */
        private fun remember() {
            handle[SAVED_SIZE] = size
            handle[SAVED_PIECES] = state.pieces.map { it.row * size + it.col }.toIntArray()
            handle[SAVED_ELAPSED] = state.elapsedSeconds
            handle[SAVED_RECORDED] = recorded
            handle[SAVED_BEST] = previousBestSeconds
        }

        /**
         * A solved board, written down once. The best time before it — what the win card compares
         * against — is read first and claimed only once the row is on the table, so a refused
         * write cannot leave a record on screen that nobody kept, and a refused read costs the
         * comparison rather than the record.
         *
         * A table that refuses costs the record and not the game. Nothing retries and nothing is
         * reported: after a win the board answers nobody — not a tap, not the clock, not the top
         * bar — so there is no second attempt to make, and the app has no place to say "not
         * saved". PROJECT §6 says so rather than pretending otherwise.
         */
        @Suppress("SwallowedException", "TooGenericExceptionCaught")
        private suspend fun record(
            solved: GameState,
            game: Int,
        ) {
            val best =
                try {
                    records.record(SolvedBoard(puzzle.key, solved.size, solved.elapsedSeconds))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (refused: Exception) {
                    return
                }

            // The board may have been played again while the table was answering, and the card of
            // a game that has ended is not the card on screen. The question is asked back on the
            // thread that plays the game: `generation` is the screen's own count and is only
            // read where it is written.
            withContext(Dispatchers.Main) {
                if (game != generation) return@withContext

                // Claimed here and not before the write: a flag saved ahead of the row would turn
                // a process death mid-write into a board that says it is finished and a record
                // nobody kept. What is left is the other way round — the row landing and the
                // process going before this line runs, which writes the same board twice on the
                // next launch. That window is one assignment wide; the one it replaces was a
                // whole database round trip.
                recorded = true
                previousBestSeconds = best
                remember()
            }
        }
    }

/**
 * The squares a restored board was left with, one index per piece.
 *
 * Only ever called for a board the saved size agrees with, so every index names a square this
 * board has: an index read against a different size would not recover a piece, it would invent
 * one somewhere the player never touched, and `GameState` would refuse it inside a property
 * initialiser where there is no screen left to catch the refusal.
 */
private fun SavedStateHandle.pieces(size: Int): Set<Cell> =
    get<IntArray>(SAVED_PIECES)
        ?.map { Cell(row = it / size, col = it % size) }
        ?.toSet()
        .orEmpty()
