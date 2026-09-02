package com.mdimitrov.puzzles.puzzletype

import com.mdimitrov.puzzles.boardlogic.MAX_BOARD_SIZE
import com.mdimitrov.puzzles.boardlogic.MIN_BOARD_SIZE
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The puzzles this build was assembled with. Each game module contributes one into the set and
 * knows nothing of the others; what reads this knows how many there are and never which.
 *
 * A game is added by depending on its module. No code here, in the shell or in the app names a
 * puzzle, so what adding one costs in Kotlin is two lines: the `include` and the app's dependency
 * on it. What does still name one is the app's own label and its launcher icon, which say
 * "N-Queens" and draw a queen, and which a build shipping a second puzzle has to answer for.
 */
@Singleton
public class Puzzles
    @Inject
    constructor(
        installed: Set<@JvmSuppressWildcards Puzzle>,
    ) {
        /** In a stable order, so a build's list of games does not depend on how a set iterated. */
        public val all: List<Puzzle> = installed.sortedBy { it.key }

        init {
            require(all.isNotEmpty()) { "No puzzle is installed: the app has nothing to play." }

            // Two puzzles under one key would write into each other's records for the rest of
            // the table's life, silently, and no type could tell them apart afterwards.
            val keys = all.map { it.key }
            require(keys.toSet().size == keys.size) {
                "Two puzzles share a key, so their records would be each other's: $keys"
            }

            all.forEach(::check)
        }

        /**
         * What a game module can get wrong is caught here, at assembly, rather than on the screen
         * that would have to survive it. Each of these has a way of failing that is quiet: a key
         * that is not a path segment makes Start do nothing for ever, and a size the domain
         * refuses reaches `GameState` and throws during composition.
         */
        private fun check(puzzle: Puzzle) {
            require(puzzle.key.matches(KEY)) {
                "The puzzle key \"${puzzle.key}\" is also a route segment and a stored value, so it " +
                    "must be lower-case letters, digits or a hyphen."
            }
            require(!puzzle.sizes.isEmpty()) {
                "The puzzle \"${puzzle.key}\" plays no board at all: its sizes are ${puzzle.sizes}."
            }
            require(puzzle.sizes.first >= MIN_BOARD_SIZE && puzzle.sizes.last <= MAX_BOARD_SIZE) {
                "The puzzle \"${puzzle.key}\" offers ${puzzle.sizes}, which reaches outside the " +
                    "boards the domain will build ($MIN_BOARD_SIZE..$MAX_BOARD_SIZE)."
            }

            puzzle.sizes.forEach { size ->
                val target = puzzle.piecesToSolve(size)
                require(target in 1..size * size) {
                    "The puzzle \"${puzzle.key}\" is solved by $target pieces on a ${size}x$size " +
                        "board, which is a game that cannot be finished."
                }
            }
        }

        private companion object {
            /** What a URL path segment and a database value can both carry without escaping. */
            val KEY = Regex("[a-z0-9-]+")
        }

        /** The puzzle a route named, or null if this build does not have it. */
        public fun byKey(key: String): Puzzle? = all.firstOrNull { it.key == key }
    }
