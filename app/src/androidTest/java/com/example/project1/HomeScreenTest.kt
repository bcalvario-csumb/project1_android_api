package com.example.project1

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.project1.database.entities.Card
import com.example.project1.database.entities.User
import com.example.project1.database.entities.UserHasCard
import com.example.project1.ui.home.HomeScreen
import com.example.project1.ui.theme.MyApplicationTheme
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest : DatabaseTest() {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val username = "home-test@example.com"
    private val firstDescription =
        "This is a deliberately long card description. " +
            "It gives the expanded card enough content to require vertical scrolling. " +
            "The complete text should remain available inside the modal card."

    @Before
    fun addHomeScreenData() = runBlocking {
        database.userDao().insertUser(
            User(
                id = 1,
                email = username,
                password = "password",
                name = "Home Test User",
            ),
        )
        database.userDao().insertUser(
            User(
                id = 2,
                email = "trade-target@example.com",
                password = "password",
                name = "Trade Target",
            ),
        )

        val cards = listOf(
            Card(
                id = 1,
                name = "First Test Card",
                description = firstDescription,
                cost = 20,
                img = "",
            ),
            Card(
                id = 2,
                name = "Second Test Card",
                description = "A second card description.",
                cost = 20,
                img = "",
            ),
            Card(
                id = 3,
                name = "Third Test Card",
                description = "A third card description.",
                cost = 20,
                img = "",
            ),
        )

        cards.forEach { card ->
            database.cardDao().insertCard(card)
            database.userHasCardDao().insertUserCard(
                UserHasCard(userId = 1, cardId = card.id),
            )
        }
    }

    @Test
    fun homeScreen_displaysCardsInCollection() {
        showHomeScreen()

        composeTestRule.onNodeWithText("Your Deck (3 Cards)").assertIsDisplayed()
        composeTestRule.onNodeWithText("First Test Card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Second Test Card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Third Test Card").assertIsDisplayed()
    }

    @Test
    fun clickingCard_opensExpandedCardWithBackAndTradeButtons() {
        showHomeScreen()

        composeTestRule.onNodeWithText("First Test Card").performClick()

        composeTestRule.onNodeWithText("Back").assertIsDisplayed()
        composeTestRule.onNodeWithText("Trade").assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(firstDescription)
            .assertCountEquals(2)
    }

    @Test
    fun expandedCard_descriptionCanScrollVertically() {
        showHomeScreen()

        composeTestRule.onNodeWithText("First Test Card").performClick()

        val description = composeTestRule.onNodeWithTag("expanded_card_description")
        description.assertIsDisplayed()
        description.performTouchInput { swipeUp() }
    }

    @Test
    fun backButton_closesExpandedCard() {
        showHomeScreen()

        composeTestRule.onNodeWithText("First Test Card").performClick()
        composeTestRule.onNodeWithText("Back").performClick()

        composeTestRule.onNodeWithText("Back").assertDoesNotExist()
        composeTestRule.onNodeWithText("Your Deck (3 Cards)").assertIsDisplayed()
    }

    private fun showHomeScreen() {
        composeTestRule.setContent {
            MyApplicationTheme {
                HomeScreen(
                    database = database,
                    username = username,
                    onOpenPack = {},
                    onOpenAdmin = {},
                    onLogout = {},
                )
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule
                .onAllNodesWithText("Your Deck (3 Cards)")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }
}
