package com.mdimitrov.puzzles.nqueens

import com.mdimitrov.puzzles.boardlogic.LineKind

/**
 * The directions a queen threatens along.
 *
 * Declared here and not in the domain because they are queens' geometry: a puzzle whose pieces
 * threaten along something else names its own kinds, and the two sets can never be confused,
 * because a `Line` carries the kind itself rather than a number standing for one.
 */
internal enum class QueenAxis : LineKind { ROW, COLUMN, DESCENDING_DIAGONAL, ASCENDING_DIAGONAL }
