package com.mdimitrov.nqueens.history.data

import androidx.annotation.StringRes
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "solves")
internal data class SolveRow(
    val size: Int,
    @StringRes val variant: Int,
    val seconds: Int,
    val finishedAt: Long,
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
)
