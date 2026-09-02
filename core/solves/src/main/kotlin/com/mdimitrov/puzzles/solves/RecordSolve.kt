package com.mdimitrov.puzzles.solves

/** A board somebody finished: which puzzle, how big, and how long it took. */
public data class SolvedBoard(
    public val puzzle: String,
    public val size: Int,
    public val seconds: Int,
)

/**
 * Writing down a finished board, for a feature that produces one but does not keep it.
 *
 * This is the whole of what a game knows about records. Where they are kept, what else is kept
 * beside them and who may delete them are questions on the other side of this interface, and a
 * game cannot ask them.
 */
public fun interface RecordSolve {
    /**
     * Writes the board down and answers with the best time for that puzzle at that size *before*
     * this one, or null while none was finished. The order matters and is settled here rather
     * than at the call site: read first, or the answer finds the board it is about to write.
     */
    public suspend fun record(board: SolvedBoard): Int?
}
