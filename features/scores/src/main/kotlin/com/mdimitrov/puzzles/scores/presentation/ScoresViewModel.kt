package com.mdimitrov.puzzles.scores.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdimitrov.puzzles.scope.ApplicationScope
import com.mdimitrov.puzzles.scores.domain.SolveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
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
        @ApplicationScope private val writes: CoroutineScope,
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

        // Not `viewModelScope`: a player who deletes a row and leaves at once — to a new game,
        // or with the back gesture — has the destination popped under the write, and a suspending
        // query that has not reached the database yet is cancelled with it. The row would still be
        // on the table next time, and nothing would have said so.
        fun onDelete(id: Long) {
            writes.launch { swallowingRefusals { solves.delete(id) } }
        }

        fun onClearAll() {
            writes.launch { swallowingRefusals { solves.clear() } }
        }

        /**
         * A table that refuses costs the row, not the screen. `runCatching` would do as much and
         * would also swallow the cancellation that ends this scope, which is the one exception
         * that has to travel.
         *
         * No test holds that rethrow, and none can while nothing follows the call: a swallowed
         * cancellation and a rethrown one leave the same coroutine finishing at the same point.
         * It is here for the first line of work that is written after it, which is when the
         * difference starts to show.
         */
        @Suppress("SwallowedException", "TooGenericExceptionCaught")
        private suspend fun swallowingRefusals(write: suspend () -> Unit) {
            try {
                write()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (refused: Exception) {
                Unit
            }
        }
    }
