package com.mdimitrov.puzzles.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "solves")
public data class SolveRow(
    val size: Int,
    // The column is older than the word: the app called a puzzle a variant before it called it a
    // puzzle. Renaming the column would change the schema's identity and cost a migration for
    // nothing anyone could see, so the name stays where the bytes are and the code speaks one word
    // everywhere else. Nothing has shipped yet, so nothing needs that migration today; the point is
    // that the rename buys no reader anything to pay for it later. TRADEOFFS D14.
    @ColumnInfo(name = "variant") val puzzle: String,
    val seconds: Int,
    val finishedAt: Long,
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
)
