package com.mdimitrov.puzzles.scores.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mdimitrov.puzzles.database.SolveDao
import com.mdimitrov.puzzles.database.SolveRow
import com.mdimitrov.puzzles.scores.domain.Solve
import com.mdimitrov.puzzles.scores.domain.SolveRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// The column keeps the puzzle's own key; the repository never looks inside it.
private const val ONE_PUZZLE = "queens"
private const val ANOTHER_PUZZLE = "another-puzzle"

// Room answers on its own executor, so the second emission is waited for rather than assumed.
private const val SECOND = 1_000L
private const val POLL = 5L

/**
 * The feature's own table in a database of its own, so what is tested is the queries and not the
 * app's arrangement of them. The app opens one database with this table in it; here it stands
 * alone, which is what lets this module be tested without one.
 */
@Database(entities = [SolveRow::class], version = 1, exportSchema = false)
internal abstract class SolvesOnly : RoomDatabase() {
    abstract fun solves(): SolveDao
}

@RunWith(RobolectricTestRunner::class)
class SolveRepositoryTest {
    private lateinit var database: SolvesOnly
    private lateinit var repository: SolveRepository

    @BeforeTest
    fun openAnInMemoryDatabase() {
        database =
            Room.inMemoryDatabaseBuilder(
                RuntimeEnvironment.getApplication(),
                SolvesOnly::class.java,
            ).build()
        repository = SolveRepositoryImpl(database.solves())
    }

    @AfterTest
    fun closeTheDatabase() {
        database.close()
    }

    @Test
    fun `a solved board comes back with everything it was given`() =
        runTest {
            repository.add(solve(size = 8, seconds = 84, finishedAt = 1_000L))

            val kept = repository.solves().first().single()

            assertEquals(8, kept.size)
            assertEquals(ONE_PUZZLE, kept.puzzle)
            assertEquals(84, kept.seconds)
            assertEquals(1_000L, kept.finishedAt)
            assertTrue(kept.id > 0, "the table gives the row an id of its own")
        }

    @Test
    fun `the newest solve is listed first`() =
        runTest {
            repository.add(solve(seconds = 10, finishedAt = 1_000L))
            repository.add(solve(seconds = 20, finishedAt = 3_000L))
            repository.add(solve(seconds = 30, finishedAt = 2_000L))

            assertEquals(listOf(20, 30, 10), repository.solves().first().map { it.seconds })
        }

    @Test
    fun `deleting takes its own row and leaves the rest`() =
        runTest {
            repository.add(solve(seconds = 10, finishedAt = 1_000L))
            repository.add(solve(seconds = 20, finishedAt = 2_000L))
            val doomed = repository.solves().first().single { it.seconds == 10 }

            repository.delete(doomed.id)

            assertEquals(listOf(20), repository.solves().first().map { it.seconds })
        }

    @Test
    fun `clearing empties the table`() =
        runTest {
            repository.add(solve(seconds = 10, finishedAt = 1_000L))
            repository.add(solve(seconds = 20, finishedAt = 2_000L))

            repository.clear()

            assertTrue(repository.solves().first().isEmpty())
        }

    @Test
    fun `the best time is the fastest of that size and of no other`() =
        runTest {
            repository.add(solve(size = 8, seconds = 84, finishedAt = 1_000L))
            repository.add(solve(size = 8, seconds = 72, finishedAt = 2_000L))
            repository.add(solve(size = 4, seconds = 6, finishedAt = 3_000L))

            assertEquals(72, repository.best(size = 8, puzzle = ONE_PUZZLE))
            assertEquals(6, repository.best(size = 4, puzzle = ONE_PUZZLE))
        }

    @Test
    fun `a size nobody has finished has no best time`() =
        runTest {
            repository.add(solve(size = 8, seconds = 84, finishedAt = 1_000L))

            assertNull(repository.best(size = 12, puzzle = ONE_PUZZLE))
        }

    @Test
    fun `the same board under two puzzles is two records`() =
        runTest {
            repository.add(solve(puzzle = ONE_PUZZLE, seconds = 84, finishedAt = 1_000L))
            repository.add(solve(puzzle = ANOTHER_PUZZLE, seconds = 30, finishedAt = 2_000L))

            assertEquals(
                listOf(ANOTHER_PUZZLE, ONE_PUZZLE),
                repository.solves().first().map { it.puzzle },
            )
        }

    @Test
    fun `a board of another puzzle has a best time of its own`() =
        runTest {
            repository.add(solve(size = 8, puzzle = ONE_PUZZLE, seconds = 84, finishedAt = 1_000L))
            repository.add(solve(size = 8, puzzle = ANOTHER_PUZZLE, seconds = 30, finishedAt = 2_000L))

            assertEquals(84, repository.best(size = 8, puzzle = ONE_PUZZLE))
            assertEquals(30, repository.best(size = 8, puzzle = ANOTHER_PUZZLE))
        }

    @Test
    fun `two boards finished in the same millisecond are still ordered, the later row first`() =
        runTest {
            repository.add(solve(seconds = 10, finishedAt = 1_000L))
            repository.add(solve(seconds = 20, finishedAt = 1_000L))

            assertEquals(listOf(20, 10), repository.solves().first().map { it.seconds })
        }

    @Test
    fun `the list reports itself again when the table changes`() =
        runTest {
            val seen = CopyOnWriteArrayList<List<Int>>()
            val watching =
                launch(Dispatchers.Default) {
                    repository.solves().collect { rows -> seen += rows.map { it.seconds } }
                }

            withContext(Dispatchers.Default) {
                awaitRows(seen, 1)
                repository.add(solve(seconds = 10, finishedAt = 1_000L))
                awaitRows(seen, 2)
                repository.add(solve(seconds = 20, finishedAt = 2_000L))
                awaitRows(seen, 3)
            }
            watching.cancel()

            assertEquals(listOf(emptyList(), listOf(10), listOf(20, 10)), seen.toList())
        }

    private suspend fun awaitRows(
        seen: CopyOnWriteArrayList<List<Int>>,
        count: Int,
    ) = withTimeout(SECOND) { while (seen.size < count) delay(POLL) }

    private fun solve(
        size: Int = 8,
        puzzle: String = ONE_PUZZLE,
        seconds: Int = 60,
        finishedAt: Long = 0L,
    ) = Solve(size = size, puzzle = puzzle, seconds = seconds, finishedAt = finishedAt)
}
