package com.mdimitrov.nqueens.domain

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The obvious implementation: compare every pair. Slow, but plainly correct. */
private fun conflictByPairs(
    queens: Set<Cell>,
    rules: PuzzleRules = NQueens,
): Set<Cell> =
    queens.filterTo(mutableSetOf()) { queen ->
        queens.any { other -> other != queen && rules.attacks(queen, other) }
    }

class ConflictsTest {
    @Test
    fun `an empty board has no conflicts`() {
        assertTrue(conflicts(emptySet()).isEmpty())
    }

    @Test
    fun `a lone queen is not in conflict with itself`() {
        assertTrue(conflicts(setOf(Cell(0, 0))).isEmpty())
    }

    @Test
    fun `both queens sharing a row are flagged`() {
        val queens = setOf(Cell(2, 1), Cell(2, 5))
        assertEquals(queens, conflicts(queens))
    }

    @Test
    fun `only the queens actually attacking are flagged`() {
        val attacking = setOf(Cell(0, 0), Cell(3, 3))
        val safe = Cell(1, 4)
        assertEquals(attacking, conflicts(attacking + safe))
    }

    @Test
    fun `a solved four board is solved and free of conflicts`() {
        val solution = setOf(Cell(0, 1), Cell(1, 3), Cell(2, 0), Cell(3, 2))
        assertTrue(conflicts(solution).isEmpty())
        assertTrue(isSolved(solution, size = 4))
        assertEquals(0, queensLeft(solution, size = 4))
    }

    @Test
    fun `a board with too few queens is not solved`() {
        val partial = setOf(Cell(0, 1), Cell(1, 3))
        assertFalse(isSolved(partial, size = 4))
        assertEquals(2, queensLeft(partial, size = 4))
    }

    @Test
    fun `a full board with conflicts is not solved`() {
        val diagonal = setOf(Cell(0, 0), Cell(1, 1), Cell(2, 2), Cell(3, 3))
        assertEquals(4, diagonal.size)
        assertFalse(isSolved(diagonal, size = 4))
    }
}

/**
 * Counting and comparing every pair are different algorithms for the same question, so making
 * them agree on many random boards is a real check rather than a restatement of the code.
 */
class ConflictsPropertyTest {
    @Test
    fun `counting agrees with comparing every pair on random boards`() {
        val random = Random(SEED)
        repeat(TRIALS) {
            val size = random.nextInt(MIN_SIZE, MAX_SIZE + 1)
            val queens = randomQueens(size, random)
            assertEquals(conflictByPairs(queens), conflicts(queens), "disagreed on $queens")
        }
    }

    private fun randomQueens(
        size: Int,
        random: Random,
    ): Set<Cell> {
        val count = random.nextInt(0, size + 1)
        return buildSet {
            repeat(count) { add(Cell(random.nextInt(size), random.nextInt(size))) }
        }
    }

    private companion object {
        const val SEED = 27_082_026
        const val TRIALS = 500
        const val MIN_SIZE = 4
        const val MAX_SIZE = 12
    }
}
