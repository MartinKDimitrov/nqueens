package com.mdimitrov.nqueens.history.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "solves")
internal data class SolveRow(
    val size: Int,
    val variant: String,
    val seconds: Int,
    val finishedAt: Long,
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
)
