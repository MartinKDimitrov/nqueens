package com.mdimitrov.puzzles.scores.presentation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/** Where the records are in a navigation graph. */
public const val SCORES_ROUTE: String = "scores"

/**
 * The records, declared by the feature that owns them. What the app knows is the route and the two
 * ways out of it; the screen, its state and its table stay inside.
 *
 * Two, because they are different journeys: [onBack] returns to whatever opened the list, and
 * [onNewGame] leaves for a new board wherever the player came from.
 */
public fun NavGraphBuilder.scoresDestination(
    onBack: () -> Unit,
    onNewGame: () -> Unit,
) {
    composable(SCORES_ROUTE) {
        ScoresScreen(onBack = onBack, onNewGame = onNewGame)
    }
}
