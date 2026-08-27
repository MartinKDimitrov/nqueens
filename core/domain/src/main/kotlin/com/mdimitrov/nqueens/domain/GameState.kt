package com.mdimitrov.nqueens.domain

public const val MIN_BOARD_SIZE: Int = 4

public data class GameState(
    public val size: Int,
    public val queens: Set<Cell> = emptySet(),
) {
    init {
        require(size >= MIN_BOARD_SIZE) { "Board size must be at least $MIN_BOARD_SIZE, was $size" }
        require(queens.all { it.row in 0 until size && it.col in 0 until size }) {
            "Every queen must stand on the board"
        }
    }
}
