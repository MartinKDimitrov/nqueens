package com.mdimitrov.nqueens.domain

public fun reduce(
    state: GameState,
    action: GameAction,
): GameState =
    when (action) {
        is GameAction.Toggle ->
            if (action.cell.isOnBoard(state.size)) {
                state.copy(pieces = state.pieces.toggle(action.cell))
            } else {
                state
            }

        is GameAction.NewGame ->
            if (action.size in MIN_BOARD_SIZE..MAX_BOARD_SIZE) {
                GameState(size = action.size)
            } else {
                state
            }

        // The count stops rather than wraps: `reduce` is total, and `GameState` refuses a
        // negative elapsed time, so an unchecked increment would throw after Int.MAX_VALUE ticks.
        GameAction.Tick ->
            if (state.elapsedSeconds == Int.MAX_VALUE) state else state.copy(elapsedSeconds = state.elapsedSeconds + 1)

        GameAction.Reset -> state.copy(pieces = emptySet(), elapsedSeconds = 0)
    }

private fun Set<Cell>.toggle(cell: Cell): Set<Cell> = if (cell in this) this - cell else this + cell
