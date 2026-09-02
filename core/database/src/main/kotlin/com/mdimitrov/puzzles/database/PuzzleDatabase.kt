package com.mdimitrov.puzzles.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * One database for the app. Room needs every entity declared in one place, so the tables live
 * here with it rather than in the features that read them; each feature is handed the accessor
 * for its own table by `DaoModule` and never sees this class.
 *
 * The schema is exported to `core/database/schemas/`, which is what a first migration will be written and
 * tested against. There is none yet: nothing has been released, so no file on any device holds a
 * version this build would have to move forward.
 */
@Database(entities = [SolveRow::class], version = 1, exportSchema = true)
internal abstract class PuzzleDatabase : RoomDatabase() {
    abstract fun solves(): SolveDao
}
