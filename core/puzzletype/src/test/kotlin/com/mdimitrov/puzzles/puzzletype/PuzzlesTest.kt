package com.mdimitrov.puzzles.puzzletype

import com.mdimitrov.puzzles.boardlogic.Line
import com.mdimitrov.puzzles.boardlogic.LineKind
import com.mdimitrov.puzzles.boardlogic.LineRules
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** One kind, because these tests only need a line to exist, not to mean anything. */
private enum class AnyAxis : LineKind { ROW, }

private val AnyRules = LineRules { cell -> setOf(Line(AnyAxis.ROW, cell.row)) }

// The ids stand for resources this module has none of and this test never resolves: what is
// under test is which puzzles a build may be assembled from, not what they say.
@Suppress("ResourceType")
private val AnyText =
    PuzzleText(subtitle = 1, counter = 2, cell = 3, cellUnderAttack = 4, idle = 5, conflict = 6)

@Suppress("ResourceType")
private fun puzzle(
    key: String,
    sizes: IntRange = 4..12,
    piecesToSolve: (Int) -> Int = { it },
) = Puzzle(
    key = key,
    name = 1,
    piece = 2,
    sizes = sizes,
    piecesToSolve = piecesToSolve,
    rules = { AnyRules },
    text = AnyText,
)

/**
 * What a game module can get wrong, refused where every game is assembled rather than on the
 * screen that would have to survive it.
 */
class PuzzlesTest {
    @Test
    fun `the installed puzzles are listed in a stable order`() {
        // Handed over in the wrong order on purpose. A set built in the order the answer wants is
        // a fixture that supplies the sorting for free — the assertion then passes whether the
        // list is sorted or handed straight back, which is the one thing it exists to tell apart.
        // What the app is really handed is a Dagger multibinding, whose iteration order is nobody's
        // to predict.
        val puzzles = Puzzles(setOf(puzzle("queens"), puzzle("another-puzzle")))

        assertEquals(listOf("another-puzzle", "queens"), puzzles.all.map { it.key })
    }

    @Test
    fun `a puzzle is found by the key a route carries, and an absent one is not`() {
        val puzzles = Puzzles(setOf(puzzle("queens")))

        assertEquals("queens", puzzles.byKey("queens")?.key)
        assertNull(puzzles.byKey("another-puzzle"))
    }

    @Test
    fun `a build with no puzzle at all is refused`() {
        assertFailsWith<IllegalArgumentException> { Puzzles(emptySet()) }
    }

    @Test
    fun `two puzzles under one key are refused, because their records would be each other's`() {
        assertFailsWith<IllegalArgumentException> {
            Puzzles(setOf(puzzle("queens"), puzzle("queens", sizes = 4..6)))
        }
    }

    @Test
    fun `a key a route could not carry is refused`() {
        for (key in listOf("queens/classic", "n queens", "Queens", "queens!")) {
            val refused = assertFailsWith<IllegalArgumentException> { Puzzles(setOf(puzzle(key))) }

            assertContains(refused.message.orEmpty(), key, message = "the refusal does not name \"$key\"")
        }
    }

    @Test
    fun `a puzzle that plays no board is refused`() {
        // Written this way round rather than as `12..4`, which static analysis reads as a
        // mistake — here it is the mistake under test: a game module with its bounds transposed.
        val transposed = IntRange(12, 4)

        val refused =
            assertFailsWith<IllegalArgumentException> { Puzzles(setOf(puzzle("queens", sizes = transposed))) }

        assertContains(refused.message.orEmpty(), "queens", message = "the refusal does not name the puzzle")
    }

    @Test
    fun `a puzzle nobody could finish is refused`() {
        val refused =
            assertFailsWith<IllegalArgumentException> {
                Puzzles(setOf(puzzle("queens", sizes = 4..4, piecesToSolve = { it * it + 1 })))
            }

        assertContains(refused.message.orEmpty(), "cannot be finished")
    }

    @Test
    fun `a puzzle solved by no pieces at all is refused`() {
        assertFailsWith<IllegalArgumentException> {
            Puzzles(setOf(puzzle("queens", piecesToSolve = { 0 })))
        }
    }

    @Test
    fun `a puzzle offering a board the domain will not build is refused`() {
        val refused =
            assertFailsWith<IllegalArgumentException> { Puzzles(setOf(puzzle("queens", sizes = 4..2000))) }

        assertContains(refused.message.orEmpty(), "4..2000", message = "the refusal does not say what was offered")
    }
}
