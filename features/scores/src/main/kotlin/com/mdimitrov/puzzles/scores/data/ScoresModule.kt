package com.mdimitrov.puzzles.scores.data

import com.mdimitrov.puzzles.scores.domain.Clock
import com.mdimitrov.puzzles.scores.domain.SolveRepository
import com.mdimitrov.puzzles.solves.RecordSolve
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** What leaves the feature's `data` is the interface its `domain` asked for. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class SolveRepositoryModule {
    @Binds
    @Singleton
    abstract fun repository(repository: SolveRepositoryImpl): SolveRepository
}

/** What every game meets the feature through: one verb, and no way to reach the rest. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class RecordSolveModule {
    @Binds
    abstract fun recorder(recorder: SolveRecorder): RecordSolve
}

/** The clock the feature stamps its records with. */
@Module
@InstallIn(SingletonComponent::class)
internal object ClockModule {
    @Provides
    fun clock(): Clock = Clock { System.currentTimeMillis() }
}
