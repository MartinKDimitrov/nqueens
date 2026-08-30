package com.mdimitrov.nqueens

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.mdimitrov.nqueens.domain.MIN_BOARD_SIZE
import com.mdimitrov.nqueens.puzzle.LARGEST_PLAYABLE_BOARD
import com.mdimitrov.nqueens.theme.NQueensTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-420dpi")
class NQueensNavHostTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun `a board larger than the app plays sends the player back to Setup`() {
        val controller = host()

        compose.runOnIdle { controller.navigate("game/${LARGEST_PLAYABLE_BOARD + 1}") }

        compose.onNodeWithText("Start").assertIsDisplayed()
    }

    @Test
    fun `a board too small to have a solution sends the player back to Setup`() {
        val controller = host()

        compose.runOnIdle { controller.navigate("game/${MIN_BOARD_SIZE - 1}") }

        compose.onNodeWithText("Start").assertIsDisplayed()
    }

    @Test
    fun `bouncing an unplayable size leaves nothing behind to go back to`() {
        val controller = host()

        compose.runOnIdle { controller.navigate("game/${LARGEST_PLAYABLE_BOARD + 1}") }

        compose.onNodeWithText("Start").assertIsDisplayed()
        compose.runOnIdle { assertNull(controller.previousBackStackEntry) }
    }

    private fun host(): NavHostController {
        lateinit var controller: NavHostController
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                controller = rememberNavController()
                NQueensTheme {
                    NQueensNavHost(navController = controller)
                }
            }
        }
        compose.waitForIdle()
        return controller
    }
}
