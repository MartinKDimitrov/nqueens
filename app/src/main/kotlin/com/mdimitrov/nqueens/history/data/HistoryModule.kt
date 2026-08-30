package com.mdimitrov.nqueens.history.data

import com.mdimitrov.nqueens.data.Databases
import com.mdimitrov.nqueens.history.domain.SolveRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val DATABASE_FILE = "puzzle.db"

/** The feature brings the tables and the queries; `:core:data` opens the file they live in. */
@Module
@InstallIn(SingletonComponent::class)
internal object PuzzleDatabaseModule {
    @Provides
    @Singleton
    fun database(databases: Databases): PuzzleDatabase = databases.connect(PuzzleDatabase::class.java, DATABASE_FILE)

    @Provides
    fun dao(database: PuzzleDatabase): SolveDao = database.solves()
}

/** What leaves the feature's `data` is the interface its `domain` asked for. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class SolveRepositoryModule {
    @Binds
    @Singleton
    abstract fun repository(repository: SolveRepositoryImpl): SolveRepository
}
