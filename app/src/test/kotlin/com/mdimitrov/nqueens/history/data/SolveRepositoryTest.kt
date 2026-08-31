package com.mdimitrov.nqueens.history.data

import androidx.room.Room
import com.mdimitrov.nqueens.history.domain.Solve
import com.mdimitrov.nqueens.history.domain.SolveRepository
import com.mdimitrov.nqueens.storage.PuzzleDatabase
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
private const val ONE_VARIANT = "queens"
private const val ANOTHER_VARIANT = "rooks"

// Room answers on its own executor, so the second emission is waited for rather than assumed.
private const val SECOND = 1_000L
private const val POLL = 5L

@RunWith(RobolectricTestRunner::class)
class SolveRepositoryTest {
    private lateinit var database: PuzzleDatabase
    private lateinit var repository: SolveRepository

    @BeforeTest
    fun openAnInMemoryDatabase() {
        database =
            Room.inMemoryDatabaseBuilder(
                RuntimeEnvironment.getApplication(),
                PuzzleDatabase::class.java,
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
            assertEquals(ONE_VARIANT, kept.variant)
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

            assertEquals(72, repository.best(size = 8, variant = ONE_VARIANT))
            assertEquals(6, repository.best(size = 4, variant = ONE_VARIANT))
        }

    @Test
    fun `a size nobody has finished has no best time`() =
        runTest {
            repository.add(solve(size = 8, seconds = 84, finishedAt = 1_000L))

            assertNull(repository.best(size = 12, variant = ONE_VARIANT))
        }

    @Test
    fun `the same board under two variants is two records`() =
        runTest {
            repository.add(solve(variant = ONE_VARIANT, seconds = 84, finishedAt = 1_000L))
            repository.add(solve(variant = ANOTHER_VARIANT, seconds = 30, finishedAt = 2_000L))

            assertEquals(
                listOf(ANOTHER_VARIANT, ONE_VARIANT),
                repository.solves().first().map { it.variant },
            )
        }

    @Test
    fun `a board of another variant has a best time of its own`() =
        runTest {
            repository.add(solve(size = 8, variant = ONE_VARIANT, seconds = 84, finishedAt = 1_000L))
            repository.add(solve(size = 8, variant = ANOTHER_VARIANT, seconds = 30, finishedAt = 2_000L))

            assertEquals(84, repository.best(size = 8, variant = ONE_VARIANT))
            assertEquals(30, repository.best(size = 8, variant = ANOTHER_VARIANT))
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
        variant: String = ONE_VARIANT,
        seconds: Int = 60,
        finishedAt: Long = 0L,
    ) = Solve(size = size, variant = variant, seconds = seconds, finishedAt = finishedAt)
}
