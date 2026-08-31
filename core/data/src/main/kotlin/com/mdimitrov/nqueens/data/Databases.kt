package com.mdimitrov.nqueens.data

import androidx.room.RoomDatabase

/**
 * How this app opens a database. The caller brings the tables and the queries; where the file
 * lives and how it is opened is settled here, once, for every feature that stores anything.
 *
 * Two databases pointed at one file corrupt each other, so each name is opened once. That is a
 * convention: nothing here keeps a register of the names it has handed out, and no check can see
 * a second caller asking for one that is already open.
 */
public interface Databases {
    public fun <T : RoomDatabase> connect(
        type: Class<T>,
        name: String,
    ): T
}
