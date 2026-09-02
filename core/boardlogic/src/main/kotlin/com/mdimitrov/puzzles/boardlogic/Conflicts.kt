package com.mdimitrov.puzzles.boardlogic

/**
 * Which pieces are threatened, and by which.
 *
 * The board paints [pieces] and nothing else, and for a long time that was all this carried. It
 * carries the pairing because the counting already knows it: two pieces are in conflict exactly
 * when they share a line, and the line's occupants are in hand at the moment the count is taken.
 * Thrown away, the only question that could be answered afterwards was "is this one in trouble";
 * kept, "with which" and "how many of them" cost nothing more.
 */
public data class Conflicts(
    /** Each threatened piece against the pieces threatening it. Nothing threatens itself. */
    public val attackers: Map<Cell, Set<Cell>>,
) {
    /** The threatened pieces. */
    public val pieces: Set<Cell> get() = attackers.keys

    public operator fun contains(cell: Cell): Boolean = cell in attackers

    public fun isEmpty(): Boolean = attackers.isEmpty()

    /** The pieces threatening the one on [cell], empty when it stands clear or holds nothing. */
    public fun attackersOf(cell: Cell): Set<Cell> = attackers[cell].orEmpty()
}

/**
 * Every piece threatened by at least one other, and by which.
 *
 * Gathers the pieces occupying each line, then reads each piece's own lines back: a line holding
 * anyone else names an attacker. One pass over the lines, as counting was.
 */
public fun conflicts(
    pieces: Set<Cell>,
    rules: LineRules,
): Conflicts {
    val linesByPiece = pieces.associateWith(rules::linesThrough)

    val occupants = HashMap<Line, MutableSet<Cell>>()
    for ((piece, lines) in linesByPiece) {
        for (line in lines) {
            occupants.getOrPut(line) { mutableSetOf() } += piece
        }
    }

    val attackers =
        linesByPiece
            .mapValues { (piece, lines) ->
                lines.flatMapTo(mutableSetOf()) { occupants.getValue(it) } - piece
            }
            .filterValues { it.isNotEmpty() }

    return Conflicts(attackers)
}

/**
 * How many pieces are still to be placed to reach [target].
 *
 * The target is the caller's, not the puzzle's: the queens want one piece per row and so pass the
 * board size, and a puzzle that wants a different number passes that instead.
 */
public fun piecesLeft(
    pieces: Set<Cell>,
    target: Int,
): Int = (target - pieces.size).coerceAtLeast(0)
