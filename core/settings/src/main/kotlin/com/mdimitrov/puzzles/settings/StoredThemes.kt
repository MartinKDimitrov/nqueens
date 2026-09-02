package com.mdimitrov.puzzles.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import java.io.IOException
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

private val CHOICE = stringPreferencesKey("theme")

/**
 * How many times a file that will not open is asked again, and how long between asks.
 *
 * A read fails while the file is busy far more often than because it is gone — a restore still
 * settling, a permission arriving late after a device unlock. Giving up on the first refusal ends
 * the flow, and ending the flow costs more than the value: the palette button writes its choice
 * successfully and nothing is left listening to redraw the app in it.
 */
private const val RETRIES = 3L
private val RetryDelay = 1.seconds

internal class StoredThemes
    @Inject
    constructor(
        @SettingsStore private val store: DataStore<Preferences>,
    ) : Themes {
        /**
         * The last answer the file gave, so that failing to read it again does not undo it.
         *
         * Null means two different things to a caller — "not chosen" and "not readable" — and only
         * the first is an answer. Without this, one refused read after a good one repaints the app
         * in the phone's palette instead of the player's, and the flow ends there, so nothing puts
         * it back until the next launch.
         */
        @Volatile
        private var lastKnown: ThemeChoice? = null

        override val choice: Flow<ThemeChoice?> =
            store.data
                .retryWhen { _, attempt ->
                    val again = attempt < RETRIES
                    if (again) delay(RetryDelay)
                    again
                }
                .map { stored -> stored.asChoice() }
                .onEach { lastKnown = it }
                // A file that will not open however often it is asked costs the app nothing it
                // already knew: the last answer stands, and the phone's own is a good enough one
                // to draw in when there has never been one. It does end the flow, so the button
                // stops repainting the app until the next launch — which is why what the file
                // *holds* is answered above rather than here, and why the asks above exist.
                .catch { emit(lastKnown) }

        // A file that will not open costs this caller the choice and nothing else. It does not ask
        // again: the asking above is for a collector that already has a screen up, and the one
        // caller here is holding the first frame.
        @Suppress("SwallowedException")
        override suspend fun chosen(): ThemeChoice? =
            try {
                store.data.first().asChoice()
            } catch (refused: IOException) {
                null
            }

        override suspend fun choose(choice: ThemeChoice) {
            store.edit { it[CHOICE] = choice.name }
        }
    }

/**
 * Anything but one of this build's own names reads as no answer at all: a name from a later
 * version, or a value written under this key as something other than a name. Both happen to a file
 * that outlives the build that wrote it, and leaving the phone to answer is better than refusing
 * to draw.
 *
 * Read out of the map rather than through the key, because the key asserts the type. Asking a
 * string key for a number throws, and throws where the answer is assembled — the one place a
 * fallback around the read would not be looking.
 */
private fun Preferences.asChoice(): ThemeChoice? {
    val stored = asMap()[CHOICE] as? String

    return ThemeChoice.entries.firstOrNull { it.name == stored }
}
