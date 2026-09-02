package com.mdimitrov.puzzles.play.presentation

import com.mdimitrov.puzzles.boardlogic.BoardSnapshot
import com.mdimitrov.puzzles.puzzletype.Puzzle

internal data class PlayUiState(
    val board: BoardSnapshot,
    val puzzle: Puzzle,
    val elapsedSeconds: Int,
    // The fastest solve of this size and puzzle before this board was finished, if there was one.
    val previousBestSeconds: Int? = null,
)
