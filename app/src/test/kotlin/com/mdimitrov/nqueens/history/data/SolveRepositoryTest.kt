package com.mdimitrov.nqueens.history.data

import androidx.room.Room
import com.mdimitrov.nqueens.R
import com.mdimitrov.nqueens.history.domain.Solve
import com.mdimitrov.nqueens.history.domain.SolveRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// The column keeps the resource that names the variant; the repository never looks inside it.
private val OneVariant = R.string.variant_queens
private val AnotherVariant = R.string.app_name

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
            assertEquals(OneVariant, kept.variant)
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

            assertEquals(72, repository.best(size = 8))
            assertEquals(6, repository.best(size = 4))
        }

    @Test
    fun `a size nobody has finished has no best time`() =
        runTest {
            repository.add(solve(size = 8, seconds = 84, finishedAt = 1_000L))

            assertNull(repository.best(size = 12))
        }

    @Test
    fun `the same board under two variants is two records`() =
        runTest {
            repository.add(solve(variant = OneVariant, seconds = 84, finishedAt = 1_000L))
            repository.add(solve(variant = AnotherVariant, seconds = 30, finishedAt = 2_000L))

            assertEquals(
                listOf(AnotherVariant, OneVariant),
                repository.solves().first().map { it.variant },
            )
        }

    private fun solve(
        size: Int = 8,
        variant: Int = OneVariant,
        seconds: Int = 60,
        finishedAt: Long = 0L,
    ) = Solve(size = size, variant = variant, seconds = seconds, finishedAt = finishedAt)
}
