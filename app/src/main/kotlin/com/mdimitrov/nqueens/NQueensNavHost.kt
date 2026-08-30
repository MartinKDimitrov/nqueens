package com.mdimitrov.nqueens

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mdimitrov.nqueens.domain.MIN_BOARD_SIZE
import com.mdimitrov.nqueens.game.presentation.gameDestination
import com.mdimitrov.nqueens.game.presentation.gameRoute
import com.mdimitrov.nqueens.history.presentation.ScoresScreen
import com.mdimitrov.nqueens.puzzle.LARGEST_PLAYABLE_BOARD
import com.mdimitrov.nqueens.setup.presentation.SetupScreen

private const val SETUP_ROUTE = "setup"
private const val SCORES_ROUTE = "scores"

@Composable
internal fun NQueensNavHost(navController: NavHostController = rememberNavController()) {
    val scores = { navController.navigate(SCORES_ROUTE) { launchSingleTop = true } }

    NavHost(navController = navController, startDestination = SETUP_ROUTE) {
        composable(SETUP_ROUTE) {
            SetupScreen(
                onStart = { size -> navController.navigate(gameRoute(size)) { launchSingleTop = true } },
                onScores = { scores() },
            )
        }
        composable(SCORES_ROUTE) {
            // A new game starts from Setup, wherever the list was opened from.
            ScoresScreen(
                onNewGame = {
                    navController.navigate(SETUP_ROUTE) { popUpTo(SETUP_ROUTE) { inclusive = true } }
                },
            )
        }
        gameDestination(
            playableSizes = MIN_BOARD_SIZE..LARGEST_PLAYABLE_BOARD,
            onBack = { if (navController.previousBackStackEntry != null) navController.popBackStack() },
            onScores = { scores() },
            onUnplayableSize = {
                navController.navigate(SETUP_ROUTE) {
                    popUpTo(0) { inclusive = true }
                }
            },
        )
    }
}
