package com.mdimitrov.nqueens.data

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
import kotlin.test.assertTrue

// A table of this module's own: what opens a connection is tested without borrowing a feature's.
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

@RunWith(RobolectricTestRunner::class)
class RoomDatabasesTest {
    private val databases: Databases = RoomDatabases(RuntimeEnvironment.getApplication())
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

    private fun connect(name: String) = databases.connect(NoteDatabase::class.java, name).also { opened += it }
}
