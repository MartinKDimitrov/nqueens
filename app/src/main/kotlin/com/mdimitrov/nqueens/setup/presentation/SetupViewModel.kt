package com.mdimitrov.nqueens.setup.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.mdimitrov.nqueens.puzzle.Variant
import com.mdimitrov.nqueens.setup.domain.DEFAULT_BOARD_SIZE
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class SetupViewModel
    @Inject
    constructor(
        variant: Variant,
    ) : ViewModel() {
        var uiState: SetupUiState by mutableStateOf(SetupUiState(DEFAULT_BOARD_SIZE, variant))
            private set

        fun grow() {
            if (uiState.canGrow) uiState = uiState.copy(size = uiState.size + 1)
        }

        fun shrink() {
            if (uiState.canShrink) uiState = uiState.copy(size = uiState.size - 1)
        }
    }
