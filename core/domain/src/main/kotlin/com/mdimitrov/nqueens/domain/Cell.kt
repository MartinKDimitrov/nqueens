package com.mdimitrov.nqueens.domain

public data class Cell(
    public val row: Int,
    public val col: Int,
) {
    public val descendingDiagonal: Int get() = row - col
    public val ascendingDiagonal: Int get() = row + col

    public fun isOnBoard(size: Int): Boolean = row in 0 until size && col in 0 until size
}
