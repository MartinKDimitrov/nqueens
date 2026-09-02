package com.mdimitrov.puzzles.play.presentation.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.mdimitrov.puzzles.play.R

private const val MAX_STREAMS = 4
private const val VOLUME = 0.7f
private const val PRIORITY = 1
private const val NO_LOOP = 0
private const val NORMAL_RATE = 1f

/** The four noises the game can make, each one synthesised rather than recorded. */
internal enum class PlaySound(
    @RawRes val resource: Int,
) {
    PLACE(R.raw.play_place),
    REMOVE(R.raw.play_remove),
    CONFLICT(R.raw.play_conflict),
    WIN(R.raw.play_win),
}

/**
 * Somewhere for a sound to go. The board asks for one by name and never learns whether anything
 * came out, which is what lets a test hand it a listener in place of a speaker.
 */
internal fun interface Sounds {
    fun play(sound: PlaySound)
}

/** A board given nowhere to send its sounds makes none, rather than raising. */
private val Silence = Sounds { }

internal val LocalSounds = staticCompositionLocalOf { Silence }

/** A pool sized for a handful of short overlapping sounds, told that it is playing a game. */
internal fun soundPool(): SoundPool =
    SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

/**
 * The sounds as the device plays them: loaded once, and quiet while the phone is. A game noise
 * travels on the media stream, which the ringer switch does not silence, so the switch is
 * honoured here instead of being ignored.
 */
internal class DeviceSounds(
    context: Context,
    private val pool: SoundPool = soundPool(),
) : Sounds {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val loaded = PlaySound.entries.associateWith { pool.load(context, it.resource, PRIORITY) }

    override fun play(sound: PlaySound) {
        if (audio.ringerMode != AudioManager.RINGER_MODE_NORMAL) return

        loaded[sound]?.let { pool.play(it, VOLUME, VOLUME, PRIORITY, NO_LOOP, NORMAL_RATE) }
    }

    fun release() {
        pool.release()
    }
}

/** The device's sounds, held for as long as the screen that asked for them. */
@Composable
internal fun rememberSounds(): Sounds {
    val context = LocalContext.current
    val sounds = remember(context) { DeviceSounds(context) }

    DisposableEffect(sounds) {
        onDispose { sounds.release() }
    }

    return sounds
}
