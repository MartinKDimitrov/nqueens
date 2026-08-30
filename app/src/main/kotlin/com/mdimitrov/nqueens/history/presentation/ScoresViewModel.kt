package com.mdimitrov.nqueens.history.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdimitrov.nqueens.history.domain.SolveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ScoresViewModel
    @Inject
    constructor(
        private val solves: SolveRepository,
    ) : ViewModel() {
        var uiState by mutableStateOf(ScoresUiState())
            private set

        init {
            viewModelScope.launch {
                solves.solves().collect { uiState = ScoresUiState(groupsOf(it)) }
            }
        }

        fun onDelete(id: Long) {
            viewModelScope.launch { solves.delete(id) }
        }

        fun onClearAll() {
            viewModelScope.launch { solves.clear() }
        }
    }
