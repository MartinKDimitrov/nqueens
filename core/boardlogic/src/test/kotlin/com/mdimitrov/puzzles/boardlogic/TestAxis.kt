package com.mdimitrov.puzzles.boardlogic

/**
 * Four directions for the family's own tests to count along.
 *
 * The domain names no directions of its own — a kind of line belongs to the puzzle that threatens
 * along it — so the tests that need a rule declare theirs here, naming no puzzle.
 */
internal enum class TestAxis : LineKind { ROW, COLUMN, DESCENDING, ASCENDING }
