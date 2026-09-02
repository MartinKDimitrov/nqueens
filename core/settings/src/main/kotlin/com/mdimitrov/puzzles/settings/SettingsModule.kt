package com.mdimitrov.puzzles.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/** The file the player's preferences live in. */
private const val PREFERENCES_FILE = "settings"

/**
 * The preference file, named so that only this module can ask for it. Unqualified, a
 * `DataStore<Preferences>` is a type any module could inject and write the same keys through,
 * which would leave [Themes] describing a contract it does not hold. Internal, so the name itself
 * does not leave the module.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class SettingsStore

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ThemesModule {
    @Binds
    @Singleton
    abstract fun themes(themes: StoredThemes): Themes
}

@Module
@InstallIn(SingletonComponent::class)
internal object PreferencesModule {
    /**
     * One store per graph, which in this app is one per process: DataStore refuses a second store
     * on a file another already has open, so anything that builds the graph twice without
     * restarting the process — an instrumented run, most of all — has to share one graph rather
     * than replace this binding.
     */
    @Provides
    @Singleton
    @SettingsStore
    fun store(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            // A half-written file — a power cut during a write, a restored backup — is otherwise
            // unreadable for ever: every read falls back and every write throws on the same
            // damage. Replacing it costs the preferences and makes the next write land.
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        ) { context.preferencesDataStoreFile(PREFERENCES_FILE) }
}
