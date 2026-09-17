package com.example.project1.ui.login

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [LoginScreen] takes plain lambdas and a nullable [com.example.project1.database.GameDatabase],
 * so it can be exercised directly with `database = null` -- no navigation graph or real
 * database needed.
 *
 * With `database = null`, `database?.userDao()` short-circuits to null inside the
 * button's click handler, so `onLoginSuccess` never fires. These tests deliberately do
 * NOT assert that path; they cover what does not depend on the database: the "Log In"
 * button's enabled state, and the sign-up navigation callback.
 */
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun logInButton_disabledWhenBothFieldsAreEmpty() {
        var loginCalled = false
        composeRule.setContent {
            LoginScreen(
                database = null,
                onLoginSuccess = { loginCalled = true },
                onNavigateToSignUp = {},
            )
        }

        composeRule.onNodeWithText("Log In").assertIsNotEnabled()
        assert(!loginCalled)
    }

    @Test
    fun logInButton_disabledWhenOnlyEmailIsFilled() {
        composeRule.setContent {
            LoginScreen(
                database = null,
                onLoginSuccess = {},
                onNavigateToSignUp = {},
            )
        }

        composeRule.onNodeWithText("Email").performTextInput("user@example.com")

        composeRule.onNodeWithText("Log In").assertIsNotEnabled()
    }

    @Test
    fun logInButton_enabledWhenBothFieldsAreFilled() {
        composeRule.setContent {
            LoginScreen(
                database = null,
                onLoginSuccess = {},
                onNavigateToSignUp = {},
            )
        }

        composeRule.onNodeWithText("Email").performTextInput("user@example.com")
        composeRule.onNodeWithText("Password").performTextInput("hunter2")

        composeRule.onNodeWithText("Log In").assertIsEnabled()
    }

    @Test
    fun signUpButton_invokesCallback() {
        var navigatedToSignUp = false
        composeRule.setContent {
            LoginScreen(
                database = null,
                onLoginSuccess = {},
                onNavigateToSignUp = { navigatedToSignUp = true },
            )
        }

        composeRule.onNodeWithText("No Account? Sign Up").performClick()

        assert(navigatedToSignUp)
    }
}
