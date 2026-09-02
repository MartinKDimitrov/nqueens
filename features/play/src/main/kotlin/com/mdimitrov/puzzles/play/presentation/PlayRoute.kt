package com.mdimitrov.puzzles.play.presentation

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mdimitrov.puzzles.puzzletype.Puzzles

private const val PLAY_ROUTE: String = "play"

public const val PUZZLE_ARGUMENT: String = "puzzle"

public const val SIZE_ARGUMENT: String = "size"

private const val PLAY_ROUTE_PATTERN: String = "$PLAY_ROUTE/{$PUZZLE_ARGUMENT}/{$SIZE_ARGUMENT}"

/** A board of this size, of this puzzle. The key is the puzzle's own and never a resource id. */
public fun playRoute(
    puzzle: String,
    size: Int,
): String = "$PLAY_ROUTE/$puzzle/$size"

/**
 * The board, declared by the shell that draws it. Which puzzle and which size are the route's,
 * and both are checked before a screen is built: a size this puzzle does not play, or a puzzle
 * this build does not have, sends the player back rather than reaching `GameState` and throwing.
 */
public fun NavGraphBuilder.playDestination(
    puzzles: Puzzles,
    onBack: () -> Unit,
    onScores: () -> Unit,
    onUnplayable: () -> Unit,
) {
    composable(
        route = PLAY_ROUTE_PATTERN,
        arguments =
            listOf(
                navArgument(PUZZLE_ARGUMENT) { type = NavType.StringType },
                navArgument(SIZE_ARGUMENT) { type = NavType.IntType },
            ),
    ) { entry ->
        val arguments = entry.arguments
        val puzzle = arguments?.getString(PUZZLE_ARGUMENT)?.let(puzzles::byKey)
        val size = arguments?.getInt(SIZE_ARGUMENT)

        if (puzzle != null && size != null && size in puzzle.sizes) {
            PlayScreen(onBack = onBack, onScores = onScores)
        } else {
            LaunchedEffect(entry.id) { onUnplayable() }
        }
    }
}
