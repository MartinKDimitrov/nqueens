package com.mdimitrov.puzzles.database

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The one database, and the only place that asks whether it actually holds each feature's table
 * and hands out its DAO. A feature tests its queries against a database of its own, which is what
 * lets it be tested without this module; nothing there would notice a table left out of here.
 */
@RunWith(RobolectricTestRunner::class)
class PuzzleDatabaseTest {
    private val database =
        Room
            .inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), PuzzleDatabase::class.java)
            .build()

    @AfterTest
    fun closeTheDatabase() {
        database.close()
    }

    @Test
    fun `the scores feature's table is in it, and its rows come back through its own DAO`() =
        runTest {
            val solves = database.solves()

            solves.add(SolveRow(size = 8, puzzle = "queens", seconds = 84, finishedAt = 1L))

            val kept = solves.all().first().single()
            assertEquals(8, kept.size)
            assertEquals("queens", kept.puzzle)
            assertEquals(84, kept.seconds)
        }
}
