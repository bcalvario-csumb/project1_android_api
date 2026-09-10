package com.example.project1

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class OpenPackFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun openCardPackButton_navigatesToPackScreen() {
        createNewUserAndReachHome()

        composeRule.onNodeWithText("Open Card Pack").performClick()

        composeRule.onNodeWithText("Open a Card Pack").assertExists()
        composeRule.onNodeWithText("Open Pack").assertExists()
    }

    @Test
    fun openingPack_addsOneApiCardToUsersDeck() {
        createNewUserAndReachHome()

        composeRule.onNodeWithText("Your Deck (0 Cards)").assertExists()

        composeRule.onNodeWithText("Open Card Pack").performClick()
        composeRule.onNodeWithText("Open Pack").performClick()

        waitForText("You opened", timeoutMillis = 20_000)

        composeRule.onNodeWithText(
            text = "You opened",
            substring = true,
        ).assertExists()

        composeRule.onNodeWithText("Back to Home").performClick()

        waitForText("Your Deck (1 Cards)")

        composeRule.onNodeWithText("Your Deck (1 Cards)").assertExists()
    }

    private fun createNewUserAndReachHome() {
        val uniqueEmail = "pack-test-${UUID.randomUUID()}@example.com"

        composeRule.onNodeWithText("No Account? Sign Up").performClick()

        waitForText("Sign Up Screen")

        val fields = composeRule.onAllNodes(hasSetTextAction())

        fields[0].performTextInput("Pack Test User")
        fields[1].performTextInput(uniqueEmail)
        fields[2].performTextInput("password123")
        fields[3].performTextInput("password123")

        composeRule.onNodeWithText("Create Account").performClick()

        waitForText("Open Card Pack")
    }

    private fun waitForText(
        text: String,
        timeoutMillis: Long = 5_000,
    ) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule
                .onAllNodesWithText(text, substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }
}