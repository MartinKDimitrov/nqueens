package com.mdimitrov.nqueens.domain

public enum class CellStatus { EMPTY, QUEEN, QUEEN_CONFLICT }

/** Everything the board needs in order to draw itself, worked out once per change of state. */
public data class BoardSnapshot(
    public val size: Int,
    public val statuses: List<CellStatus>,
    public val queensLeft: Int,
    public val isSolved: Boolean,
) {
    public fun statusAt(
        row: Int,
        col: Int,
    ): CellStatus = statuses[row * size + col]
}

/** Projects a [state] onto the grid the board draws. */
public fun snapshotOf(
    state: GameState,
    rules: LineRules = NQueensLines,
): BoardSnapshot {
    val conflicting = conflicts(state.queens, rules)
    val statuses =
        buildList(state.size * state.size) {
            for (row in 0 until state.size) {
                for (col in 0 until state.size) {
                    val cell = Cell(row, col)
                    add(
                        when (cell) {
                            in conflicting -> CellStatus.QUEEN_CONFLICT
                            in state.queens -> CellStatus.QUEEN
                            else -> CellStatus.EMPTY
                        },
                    )
                }
            }
        }

    return BoardSnapshot(
        size = state.size,
        statuses = statuses,
        queensLeft = queensLeft(state.queens, state.size),
        isSolved = isSolved(state.queens, state.size, rules),
    )
}
