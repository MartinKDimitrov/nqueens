package com.mdimitrov.puzzles.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Duration.Companion.seconds

/** Long enough that a machine under load does not fail the run, short enough to fail it. */
private val WAIT = 5.seconds

/**
 * Against a real preference file rather than a stand-in for one, so what is tested is the round
 * trip a player's choice actually takes.
 */
@RunWith(RobolectricTestRunner::class)
class StoredThemesTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `a player who has never chosen has no answer to give`() =
        runTest {
            assertEquals(null, StoredThemes(storeIn("empty")).choice.first())
        }

    @Test
    fun `the choice is written under the key it is read from`() =
        runTest {
            // What is ours is the key and the name written under it — that a file survives a
            // process is DataStore's guarantee and its own tests'. A key renamed on one side and
            // not the other would leave every player back on the phone's answer, silently.
            val store = storeIn("named")
            StoredThemes(store).choose(ThemeChoice.DARK)

            assertEquals("DARK", store.data.first()[stringPreferencesKey("theme")])
        }

    @Test
    fun `choosing again replaces the answer rather than adding to it`() =
        runTest {
            val themes = StoredThemes(storeIn("changed"))
            themes.choose(ThemeChoice.DARK)

            themes.choose(ThemeChoice.LIGHT)

            assertEquals(ThemeChoice.LIGHT, themes.choice.first())
        }

    @Test
    fun `a name this build does not know reads as no answer at all`() =
        runTest {
            // What a file written by a later version looks like to an earlier one. Refusing to
            // draw would be the wrong answer; the phone's own is the right one.
            val store = storeIn("later")
            store.edit { it[stringPreferencesKey("theme")] = "SEPIA" }

            assertEquals(null, StoredThemes(store).choice.first())
        }

    @Test
    fun `a value stored under the key as something other than a name costs the answer, not the flow`() =
        // Not `runTest`: the store answers on a dispatcher of its own, and the waiting here has to
        // be for it rather than for a clock the test controls.
        runBlocking {
            // The same key written as a number — by a version of this app that kept something else
            // under it, or by a file damaged in a way that still parses. Asking a string key for it
            // throws, and the guard around the read would answer it by ending the flow: the palette
            // would be right and the button that changes it dead for the rest of the run.
            val store = storeIn("mistyped")
            store.edit { it[intPreferencesKey("theme")] = 1 }
            val themes = StoredThemes(store)
            val seen = Channel<ThemeChoice?>(Channel.UNLIMITED)
            val collecting = launch(Dispatchers.IO) { themes.choice.collect(seen::send) }

            assertEquals(null, withTimeout(WAIT) { seen.receive() })
            themes.choose(ThemeChoice.DARK)

            assertEquals(ThemeChoice.DARK, withTimeout(WAIT) { seen.receive() })
            collecting.cancel()
        }

    @Test
    fun `a file that cannot be read at all reads as no answer at all`() =
        runTest {
            // What is left after the corruption handler: a file the platform will not give up, a
            // permission the app no longer has. Refusing to draw the app over it would be the
            // wrong answer.
            assertEquals(null, StoredThemes(unreadable()).choice.first())
        }

    @Test
    fun `a file that is busy on the first read is asked again rather than given up on`() =
        runTest {
            // The realistic failure: a restore still settling, a permission arriving late. Giving
            // up on the first refusal ends the flow, and the palette button then writes a choice
            // nothing is left listening for.
            val store = busyOnce()

            assertEquals(ThemeChoice.DARK, StoredThemes(store).choice.first())
        }

    /** A store that refuses its first read and answers every one after it, as a busy file does. */
    private fun busyOnce(): DataStore<Preferences> {
        var refused = false
        val answer = mutablePreferencesOf().apply { this[stringPreferencesKey("theme")] = "DARK" }

        return object : DataStore<Preferences> {
            override val data: Flow<Preferences> =
                flow {
                    if (!refused) {
                        refused = true
                        throw IOException("the file is busy")
                    }
                    emit(answer)
                }

            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
                throw unopenable()
            }
        }
    }

    @Test
    fun `the answer for the first frame is asked for once, however busy the file is`() =
        runTest {
            // What this pins is a number of attempts, not a number of milliseconds. The caller is
            // holding the first frame, and the flow's own three-attempts-a-second-apart is three
            // seconds of frozen window on the thread that draws. No clock here can see that —
            // `runTest` skips every delay — so the reads are counted instead.
            val store = countingReads()

            assertEquals(null, StoredThemes(store).chosen())

            assertEquals(1, store.reads, "the first frame waited on more than one attempt")
        }

    @Test
    fun `a read that fails after a good one leaves the palette where it was`() =
        runTest {
            // Null is not a third palette — it is a player who has not chosen — so answering a
            // failed read with it hands the app back to the phone and undoes a choice that was
            // read successfully seconds earlier. The flow ends either way; what it must not do is
            // end on the wrong answer.
            val store = goodThenBroken()

            val seen = StoredThemes(store).choice.toList()

            assertFalse(seen.contains(null), "a failed read was reported as a player who had not chosen")
            assertEquals(ThemeChoice.DARK, seen.last(), "the app was left in a palette nobody asked for")
        }

    /** A file that answers once and then will not open, the way one being restored under the app does. */
    private fun goodThenBroken(): DataStore<Preferences> {
        val answer = mutablePreferencesOf().apply { this[stringPreferencesKey("theme")] = "DARK" }

        return object : DataStore<Preferences> {
            override val data: Flow<Preferences> =
                flow {
                    emit(answer)
                    throw unopenable()
                }

            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
                throw unopenable()
            }
        }
    }

    /** A store that will not open, and remembers how often it was asked. */
    private class CountingReads : DataStore<Preferences> {
        var reads = 0
            private set

        override val data: Flow<Preferences> =
            flow {
                reads++
                throw unopenable()
            }

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            throw unopenable()
        }
    }

    private fun countingReads() = CountingReads()

    /** A store whose file is there and will not open. */
    private fun unreadable() =
        object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw unopenable() }

            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
                throw unopenable()
            }
        }

    private fun storeIn(
        name: String,
        scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    ): DataStore<Preferences> {
        val file = File(folder.root, "$name.preferences_pb")

        return PreferenceDataStoreFactory.create(scope = scope) { file }
    }
}

private fun unopenable() = IOException("the preference file will not open")
