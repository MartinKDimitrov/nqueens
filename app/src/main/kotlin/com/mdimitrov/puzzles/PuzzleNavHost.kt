package com.mdimitrov.puzzles

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.mdimitrov.puzzles.play.presentation.playDestination
import com.mdimitrov.puzzles.play.presentation.playRoute
import com.mdimitrov.puzzles.puzzletype.Puzzles
import com.mdimitrov.puzzles.scores.presentation.SCORES_ROUTE
import com.mdimitrov.puzzles.scores.presentation.scoresDestination
import com.mdimitrov.puzzles.setup.presentation.SETUP_ROUTE
import com.mdimitrov.puzzles.setup.presentation.setupDestination

@Composable
internal fun PuzzleNavHost(
    puzzles: Puzzles,
    navController: NavHostController = rememberNavController(),
) {
    val scores = { navController.navigate(SCORES_ROUTE) { launchSingleTop = true } }

    NavHost(navController = navController, startDestination = SETUP_ROUTE) {
        setupDestination(
            onStart = { puzzle, size ->
                navController.navigate(playRoute(puzzle.key, size)) { launchSingleTop = true }
            },
            onScores = { scores() },
        )
        // A new game starts from Setup, wherever the list was opened from. Popped back to rather
        // than navigated to: Setup is where the size the player chose is kept, and a second entry
        // in its place is a second view model that has never heard of it. Opened from Setup, the
        // button and the system's own back gesture now do the same thing; opened from a finished
        // board, the gesture goes back to that board and the button skips it, which is what its
        // words promise.
        scoresDestination(
            // What the system's own gesture does, so the button beside the title and the gesture
            // under it agree: back to whatever opened the list.
            onBack = { if (navController.previousBackStackEntry != null) navController.popBackStack() },
            onNewGame = { navController.popBackStack(SETUP_ROUTE, inclusive = false) },
        )
        playDestination(
            puzzles = puzzles,
            onBack = { if (navController.previousBackStackEntry != null) navController.popBackStack() },
            onScores = { scores() },
            onUnplayable = {
                navController.navigate(SETUP_ROUTE) {
                    popUpTo(0) { inclusive = true }
                }
            },
        )
    }
}
