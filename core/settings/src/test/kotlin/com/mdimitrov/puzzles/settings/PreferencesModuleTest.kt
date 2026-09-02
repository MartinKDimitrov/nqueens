package com.mdimitrov.puzzles.settings

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The store the app actually opens, against the file it actually opens it on.
 *
 * [StoredThemesTest] builds its own stores, which is right for what it asks — but a store built in
 * a test is not the one production hands out, and the difference between them is the corruption
 * handler. Without it a half-written file is unreadable for ever: every read falls back and every
 * write throws on the same damage, so the app opens on the wrong palette and can never be told
 * otherwise. That is the one property no test of a hand-built store can hold.
 */
@RunWith(RobolectricTestRunner::class)
class PreferencesModuleTest {
    private val choice = stringPreferencesKey("theme")

    @Test
    fun `a file too damaged to parse is replaced rather than refused for ever`() =
        runBlocking {
            val context = RuntimeEnvironment.getApplication()
            // What a power cut during a write, or a half-restored backup, leaves behind: bytes
            // where a protocol buffer should be.
            context.preferencesDataStoreFile("settings").apply {
                parentFile?.mkdirs()
                writeBytes(byteArrayOf(0x00, 0x01, 0x02, 0x03))
            }

            val store = PreferencesModule.store(context)

            val read = store.data.first()
            assertEquals(emptySet(), read.asMap().keys, "the damage was read as though it were preferences")

            store.edit { it[choice] = "DARK" }

            assertEquals("DARK", store.data.first()[choice], "the next write did not land either")
        }
}
