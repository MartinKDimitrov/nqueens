package com.mdimitrov.nqueens.history.data

import androidx.room.Database
import androidx.room.RoomDatabase

/** The puzzle's own database. Today it holds the solved boards; a later table is listed here. */
@Database(entities = [SolveRow::class], version = 1, exportSchema = false)
internal abstract class PuzzleDatabase : RoomDatabase() {
    abstract fun solves(): SolveDao
}
