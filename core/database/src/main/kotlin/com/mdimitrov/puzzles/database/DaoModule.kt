package com.mdimitrov.puzzles.database

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * The accessors, one per feature that stores something. A feature is injected the DAO for its own
 * table and nothing else: it never names the database and never opens a file.
 *
 * What is injected is one table's accessor; what is *reachable* is every accessor in this module,
 * because Kotlin has no package-private and these are one module. With one table the two are the
 * same thing. With a second, only review keeps them so — see TRADEOFFS D18.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object DaoModule {
    @Provides
    fun solves(database: PuzzleDatabase): SolveDao = database.solves()
}
