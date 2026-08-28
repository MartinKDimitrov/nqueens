package com.mdimitrov.nqueens.domain

public fun reduce(
    state: GameState,
    action: GameAction,
): GameState =
    when (action) {
        is GameAction.Toggle ->
            if (action.cell.isOnBoard(state.size)) {
                state.copy(queens = state.queens.toggle(action.cell))
            } else {
                state
            }

        is GameAction.NewGame ->
            if (action.size in MIN_BOARD_SIZE..MAX_BOARD_SIZE) {
                GameState(size = action.size)
            } else {
                state
            }

        GameAction.Reset -> state.copy(queens = emptySet())
    }

private fun Set<Cell>.toggle(cell: Cell): Set<Cell> = if (cell in this) this - cell else this + cell
