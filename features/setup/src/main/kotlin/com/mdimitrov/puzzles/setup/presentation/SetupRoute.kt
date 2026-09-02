package com.mdimitrov.puzzles.setup.presentation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.mdimitrov.puzzles.puzzletype.Puzzle

/** Where a game is chosen, in a navigation graph. */
public const val SETUP_ROUTE: String = "setup"

/**
 * Choosing a board, declared by the shell that owns the screen. What the app knows is the route
 * and the two ways out of it.
 */
public fun NavGraphBuilder.setupDestination(
    onStart: (Puzzle, Int) -> Unit,
    onScores: () -> Unit,
) {
    composable(SETUP_ROUTE) {
        SetupScreen(onStart = onStart, onScores = onScores)
    }
}
