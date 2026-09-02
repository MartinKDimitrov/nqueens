package com.mdimitrov.puzzles.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * How every database in this module is opened.
 *
 * No destructive fallback: a version this build cannot migrate throws rather than empties the
 * table. Somebody's hundred solved boards are not ours to delete on an upgrade.
 */
internal fun <T : RoomDatabase> connect(
    context: Context,
    type: Class<T>,
    name: String,
): T = Room.databaseBuilder(context, type, name).build()
