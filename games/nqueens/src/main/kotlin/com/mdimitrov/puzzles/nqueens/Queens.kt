package com.mdimitrov.puzzles.nqueens

import com.mdimitrov.puzzles.puzzletype.Puzzle
import com.mdimitrov.puzzles.puzzletype.PuzzleText

/**
 * The smallest board this puzzle has a solution on. Two and three queens cannot be placed on
 * their own boards at all, so below four there is no game to play — a fact about queens, which
 * is why it is here and not in the domain.
 */
private const val SMALLEST_SOLVABLE = 4

/**
 * The largest board this app will play it on. Above it the squares get too small to tap reliably
 * and the grid too large to draw comfortably.
 */
private const val LARGEST_PLAYABLE = 12

public val Queens: Puzzle =
    Puzzle(
        key = "queens",
        name = R.string.nqueens_name,
        piece = R.drawable.nqueens_ic_queen,
        sizes = SMALLEST_SOLVABLE..LARGEST_PLAYABLE,
        // One queen per row, which is the same as one per board size.
        piecesToSolve = { size -> size },
        rules = NQueensLines,
        text =
            PuzzleText(
                subtitle = R.string.nqueens_subtitle,
                counter = R.string.nqueens_counter,
                cell = R.string.nqueens_cell,
                cellUnderAttack = R.string.nqueens_cell_attacked,
                idle = R.string.nqueens_idle,
                conflict = R.plurals.nqueens_conflict,
            ),
    )
