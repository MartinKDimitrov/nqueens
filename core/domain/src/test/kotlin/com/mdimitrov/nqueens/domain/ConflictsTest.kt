package com.mdimitrov.nqueens.domain

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The obvious implementation: compare every pair. Slow, but plainly correct. */
private fun conflictByPairs(
    queens: Set<Cell>,
    rules: PuzzleRules,
): Set<Cell> =
    queens.filterTo(mutableSetOf()) { queen ->
        queens.any { other -> other != queen && rules.attacks(queen, other) }
    }

class ConflictsTest {
    @Test
    fun `an empty board has no conflicts`() {
        assertTrue(conflicts(emptySet(), NQueensLines).isEmpty())
    }

    @Test
    fun `a lone queen is not in conflict with itself`() {
        assertTrue(conflicts(setOf(Cell(0, 0)), NQueensLines).isEmpty())
    }

    @Test
    fun `both queens sharing a row are flagged`() {
        val queens = setOf(Cell(2, 1), Cell(2, 5))
        assertEquals(queens, conflicts(queens, NQueensLines))
    }

    @Test
    fun `only the queens actually attacking are flagged`() {
        val attacking = setOf(Cell(0, 0), Cell(3, 3))
        val safe = Cell(1, 4)
        assertEquals(attacking, conflicts(attacking + safe, NQueensLines))
    }

    @Test
    fun `a solution has no conflicts`() {
        val solution = setOf(Cell(0, 1), Cell(1, 3), Cell(2, 0), Cell(3, 2))
        assertTrue(conflicts(solution, NQueensLines).isEmpty())
    }

    @Test
    fun `the counter falls as queens are placed`() {
        assertEquals(4, piecesLeft(emptySet(), target = 4))
        assertEquals(2, piecesLeft(setOf(Cell(0, 1), Cell(1, 3)), target = 4))
        assertEquals(0, piecesLeft(setOf(Cell(0, 1), Cell(1, 3), Cell(2, 0), Cell(3, 2)), target = 4))
    }

    @Test
    fun `the counter stops at zero when more queens are placed than the board asks for`() {
        val crowded = setOf(Cell(0, 0), Cell(1, 1), Cell(2, 2), Cell(3, 3), Cell(0, 1))
        assertEquals(0, piecesLeft(crowded, target = 4))
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
            val size = random.nextInt(SMALLEST_BOARD, LARGEST_BOARD + 1)
            val queens = randomQueens(size, random)
            assertEquals(conflictByPairs(queens, NQueens), conflicts(queens, NQueensLines), "disagreed on $queens")
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
        const val SMALLEST_BOARD = MIN_BOARD_SIZE

        /**
         * The largest board an app would put in front of a player. The domain cannot see
         * `LARGEST_PLAYABLE_BOARD`, so the number is repeated here; widen it if that one moves.
         */
        const val LARGEST_BOARD = 12
    }
}
