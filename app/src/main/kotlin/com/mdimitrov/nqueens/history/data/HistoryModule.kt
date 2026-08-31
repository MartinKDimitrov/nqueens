package com.mdimitrov.nqueens.history.data

import com.mdimitrov.nqueens.history.domain.Clock
import com.mdimitrov.nqueens.history.domain.SolveRepository
import com.mdimitrov.nqueens.storage.PuzzleDatabase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** The feature's own table, read through the app's one database. */
@Module
@InstallIn(SingletonComponent::class)
internal object SolveDaoModule {
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

/** The clock the feature stamps its records with. */
@Module
@InstallIn(SingletonComponent::class)
internal object ClockModule {
    @Provides
    fun clock(): Clock = Clock { System.currentTimeMillis() }
}
