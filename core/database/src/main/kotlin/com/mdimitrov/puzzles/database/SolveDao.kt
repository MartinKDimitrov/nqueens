package com.mdimitrov.puzzles.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
public interface SolveDao {
    // Two boards finished in the same millisecond are still ordered: the later row wins.
    @Query("SELECT * FROM solves ORDER BY finishedAt DESC, id DESC")
    public fun all(): Flow<List<SolveRow>>

    @Insert
    public suspend fun add(row: SolveRow)

    @Query("DELETE FROM solves WHERE id = :id")
    public suspend fun delete(id: Long)

    @Query("DELETE FROM solves")
    public suspend fun clear()

    @Query("SELECT MIN(seconds) FROM solves WHERE size = :size AND variant = :puzzle")
    public suspend fun best(
        size: Int,
        puzzle: String,
    ): Int?
}
