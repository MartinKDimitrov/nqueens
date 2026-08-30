package com.mdimitrov.nqueens.data

import androidx.room.RoomDatabase

/**
 * How this app opens a database. The caller brings the tables and the queries; where the file
 * lives and how it is opened is settled here, once, for every feature that stores anything.
 *
 * Two databases must not be pointed at one file, so a feature that asks for a connection asks
 * for a name of its own.
 */
public interface Databases {
    public fun <T : RoomDatabase> connect(
        type: Class<T>,
        name: String,
    ): T
}
