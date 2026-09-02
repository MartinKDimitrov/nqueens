package com.mdimitrov.puzzles.setup.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdimitrov.puzzles.puzzletype.Puzzle
import com.mdimitrov.puzzles.puzzletype.Puzzles
import com.mdimitrov.puzzles.scope.ApplicationScope
import com.mdimitrov.puzzles.settings.ThemeChoice
import com.mdimitrov.puzzles.settings.Themes
import com.mdimitrov.puzzles.setup.domain.DEFAULT_BOARD_SIZE
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SetupViewModel
    @Inject
    constructor(
        private val puzzles: Puzzles,
        private val themes: Themes,
        @ApplicationScope private val writes: CoroutineScope,
    ) : ViewModel() {
        var uiState: SetupUiState by mutableStateOf(startingOn(puzzles.all.first()))
            private set

        init {
            viewModelScope.launch {
                themes.choice.collectLatest { uiState = uiState.copy(theme = it) }
            }
        }

        /**
         * The palette the player asked for, kept for the next run as well as this one.
         *
         * Not on this screen's scope: a player may press the button and leave in the same breath,
         * and a preference file that was never written is a choice the app forgot. And a refused
         * write costs the choice, not the app — the same rule every other write here follows, and
         * the one this one did not until a corrupt file was found to end the process.
         */
        @Suppress("SwallowedException", "TooGenericExceptionCaught")
        fun chooseTheme(theme: ThemeChoice) {
            writes.launch {
                try {
                    themes.choose(theme)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (refused: Exception) {
                    return@launch
                }
            }
        }

        fun grow() {
            if (uiState.canGrow) uiState = uiState.copy(size = uiState.size + 1)
        }

        fun shrink() {
            if (uiState.canShrink) uiState = uiState.copy(size = uiState.size - 1)
        }

        /**
         * Choosing another puzzle keeps the size when that puzzle plays it, and moves to the
         * nearest one it does when it does not.
         */
        fun choose(puzzle: Puzzle) {
            if (puzzle.key == uiState.puzzle.key) return

            // `copy`, not the constructor: the state carries the palette too, and building it
            // positionally silently left that behind — the button then named a palette the app
            // was not drawn in, and nothing re-emitted to correct it.
            uiState = uiState.copy(size = uiState.size.coerceIn(puzzle.sizes), puzzle = puzzle)
        }

        // The board the stepper opens on is the app's preference, not the puzzle's, so a puzzle
        // that does not play it starts at the nearest size it does.
        private fun startingOn(puzzle: Puzzle) =
            SetupUiState(
                size = DEFAULT_BOARD_SIZE.coerceIn(puzzle.sizes),
                puzzle = puzzle,
                installed = puzzles.all,
            )
    }
