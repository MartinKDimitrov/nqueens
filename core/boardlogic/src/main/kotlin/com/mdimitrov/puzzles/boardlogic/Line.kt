package com.mdimitrov.puzzles.boardlogic

/**
 * What kind of line two squares can share.
 *
 * An interface and not an enum, because the kinds belong to the puzzle. The queens' four axes are
 * declared where the queens are; a game whose pieces threaten along something else declares its
 * own, and two games' kinds can never be mistaken for one another because a [Line] carries the
 * kind itself rather than a number naming one.
 *
 * A kind need not describe a direction. A line is whatever squares a rule says share one, and a
 * pair is a line: name it by one of its two squares and the step between them, and both ends
 * compute the same [Line] while no third square joins it. That is how a threat which is not a
 * direction — a knight's leap — is counted by the same `conflicts` as a queen's row.
 */
public interface LineKind

/** One line of attack: all squares with the same [kind] and [index] threaten one another. */
public data class Line(
    public val kind: LineKind,
    public val index: Int,
)

/** Rules expressed structurally, as the lines a square belongs to. */
public fun interface LineRules {
    public fun linesThrough(cell: Cell): Set<Line>
}
