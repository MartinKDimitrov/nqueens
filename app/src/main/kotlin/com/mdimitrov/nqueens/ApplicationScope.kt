package com.mdimitrov.nqueens

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * The scope for work that has to finish although the screen that started it is gone.
 *
 * A view model's own scope is cancelled when its destination is popped, which is right for
 * everything that only feeds a screen. Writing down a solved board is not that: the player has
 * finished and may leave at once, and the row is the only trace of what they did.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
internal object ApplicationScopeModule {
    /**
     * A supervisor, so that one failed write cannot take the scope down with it, and the default
     * dispatcher, because nothing here touches the screen.
     */
    @Provides
    @Singleton
    @ApplicationScope
    fun scope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
