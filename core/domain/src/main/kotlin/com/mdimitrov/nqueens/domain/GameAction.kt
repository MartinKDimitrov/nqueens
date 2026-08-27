package com.mdimitrov.nqueens.domain

/** Everything the player can do. */
public sealed interface GameAction {
    /** Put a queen on an empty square, or take back the one standing there. */
    public data class Toggle(public val cell: Cell) : GameAction

    /** Clear the board without changing its size. */
    public data object Reset : GameAction

    /** Abandon this board and start new one. */
    public data class NewGame(public val size: Int) : GameAction
}
