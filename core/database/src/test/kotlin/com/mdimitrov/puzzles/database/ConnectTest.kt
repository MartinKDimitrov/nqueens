package com.mdimitrov.puzzles.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

// A table of this module's own: what opens a database is tested without borrowing a feature's.
@Entity(tableName = "notes")
internal data class NoteRow(
    val text: String,
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
)

@Dao
internal interface NoteDao {
    @Insert
    suspend fun add(row: NoteRow)

    @Query("SELECT text FROM notes")
    suspend fun all(): List<String>
}

@Database(entities = [NoteRow::class], version = 1, exportSchema = false)
internal abstract class NoteDatabase : RoomDatabase() {
    abstract fun notes(): NoteDao
}

/** The same table, a version later — what an update to the app looks like to a file on disk. */
@Database(entities = [NoteRow::class], version = 2, exportSchema = false)
internal abstract class NoteDatabaseNext : RoomDatabase() {
    abstract fun notes(): NoteDao
}

@RunWith(RobolectricTestRunner::class)
class ConnectTest {
    private val context = RuntimeEnvironment.getApplication()
    private val opened = mutableListOf<RoomDatabase>()

    @AfterTest
    fun closeWhatWasOpened() {
        opened.forEach { it.close() }
    }

    @Test
    fun `a database opened here keeps what it was given`() =
        runTest {
            val notes = connect("kept.db")

            notes.notes().add(NoteRow(text = "a solved board"))

            assertEquals(listOf("a solved board"), notes.notes().all())
        }

    @Test
    fun `two names are two databases`() =
        runTest {
            connect("one.db").notes().add(NoteRow(text = "mine"))

            assertTrue(connect("another.db").notes().all().isEmpty())
        }

    @Test
    fun `a version this build cannot migrate is refused rather than emptied`() =
        runTest {
            val name = "kept.db"
            connect(name).notes().add(NoteRow(text = "a hundred solved boards"))
            opened.forEach { it.close() }
            opened.clear()

            val next = connect(context, NoteDatabaseNext::class.java, name)
            assertFailsWith<IllegalStateException> { next.openHelper.writableDatabase }
            next.close()

            val kept = connect(name).notes().all()
            assertEquals(listOf("a hundred solved boards"), kept)
        }

    private fun connect(name: String) =
        connect(context, NoteDatabase::class.java, name)
            .also { opened += it }
}
