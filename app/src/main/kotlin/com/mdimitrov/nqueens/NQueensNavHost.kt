package com.mdimitrov.nqueens

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mdimitrov.nqueens.domain.MIN_BOARD_SIZE
import com.mdimitrov.nqueens.setup.presentation.SetupScreen

private const val SETUP_ROUTE = "setup"
private const val GAME_ROUTE = "game"
private const val SIZE_ARGUMENT = "size"

private fun gameRoute(size: Int) = "$GAME_ROUTE/$size"

@Composable
internal fun NQueensNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = SETUP_ROUTE) {
        composable(SETUP_ROUTE) {
            SetupScreen(onStart = { size -> navController.navigate(gameRoute(size)) })
        }
        composable(
            route = "$GAME_ROUTE/{$SIZE_ARGUMENT}",
            arguments = listOf(navArgument(SIZE_ARGUMENT) { type = NavType.IntType }),
        ) { entry ->
            val size = entry.arguments?.getInt(SIZE_ARGUMENT) ?: 0
            if (size in MIN_BOARD_SIZE..LARGEST_PLAYABLE_BOARD) {
                Text(text = stringResource(R.string.setup_board_size_value, size))
            } else {
                LaunchedEffect(size) {
                    navController.navigate(SETUP_ROUTE) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }
}
