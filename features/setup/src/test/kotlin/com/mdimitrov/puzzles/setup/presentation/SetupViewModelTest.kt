package com.mdimitrov.puzzles.setup.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.mdimitrov.puzzles.nqueens.Queens
import com.mdimitrov.puzzles.puzzletype.Puzzles
import com.mdimitrov.puzzles.settings.ThemeChoice
import com.mdimitrov.puzzles.settings.Themes
import com.mdimitrov.puzzles.setup.domain.DEFAULT_BOARD_SIZE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SetupViewModelTest {
    // The view model watches the theme preference for as long as it lives, and a watch needs a
    // main dispatcher even when no test looks at what it sees.
    private val clock = StandardTestDispatcher()

    // What the app provides: a scope that outlives any one screen, on the test clock so a
    // preference still lands when the clock is advanced.
    private val writes = CoroutineScope(clock)

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun useTheTestClock() = Dispatchers.setMain(clock)

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun releaseTheClock() = Dispatchers.resetMain()

    @Test
    fun `the board starts at the default size`() {
        assertEquals(DEFAULT_BOARD_SIZE, setupOf().uiState.size)
    }

    @Test
    fun `growing and shrinking move the size by one`() {
        val viewModel = setupOf()
        val start = viewModel.uiState.size

        viewModel.grow()
        assertEquals(start + 1, viewModel.uiState.size)

        viewModel.shrink()
        assertEquals(start, viewModel.uiState.size)
    }

    @Test
    fun `the size cannot shrink below the smallest board on offer`() {
        val viewModel = setupOf()
        repeat(Queens.sizes.last - Queens.sizes.first + 1) { viewModel.shrink() }

        assertEquals(Queens.sizes.first, viewModel.uiState.size)
        assertFalse(viewModel.uiState.canShrink)
        assertTrue(viewModel.uiState.canGrow)
    }

    @Test
    fun `the size cannot grow past the largest board offered`() {
        val viewModel = setupOf()
        repeat(Queens.sizes.last - Queens.sizes.first + 1) { viewModel.grow() }

        assertEquals(Queens.sizes.last, viewModel.uiState.size)
        assertFalse(viewModel.uiState.canGrow)
        assertTrue(viewModel.uiState.canShrink)
    }

    @Test
    fun `choosing the puzzle already chosen leaves the board where it stands`() {
        // The only choice this build can offer: it ships one puzzle, so the row a player can press
        // is the row already selected. What a second puzzle would do to the size is the puzzle's
        // own range and is tested where that range is read.
        val viewModel = setupOf(Queens)
        repeat(2) { viewModel.grow() }

        viewModel.choose(Queens)

        assertEquals(DEFAULT_BOARD_SIZE + 2, viewModel.uiState.size, "the size moved under the player")
        assertEquals(Queens.key, viewModel.uiState.puzzle.key)
    }

    @Test
    fun `a puzzle that does not play the default size opens on one it does`() {
        assertEquals(5, setupOf(onSizes(4..5)).uiState.size)
    }

    @Test
    fun `choosing a puzzle that cannot play the size in hand moves it into range`() {
        val narrow = onSizes(4..6)
        val viewModel = setupOf(Queens, narrow)
        repeat(4) { viewModel.grow() }
        assertEquals(12, viewModel.uiState.size, "the stepper did not reach the wider puzzle's end")

        viewModel.choose(narrow)

        assertEquals(narrow.key, viewModel.uiState.puzzle.key)
        assertEquals(6, viewModel.uiState.size, "the size stayed outside the puzzle that was chosen")
    }

    @Test
    fun `choosing a puzzle keeps the palette the player asked for`() {
        val viewModel = setupOf(Queens, onSizes(4..6))
        viewModel.chooseTheme(ThemeChoice.DARK)
        clock.scheduler.runCurrent()

        viewModel.choose(onSizes(4..6))

        assertEquals(ThemeChoice.DARK, viewModel.uiState.theme, "the button forgot the chosen palette")
    }

    /**
     * The queens on a narrower range, under a key of their own.
     *
     * Not a second game — this build ships one and inventing another would test the fixture. It
     * stands for a second *module*, which is what `choose` exists for: what the shell does when
     * the puzzle in hand and the size in hand disagree.
     */
    private fun onSizes(sizes: IntRange) = Queens.copy(key = "queens-narrower", sizes = sizes)

    @Test
    fun `the palette the player chose last time is what the button says`() {
        val themes = RememberedThemes(ThemeChoice.DARK)

        val viewModel = SetupViewModel(Puzzles(setOf(Queens)), themes, writes)
        clock.scheduler.runCurrent()

        assertEquals(ThemeChoice.DARK, viewModel.uiState.theme)
    }

    @Test
    fun `a palette the player asks for is kept for the next run as well as this one`() {
        val themes = RememberedThemes()
        val viewModel = SetupViewModel(Puzzles(setOf(Queens)), themes, writes)
        clock.scheduler.runCurrent()

        viewModel.chooseTheme(ThemeChoice.DARK)
        clock.scheduler.runCurrent()

        assertEquals(ThemeChoice.DARK, viewModel.uiState.theme, "the screen did not follow the choice")
        assertEquals(ThemeChoice.DARK, themes.kept, "the choice was not handed on to be remembered")
    }

    @Test
    fun `a player who has never chosen leaves the palette to the phone`() {
        val viewModel = SetupViewModel(Puzzles(setOf(Queens)), RememberedThemes(), writes)
        clock.scheduler.runCurrent()

        assertEquals(null, viewModel.uiState.theme, "an unanswered question is not an answer")
    }

    @Test
    fun `a preference that refuses to be written never reaches the handler that ends the process`() {
        // A half-written file makes every write throw on the same damage. Before this was caught,
        // one press ended the app — and the next launch ended it again, because nothing repaired
        // the file. Asserting on the state alone cannot see it: an escaped exception leaves the
        // state exactly as a swallowed one does, and only one of them kills the process.
        val viewModel = SetupViewModel(Puzzles(setOf(Queens)), RefusingThemes(), writes)
        clock.scheduler.runCurrent()

        val escaped = mutableListOf<Throwable>()
        val before = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, failure -> escaped += failure }

        try {
            viewModel.chooseTheme(ThemeChoice.DARK)
            clock.scheduler.runCurrent()
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(before)
        }

        assertTrue(escaped.isEmpty(), "a refused preference write reached the uncaught handler: $escaped")
    }

    @Test
    fun `a palette chosen as the player leaves is still remembered`() {
        // The press and the back gesture can be one movement. On this screen's own scope the write
        // is queued behind the gesture and cancelled by it, and the app opens next time in a
        // palette the player did choose and it never wrote down.
        val themes = RememberedThemes()
        val viewModel = SetupViewModel(Puzzles(setOf(Queens)), themes, writes)
        clock.scheduler.runCurrent()

        viewModel.chooseTheme(ThemeChoice.DARK)
        pop(viewModel)
        clock.scheduler.runCurrent()

        assertEquals(ThemeChoice.DARK, themes.kept, "the choice went down with the screen that made it")
    }

    private fun setupOf(vararg installed: com.mdimitrov.puzzles.puzzletype.Puzzle = arrayOf(Queens)) =
        SetupViewModel(Puzzles(installed.toSet()), RememberedThemes(), writes)
}

