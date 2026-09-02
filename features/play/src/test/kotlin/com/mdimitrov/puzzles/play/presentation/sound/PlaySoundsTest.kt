package com.mdimitrov.puzzles.play.presentation.sound

import android.content.Context
import android.media.AudioManager
import com.mdimitrov.puzzles.play.R
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class PlaySoundsTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    @Test
    fun `every sound names a file of its own`() {
        val files = PlaySound.entries.map { it.resource }

        assertEquals(PlaySound.entries.size, files.toSet().size, "two sounds share a file: $files")
    }

    @Test
    fun `a sound reaches the device, and a phone on silent stays silent`() {
        val pool = soundPool()
        val sounds = DeviceSounds(context, pool)

        audio.ringerMode = AudioManager.RINGER_MODE_NORMAL
        sounds.play(PlaySound.PLACE)
        assertTrue(shadowOf(pool).wasResourcePlayed(R.raw.play_place), "the place sound never reached the pool")

        audio.ringerMode = AudioManager.RINGER_MODE_SILENT
        sounds.play(PlaySound.WIN)
        assertFalse(shadowOf(pool).wasResourcePlayed(R.raw.play_win), "a phone on silent played the win")
    }
}
