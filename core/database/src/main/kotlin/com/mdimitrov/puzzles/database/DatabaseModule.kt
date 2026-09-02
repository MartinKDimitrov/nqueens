package com.mdimitrov.puzzles.database

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** The file every table in this module lives in. */
private const val DATABASE_FILE = "puzzle.db"

/** The one database, opened once for the life of the process. */
@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun database(
        @ApplicationContext context: Context,
    ): PuzzleDatabase = connect(context, PuzzleDatabase::class.java, DATABASE_FILE)
}
