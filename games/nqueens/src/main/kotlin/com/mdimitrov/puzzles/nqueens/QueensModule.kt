package com.mdimitrov.puzzles.nqueens

import com.mdimitrov.puzzles.puzzletype.Puzzle
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal object QueensModule {
    /**
     * One line is all a game module contributes. It goes into a set rather than being bound on
     * its own, so a second game is another module doing the same thing rather than a clash.
     */
    @Provides
    @IntoSet
    fun puzzle(): Puzzle = Queens
}
