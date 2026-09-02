package com.mdimitrov.puzzles.puzzletype

import androidx.annotation.DrawableRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.mdimitrov.puzzles.boardlogic.LineRules

/**
 * One puzzle of the family this app plays: place pieces on a square board so that no two of them
 * threaten each other. What separates one such puzzle from another is here and nowhere else — the rules, the
 * words, the glyph — so a second puzzle is a module that provides one of these and changes
 * nothing that draws it.
 */
public data class Puzzle(
    /**
     * How a record names this puzzle. It is the puzzle's own word and never a resource id: ids
     * are assigned when the resource table is built and move whenever a string is added or
     * removed, so a row written by one build would name a different puzzle in the next
     * (TRADEOFFS D14).
     */
    public val key: String,
    @StringRes public val name: Int,
    @DrawableRes public val piece: Int,
    /** The sizes it is worth playing at. `Puzzles` refuses a range the domain cannot build. */
    public val sizes: IntRange,
    /**
     * How many pieces solve a board of that size — the goal, which the rules do not carry.
     * The queens want one per row; a puzzle whose pieces threaten differently may not. Without
     * this on the puzzle, the
     * shell would be deciding what solving means for a game it knows nothing about.
     */
    public val piecesToSolve: (size: Int) -> Int,
    /**
     * What its pieces threaten along, on a board of that size.
     *
     * A function of the size rather than one value, for the same reason [piecesToSolve] is: a
     * rule may depend on the board it is played on. Nothing in this build needs that yet — the
     * queens ignore the argument — but a rule that had to see the board could not be given one
     * afterwards without changing every game module at once.
     */
    public val rules: (size: Int) -> LineRules,
    public val text: PuzzleText,
)

/** Everything the screens say about the piece this puzzle is played with. Resource ids. */
public data class PuzzleText(
    @StringRes public val subtitle: Int,
    @StringRes public val counter: Int,
    @StringRes public val cell: Int,
    @StringRes public val cellUnderAttack: Int,
    @StringRes public val idle: Int,
    @PluralsRes public val conflict: Int,
)
