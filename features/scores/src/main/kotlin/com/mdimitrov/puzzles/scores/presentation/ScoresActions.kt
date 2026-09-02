package com.mdimitrov.puzzles.scores.presentation

/** Everything the records screen asks of the app around it, and of the table behind it. */
internal data class ScoresActions(
    val onDelete: (Long) -> Unit,
    val onClearAll: () -> Unit,
    /** Back to whatever opened the list — the stepper, or a board the player has not left. */
    val onBack: () -> Unit,
    /** Away to a new board, wherever the list was opened from. */
    val onNewGame: () -> Unit,
)
