package com.mdimitrov.nqueens.domain

private fun Set<Cell>.toggle(cell: Cell): Set<Cell> = if (cell in this) this - cell else this + cell

public fun reduce(
    state: GameState,
    action: GameAction,
): GameState =
    when (action) {
        is GameAction.Toggle -> state.copy(queens = state.queens.toggle(action.cell))
        is GameAction.NewGame -> GameState(size = action.size)
        GameAction.Reset -> state.copy(queens = emptySet())
    }