/**
 * What the framework does to a view model when its destination is popped: the store it was kept in
 * is cleared, and clearing cancels the scope the view model composes on.
 *
 * There is no shorter way to reach it — cancelling that scope is not something a view model
 * exposes, and a test that reached around it would be testing its own reach.
 */
private fun pop(viewModel: ViewModel) {
    val store = ViewModelStore()
    ViewModelProvider(
        store,
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = viewModel as T
        },
    )[viewModel::class.java]
    store.clear()
}

/** A preference that keeps what it is given, the way the file behind it does. */
private class RememberedThemes(
    initial: ThemeChoice? = null,
) : Themes {
    private val chosen = MutableStateFlow(initial)

    val kept: ThemeChoice? get() = chosen.value

    override val choice: Flow<ThemeChoice?> get() = chosen

    override suspend fun chosen(): ThemeChoice? = chosen.value

    override suspend fun choose(choice: ThemeChoice) {
        chosen.value = choice
    }
}

/** A preference file that cannot be written, the way a half-written one cannot. */
private class RefusingThemes : Themes {
    override val choice: Flow<ThemeChoice?> get() = MutableStateFlow(null)

    override suspend fun chosen(): ThemeChoice? = null

    override suspend fun choose(choice: ThemeChoice): Unit = error("the file is damaged")
}
