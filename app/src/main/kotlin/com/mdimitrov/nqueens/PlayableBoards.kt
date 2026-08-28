package com.mdimitrov.nqueens

/**
 * The largest board this app will play. Above it the squares get too small to tap reliably and
 * the grid too large to draw comfortably.
 *
 * It sits above the screens because both of them answer to it: Setup will not offer a larger
 * board, and the game route will not accept one. It is a decision about this app, not about the
 * puzzle — `:core:domain` holds boards far beyond it.
 */
public const val LARGEST_PLAYABLE_BOARD: Int = 12
