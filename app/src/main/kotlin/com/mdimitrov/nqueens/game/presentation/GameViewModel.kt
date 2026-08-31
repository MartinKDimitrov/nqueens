package com.mdimitrov.nqueens.game.presentation

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdimitrov.nqueens.domain.GameAction
import com.mdimitrov.nqueens.domain.GameState
import com.mdimitrov.nqueens.domain.reduce
import com.mdimitrov.nqueens.domain.snapshotOf
import com.mdimitrov.nqueens.history.domain.Clock
import com.mdimitrov.nqueens.history.domain.Solve
import com.mdimitrov.nqueens.history.domain.SolveRepository
import com.mdimitrov.nqueens.puzzle.Variant
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

private val TickInterval = 1.seconds

@HiltViewModel
internal class GameViewModel
    @Inject
    constructor(
        private val variant: Variant,
        private val solves: SolveRepository,
        private val clock: Clock,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private var state by mutableStateOf(
            GameState(
                size =
                    checkNotNull(savedStateHandle.get<Int>(SIZE_ARGUMENT)) {
                        "No \"$SIZE_ARGUMENT\" in the route arguments; the game is reached through gameRoute(size)."
                    },
            ),
        )

        // What the win card compares against: the fastest board of this size and variant before
        // this one was finished.
        private var previousBestSeconds by mutableStateOf<Int?>(null)

        // One game is one record. A solved board disturbed and solved again is still that game;
        // a reset starts another, and `gamesStarted` tells a table's late answer which game asked.
        private var recorded = false
        private var gamesStarted = 0

        val uiState: GameUiState by derivedStateOf {
            GameUiState(snapshotOf(state, variant.rules), variant, state.elapsedSeconds, previousBestSeconds)
        }

        init {
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
                gamesStarted++
                recorded = false
                previousBestSeconds = null
            }
            if (!recorded && uiState.board.isSolved) {
                recorded = true
                // Both are read here rather than inside the coroutine: what is written down is the
                // board as it was finished, whatever the dispatcher does with the launch.
                val solved = state
                val game = gamesStarted
                viewModelScope.launch { record(solved, game) }
            }
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
            // The best time is what the card compares against, and it must be read before the
            // row is written or it would find this very solve. A read that fails costs that
            // comparison and nothing else: the record is what matters and is written regardless.
            val best =
                try {
                    solves.best(solved.size, variant.key)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (refused: Exception) {
                    null
                }

            try {
                solves.add(
                    Solve(
                        size = solved.size,
                        variant = variant.key,
                        seconds = solved.elapsedSeconds,
                        finishedAt = clock.millis(),
                    ),
                )
                // The board may have been played again while the table was answering, and the card
                // of a game that has ended is not the card on screen.
                if (game == gamesStarted) previousBestSeconds = best
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (refused: Exception) {
                Unit
            }
        }
    }
