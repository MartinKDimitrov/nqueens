package com.mdimitrov.nqueens.format

import java.util.Locale

private const val SECONDS_PER_MINUTE = 60

/** Elapsed time as the game and the records both show it: minutes and seconds, padded. */
internal fun formatElapsed(seconds: Int): String {
    val counted = seconds.coerceAtLeast(0)
    return String.format(
        Locale.getDefault(),
        "%02d:%02d",
        counted / SECONDS_PER_MINUTE,
        counted % SECONDS_PER_MINUTE,
    )
}
