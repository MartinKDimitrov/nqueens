package com.mdimitrov.nqueens.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DatabaseModule {
    @Binds
    @Singleton
    abstract fun databases(databases: RoomDatabases): Databases
}
