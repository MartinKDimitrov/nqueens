package com.mdimitrov.puzzles

import android.media.AudioAttributes
import android.media.SoundPool
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val LOADED = 0
private const val PATIENCE_SECONDS = 10L
private const val MAX_STREAMS = 4
private const val VOLUME = 1f
private const val PRIORITY = 1
private const val NO_LOOP = 0
private const val NORMAL_RATE = 1f

/**
 * That the four sounds are files a real `SoundPool` will take.
 *
 * The JVM tests assert which sound is asked for; they cannot assert that the file behind it
 * decodes, because Robolectric's `SoundPool` accepts anything. A wav the platform's decoder
 * refuses loads with a status the app never reads and plays as silence — which is what a player
 * would hear, and what no test on a workstation can catch.
 */
@RunWith(AndroidJUnit4::class)
class SoundsOnDeviceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun everySoundLoadsAndPlays() {
        val sounds =
            listOf(
                com.mdimitrov.puzzles.play.R.raw.play_place,
                com.mdimitrov.puzzles.play.R.raw.play_remove,
                com.mdimitrov.puzzles.play.R.raw.play_conflict,
                com.mdimitrov.puzzles.play.R.raw.play_win,
            )
        val loaded = CountDownLatch(sounds.size)
        val refused = mutableListOf<Int>()
        val pool =
            SoundPool.Builder()
                .setMaxStreams(MAX_STREAMS)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .build()

        pool.setOnLoadCompleteListener { _, id, status ->
            if (status != LOADED) refused += id
            loaded.countDown()
        }
        val ids = sounds.map { pool.load(context, it, PRIORITY) }

        assertTrue(loaded.await(PATIENCE_SECONDS, TimeUnit.SECONDS), "a sound never finished loading")
        assertEquals(emptyList(), refused, "the platform's decoder refused a sound")

        val silent = ids.filter { pool.play(it, VOLUME, VOLUME, PRIORITY, NO_LOOP, NORMAL_RATE) == 0 }
        assertEquals(emptyList(), silent, "a loaded sound produced no stream")

        pool.release()
    }
}
