package com.mdimitrov.nqueens.history.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface SolveDao {
    // Two boards finished in the same millisecond are still ordered: the later row wins.
    @Query("SELECT * FROM solves ORDER BY finishedAt DESC, id DESC")
    fun all(): Flow<List<SolveRow>>

    @Insert
    suspend fun add(row: SolveRow)

    @Query("DELETE FROM solves WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM solves")
    suspend fun clear()

    @Query("SELECT MIN(seconds) FROM solves WHERE size = :size AND variant = :variant")
    suspend fun best(
        size: Int,
        variant: String,
    ): Int?
}
