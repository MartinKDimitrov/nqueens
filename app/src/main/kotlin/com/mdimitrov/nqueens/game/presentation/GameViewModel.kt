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
                        "Board size missing from the route."
                    },
            ),
        )

        // What the win card compares against: the fastest board of this size before this one.
        private var bestBefore by mutableStateOf<Int?>(null)

        // One game is one record. A solved board disturbed and solved again is still that game;
        // a reset starts another.
        private var recorded = false

        val uiState: GameUiState by derivedStateOf {
            GameUiState(snapshotOf(state, variant.rules), variant, state.elapsedSeconds, bestBefore)
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
                recorded = false
                bestBefore = null
            }
            if (!recorded && uiState.board.isSolved) {
                recorded = true
                viewModelScope.launch { record(state) }
            }
        }

        private suspend fun record(solved: GameState) {
            bestBefore = solves.best(solved.size)
            solves.add(
                Solve(
                    size = solved.size,
                    variant = variant.name,
                    seconds = solved.elapsedSeconds,
                    finishedAt = clock.millis(),
                ),
            )
        }
    }
