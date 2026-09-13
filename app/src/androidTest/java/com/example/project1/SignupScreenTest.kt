package com.example.project1

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.project1.ui.signup.SignUpScreen
import com.example.project1.ui.theme.MyApplicationTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignupScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun createAccountButton_isDisabled_whenFieldsAreEmpty() {
        composeTestRule.setContent {
            MyApplicationTheme {
                SignUpScreen(database = null, onSignUpSuccess = {}, onNavigateBack = {})
            }
        }
        composeTestRule.onNodeWithText("Create Account").assertIsDisplayed()
        composeTestRule.onNodeWithText("Create Account").assertIsNotEnabled()
    }

    @Test
    fun createAccountButton_isDisabled_whenPasswordsDoNotMatch() {
        composeTestRule.setContent {
            MyApplicationTheme {
                SignUpScreen(database = null, onSignUpSuccess = {}, onNavigateBack = {})
            }
            composeTestRule.onNodeWithText("Name").performTextInput("LearnerGamer")
            composeTestRule.onNodeWithText("Email").performTextInput("alexSpartan06@gmail.com")
            composeTestRule.onNodeWithText("Password").performTextInput("Peepee17!")
            composeTestRule.onNodeWithText("Confirm Password").performTextInput("Peepee17!")
            composeTestRule.onNodeWithText("Create Account").assertIsNotEnabled()
            composeTestRule.onNodeWithText("Passwords do not match").assertIsDisplayed()
        }
    }

    @Test
    fun createAccountButton_isEnabled_whenFieldsAreFilledAndPasswordsMatch() {
        composeTestRule.setContent {
            MyApplicationTheme {
                SignUpScreen(database = null, onSignUpSuccess = {}, onNavigateBack = {})
            }
        }
        composeTestRule.onNodeWithText("Name").performTextInput("LearnerGamer")
        composeTestRule.onNodeWithText("Email").performTextInput("alexSpartan06@gmail.com")
        composeTestRule.onNodeWithText("Password").performTextInput("Peepee17!")
        composeTestRule.onNodeWithText("Confirm Password").performTextInput("Peepee17!")
        composeTestRule.onNodeWithText("Create Account").assertIsEnabled()
    }

    @Test
    fun loginButton_triggersNavigateBackCallback() {
        var navigatedBack = false
        composeTestRule.setContent {
            MyApplicationTheme {
                SignUpScreen(database = null, onSignUpSuccess = {}, onNavigateBack = { navigatedBack = true })
            }
        }
        composeTestRule.onNodeWithText("Already have an account? Log In").performClick()
        assertTrue(navigatedBack)
    }
}