package com.mdimitrov.nqueens.domain

public const val MIN_BOARD_SIZE: Int = 4

/**
 * The largest board the domain will hold. The puzzle has solutions far beyond it; this is where
 * a board stops being one that can be drawn. [snapshotOf] produces an entry per square, so past
 * roughly a million squares the projection is no longer something to build in one go — and a
 * state that cannot be projected is a state that would crash whatever asked for it.
 */
public const val MAX_BOARD_SIZE: Int = 1024

public data class GameState(
    public val size: Int,
    public val pieces: Set<Cell> = emptySet(),
    public val elapsedSeconds: Int = 0,
) {
    init {
        require(size in MIN_BOARD_SIZE..MAX_BOARD_SIZE) {
            "Board size must be between $MIN_BOARD_SIZE and $MAX_BOARD_SIZE, was $size"
        }
        val offBoard = pieces.firstOrNull { !it.isOnBoard(size) }
        require(offBoard == null) {
            "Piece $offBoard is outside a ${size}x$size board"
        }
        require(elapsedSeconds >= 0) {
            "Elapsed seconds cannot be negative, was $elapsedSeconds"
        }
    }
}
