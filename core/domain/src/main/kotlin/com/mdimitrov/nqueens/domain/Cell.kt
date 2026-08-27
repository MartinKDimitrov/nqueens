package com.mdimitrov.nqueens.domain

public data class Cell(
    public val row: Int,
    public val col: Int,
) {
    public val descendingDiagonal: Int get() = row - col
    public val ascendingDiagonal: Int get() = row + col
}
