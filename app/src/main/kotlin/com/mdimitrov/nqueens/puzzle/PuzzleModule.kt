package com.mdimitrov.nqueens.puzzle

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object PuzzleModule {
    @Provides
    @Singleton
    fun variant(): Variant = Queens
}
