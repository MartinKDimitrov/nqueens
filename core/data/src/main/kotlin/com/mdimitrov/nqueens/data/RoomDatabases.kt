package com.mdimitrov.nqueens.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

// No destructive fallback: a version this build cannot migrate throws rather than empties the
// table. Somebody's hundred solved boards are not ours to delete on an upgrade.
internal class RoomDatabases
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : Databases {
        override fun <T : RoomDatabase> connect(
            type: Class<T>,
            name: String,
        ): T = Room.databaseBuilder(context, type, name).build()
    }
