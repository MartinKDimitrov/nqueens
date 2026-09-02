package com.mdimitrov.puzzles.settings

import kotlinx.coroutines.flow.Flow

/** Which of the two palettes the app draws in. */
public enum class ThemeChoice { LIGHT, DARK }

/**
 * The player's answer to that question, kept between runs.
 *
 * Null is not a third palette: it is a player who has not answered, and the phone's own setting
 * answers for them until they do. Only a screen can resolve it, because only a screen can ask the
 * phone — which is why what is stored and what is drawn are different types.
 *
 * [choice] is a flow rather than a value because the answer changes while the app is running: the
 * button that sets it is on one screen and every screen is drawn from it.
 */
public interface Themes {
    public val choice: Flow<ThemeChoice?>

    /**
     * The answer as it stands, for the one caller that has to draw a frame with it and cannot wait
     * for a better one.
     *
     * One read, and no second attempt: [choice] is allowed to ask a busy file again because
     * whoever is collecting it has a screen up already, and this caller has nothing on screen at
     * all. Asking again here spends the wait where the player is looking at a blank window.
     */
    public suspend fun chosen(): ThemeChoice?

    public suspend fun choose(choice: ThemeChoice)
}
