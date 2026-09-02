package com.mdimitrov.puzzles

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.mdimitrov.puzzles.nqueens.Queens
import com.mdimitrov.puzzles.play.presentation.playRoute
import com.mdimitrov.puzzles.puzzletype.Puzzles
import com.mdimitrov.puzzles.theme.PuzzleTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-420dpi")
class PuzzleNavHostTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun `a board larger than the app plays sends the player back to Setup`() {
        val controller = host()

        compose.runOnIdle { controller.navigate(playRoute(Queens.key, Queens.sizes.last + 1)) }

        compose.onNodeWithText("Start").assertIsDisplayed()
    }

    @Test
    fun `a board too small to have a solution sends the player back to Setup`() {
        val controller = host()

        compose.runOnIdle { controller.navigate(playRoute(Queens.key, Queens.sizes.first - 1)) }

        compose.onNodeWithText("Start").assertIsDisplayed()
    }

    @Test
    fun `a puzzle this build was not assembled with sends the player back to Setup`() {
        val controller = host()

        compose.runOnIdle { controller.navigate(playRoute("another-puzzle", Queens.sizes.first)) }

        compose.onNodeWithText("Start").assertIsDisplayed()
    }

    @Test
    fun `bouncing an unplayable size leaves nothing behind to go back to`() {
        val controller = host()

        compose.runOnIdle { controller.navigate(playRoute(Queens.key, Queens.sizes.last + 1)) }

        compose.onNodeWithText("Start").assertIsDisplayed()
        compose.runOnIdle { assertNull(controller.previousBackStackEntry) }
    }

    private fun host(): NavHostController {
        lateinit var controller: NavHostController
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                controller = rememberNavController()
                PuzzleTheme {
                    PuzzleNavHost(puzzles = Puzzles(setOf(Queens)), navController = controller)
                }
            }
        }
        compose.waitForIdle()
        return controller
    }
}
