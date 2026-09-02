package com.mdimitrov.puzzles.scope

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * The scope for work that has to finish although the screen that started it is gone.
 *
 * A view model's own scope is cancelled when its destination is popped, which is right for
 * everything that only feeds a screen. Four writes are not that, and they are the four the app
 * would otherwise lose without saying so: writing down a solved board, deleting one record,
 * clearing them all, and remembering which palette the player asked for. In each the player has
 * finished and may leave at once, and what they did survives only if the write does.
 *
 * It is a module of its own because the three screens that make those writes are features, and a
 * feature cannot reach another feature.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
public annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
internal object ApplicationScopeModule {
    /**
     * A supervisor, so that one failed write cannot take the scope down with it, and the default
     * dispatcher, because nothing here touches the screen.
     *
     * One thread at a time out of that dispatcher, so that work of the same shape is begun in the
     * order it was handed over. Given the whole pool, two presses of the palette button in one
     * second are two coroutines racing on two threads, and the palette the app keeps is whichever
     * won rather than the one pressed last. Nothing here is slow enough for the pool to be worth
     * the race: every one of these writes is a file or a row, and waits on the disk rather than
     * the CPU.
     *
     * It is not a queue, and it does not order work of *different* shapes. A coroutine that
     * suspends gives the slot up and rejoins behind whatever is already waiting, so one that
     * suspends twice can finish after one handed over later that suspends once. The one place two
     * shapes meet here is a solved board being written down — a read and then a write — while the
     * records screen is cleared: the clearing can land between the two and leave the row in a
     * table the player has just emptied. It is a window of milliseconds, it needs the win and the
     * clearing in one movement, and closing it properly means the two writes knowing about each
     * other, which is a coupling worth more than the case costs.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Provides
    @Singleton
    @ApplicationScope
    fun scope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))
}
