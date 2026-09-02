package com.mdimitrov.puzzles.boardlogic

/** Everything the player can do. */
public sealed interface GameAction {
    /** Put a piece on an empty square, or take back the one standing there. */
    public data class Toggle(public val cell: Cell) : GameAction

    /** Clear the board without changing its size. */
    public data object Reset : GameAction

    /** One unit of elapsed time. The caller decides the cadence; the state only counts. */
    public data object Tick : GameAction
}
