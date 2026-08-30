package com.mdimitrov.nqueens.game.presentation

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.mdimitrov.nqueens.domain.GameAction
import com.mdimitrov.nqueens.domain.GameState
import com.mdimitrov.nqueens.domain.reduce
import com.mdimitrov.nqueens.domain.snapshotOf
import com.mdimitrov.nqueens.puzzle.Variant
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class GameViewModel
    @Inject
    constructor(
        private val variant: Variant,
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

        val uiState: GameUiState by derivedStateOf { GameUiState(snapshotOf(state, variant.rules), variant) }

        fun onAction(action: GameAction) {
            state = reduce(state, action)
        }
    }
