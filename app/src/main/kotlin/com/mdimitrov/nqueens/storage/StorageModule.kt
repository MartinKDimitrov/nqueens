package com.mdimitrov.nqueens.storage

import com.mdimitrov.nqueens.data.Databases
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val DATABASE_FILE = "puzzle.db"

/** One database for the app; `:core:data` opens the file it lives in. */
@Module
@InstallIn(SingletonComponent::class)
internal object StorageModule {
    @Provides
    @Singleton
    fun database(databases: Databases): PuzzleDatabase = databases.connect(PuzzleDatabase::class.java, DATABASE_FILE)
}
