package com.mdimitrov.nqueens.storage

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mdimitrov.nqueens.history.data.SolveDao
import com.mdimitrov.nqueens.history.data.SolveRow

/**
 * The app's database — there is one, and a feature that wants storage adds its table to it rather
 * than opening a file of its own. That is why this class sits above the features and names each
 * of their tables: Room needs every entity of a database declared in one place, and the accessor
 * a feature reads its own table through is handed out beside it.
 *
 * The schema is exported to `app/schemas/`, which is what a first migration will be written and
 * tested against. There is none yet: nothing has been released, so no file on any device holds a
 * version this build would have to move forward.
 */
@Database(entities = [SolveRow::class], version = 1, exportSchema = true)
internal abstract class PuzzleDatabase : RoomDatabase() {
    abstract fun solves(): SolveDao
}
