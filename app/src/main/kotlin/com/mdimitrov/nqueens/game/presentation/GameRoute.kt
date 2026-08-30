package com.mdimitrov.nqueens.game.presentation

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

private const val GAME_ROUTE: String = "game"

internal const val SIZE_ARGUMENT: String = "size"

private const val GAME_ROUTE_PATTERN: String = "$GAME_ROUTE/{$SIZE_ARGUMENT}"

internal fun gameRoute(size: Int): String = "$GAME_ROUTE/$size"

internal fun NavGraphBuilder.gameDestination(
    playableSizes: IntRange,
    onBack: () -> Unit,
    onScores: () -> Unit,
    onUnplayableSize: () -> Unit,
) {
    composable(
        route = GAME_ROUTE_PATTERN,
        arguments = listOf(navArgument(SIZE_ARGUMENT) { type = NavType.IntType }),
    ) { entry ->
        val size = entry.arguments?.getInt(SIZE_ARGUMENT) ?: (playableSizes.first - 1)
        if (size in playableSizes) {
            GameScreen(onBack = onBack, onScores = onScores)
        } else {
            LaunchedEffect(size) { onUnplayableSize() }
        }
    }
}
