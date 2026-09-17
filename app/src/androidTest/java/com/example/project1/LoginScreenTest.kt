package com.example.project1

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.project1.ui.login.LoginScreen
import com.example.project1.ui.theme.MyApplicationTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginButton_isDisabled_whenFieldsAreEmpty() {
        composeTestRule.setContent {
            MyApplicationTheme {
                LoginScreen(database = null, onLoginSuccess = {},onNavigateToSignUp = {})
            }
        }
        composeTestRule.onNodeWithText("Log In").assertIsDisplayed()
        composeTestRule.onNodeWithText("Log In").assertIsNotEnabled()
    }

    @Test
    fun loginButton_isEnabled_whenFieldsAreFilled() {
        composeTestRule.setContent {
            MyApplicationTheme {
                LoginScreen(database = null, onLoginSuccess = {}, onNavigateToSignUp = {})
            }
        }
        composeTestRule.onNodeWithText("Email").performTextInput("kyleparker@gmail.com")
        composeTestRule.onNodeWithText("Password").performTextInput("screwyoukyle")
        composeTestRule.onNodeWithText("Log In").assertIsEnabled()
    }

    @Test
    fun signUpButton_triggersNavigationCallback() {
        var navigatedToSignUp = false
        composeTestRule.setContent {
            MyApplicationTheme {
                LoginScreen(database = null, onLoginSuccess = {},onNavigateToSignUp = { navigatedToSignUp = true })
            }
        }
        composeTestRule.onNodeWithText("No Account? Sign Up").performClick()
        assertTrue(navigatedToSignUp)
    }
}