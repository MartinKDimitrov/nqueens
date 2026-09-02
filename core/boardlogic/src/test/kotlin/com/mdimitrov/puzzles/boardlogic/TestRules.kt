package com.mdimitrov.puzzles.boardlogic

/**
 * A rule set for the family's own tests: the four lines a queen threatens along.
 *
 * The app's copy of this lives in `:games:nqueens`, where a puzzle's rules belong. It is written
 * again here because the domain cannot depend on a game, and because these tests are about what
 * `conflicts` does with a rule rather than about which rule N-Queens has.
 */
internal val FourLines: LineRules =
    LineRules { cell ->
        setOf(
            Line(TestAxis.ROW, cell.row),
            Line(TestAxis.COLUMN, cell.col),
            Line(TestAxis.DESCENDING, cell.descendingDiagonal),
            Line(TestAxis.ASCENDING, cell.ascendingDiagonal),
        )
    }
