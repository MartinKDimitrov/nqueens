package com.mdimitrov.puzzles.nqueens

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals

private const val A_ROW = 3
private const val A_COLUMN = 5
private const val TWO_CONFLICTS = 2

/**
 * A puzzle's words cross into the shell as resource ids, so no compiler and no lint can see that
 * one of them takes an argument the screen does not pass, or that the plural is a string. This
 * module is what a second puzzle is copied from, and this is the test that copy needs most: the
 * mistake it catches would otherwise be a crash on the first piece placed.
 *
 * The arguments below are the ones the shell passes — `Board`, `TopBar`, `PlayScreen` and
 * `SetupScreen` between them. A word that wants more than it is given throws here.
 */
@RunWith(RobolectricTestRunner::class)
class QueensTextTest {
    private val resources = RuntimeEnvironment.getApplication().resources

    @Test
    fun `every word this puzzle offers takes the arguments the screens pass it`() {
        val spoken =
            mapOf(
                "name" to resources.getString(Queens.name),
                "subtitle" to resources.getString(Queens.text.subtitle),
                "counter" to resources.getString(Queens.text.counter),
                "idle" to resources.getString(Queens.text.idle),
                "cell" to resources.getString(Queens.text.cell, A_ROW, A_COLUMN),
                "cellUnderAttack" to resources.getString(Queens.text.cellUnderAttack, A_ROW, A_COLUMN),
                "conflict" to resources.getQuantityString(Queens.text.conflict, TWO_CONFLICTS, TWO_CONFLICTS),
            )

        assertEquals(
            emptyMap(),
            spoken.filterValues { it.contains('%') },
            "words left holding an argument no screen passes them",
        )
    }

    @Test
    fun `the conflict count has a wording of its own for one`() {
        // The count is held still and only the quantity moves, so what differs is the plural rule
        // rather than the number substituted into it. Comparing "1 …" with "2 …" would pass for a
        // plural whose `one` item had been deleted, which is the mistake a copied module makes.
        val forOne = resources.getQuantityString(Queens.text.conflict, 1, TWO_CONFLICTS)
        val forSeveral = resources.getQuantityString(Queens.text.conflict, TWO_CONFLICTS, TWO_CONFLICTS)

        assertEquals(false, forOne == forSeveral, "a plural that reads the same for one and for two is not a plural")
    }
}
