package com.mdimitrov.nqueens.history.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdimitrov.nqueens.history.domain.SolveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

private const val RETRIES = 3L
private val RetryDelay = 1.seconds

@HiltViewModel
internal class ScoresViewModel
    @Inject
    constructor(
        private val solves: SolveRepository,
    ) : ViewModel() {
        var uiState by mutableStateOf(ScoresUiState())
            private set

        // A table that cannot be read costs the records, not the app. It is asked again a few
        // times — a locked database is usually busy rather than broken — and then the screen says
        // so, because a list that reads "nothing solved yet" over a full table is a lie, and one
        // that retries for ever is a loop nobody can see.
        init {
            viewModelScope.launch {
                solves.solves()
                    .retryWhen { _, attempt ->
                        val again = attempt < RETRIES
                        if (again) delay(RetryDelay)
                        again
                    }
                    .catch { uiState = uiState.copy(answered = true, readable = false) }
                    .collect { uiState = ScoresUiState(groups = groupsOf(it), answered = true) }
            }
        }

        fun onDelete(id: Long) {
            viewModelScope.launch { runCatching { solves.delete(id) } }
        }

        fun onClearAll() {
            viewModelScope.launch { runCatching { solves.clear() } }
        }
    }
