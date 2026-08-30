package com.mdimitrov.nqueens.domain

public enum class CellStatus { EMPTY, PIECE, PIECE_CONFLICT }

/** The grid the board draws, and the two counts it shows, worked out once per change of state. */
public data class BoardSnapshot(
    public val size: Int,
    public val statuses: List<CellStatus>,
    public val piecesLeft: Int,
    public val piecesUnderAttack: Int,
    public val isSolved: Boolean,
) {
    public fun statusAt(
        row: Int,
        col: Int,
    ): CellStatus {
        require(row in 0 until size && col in 0 until size) {
            "No square at row $row, column $col on a ${size}x$size board"
        }
        return statuses[row * size + col]
    }
}

/** Projects a [state] onto the grid the board draws. */
public fun snapshotOf(
    state: GameState,
    rules: LineRules,
): BoardSnapshot {
    val conflicting = conflicts(state.pieces, rules)
    val statuses =
        buildList(state.size * state.size) {
            for (row in 0 until state.size) {
                for (col in 0 until state.size) {
                    val cell = Cell(row, col)
                    add(
                        when (cell) {
                            in conflicting -> CellStatus.PIECE_CONFLICT
                            in state.pieces -> CellStatus.PIECE
                            else -> CellStatus.EMPTY
                        },
                    )
                }
            }
        }

    return BoardSnapshot(
        size = state.size,
        statuses = statuses,
        piecesLeft = piecesLeft(state.pieces, target = state.size),
        piecesUnderAttack = conflicting.size,
        isSolved = state.pieces.size == state.size && conflicting.isEmpty(),
    )
}
