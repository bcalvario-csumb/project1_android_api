package com.example.project1.ui.signup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.project1.database.GameDatabase
import com.example.project1.database.entities.User
import com.example.project1.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import android.util.Patterns
import android.database.sqlite.SQLiteConstraintException
import androidx.compose.material3.MaterialTheme

/**
 * Sign-up screen.
 *
 * @param onSignUpSuccess called with the identifier of the account just created.
 * @param onNavigateBack called when the user backs out to the login screen.
 */
@Composable
fun SignUpScreen(
    onSignUpSuccess: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    database: GameDatabase?
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf("") }
    val passwordsMatch = password == confirmPassword
    val canSubmit =
        name.isNotBlank() &&
                email.isNotBlank() &&
                password.isNotBlank() &&
                confirmPassword.isNotBlank() &&
                passwordsMatch
    val coroutineScope = rememberCoroutineScope()
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = "Sign Up Screen",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                singleLine = true,
                isError = confirmPassword.isNotBlank() && !passwordsMatch,
                supportingText = {
                    if (confirmPassword.isNotBlank() && !passwordsMatch) {
                        Text("Passwords do not match")
                    }
                },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (errorMessage.isNotBlank()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // TODO(team): no account is actually created yet. Real sign-up goes in a
            //   SignUpViewModel that calls UserDAO.insertUser(). Never store a raw
            //   password, hash it before it reaches the database.
            Button(
                onClick = {
                    errorMessage = ""

                    val normalizedName = name.trim()
                    val normalizedEmail = email.trim()

                    when {
                        normalizedName.isBlank() -> {
                            errorMessage = "Please enter your name."
                            return@Button
                        }

                        normalizedEmail.isBlank() -> {
                            errorMessage = "Please enter your email."
                            return@Button
                        }

                        !Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches() -> {
                            errorMessage = "Please enter a valid email address."
                            return@Button
                        }

                        password.isBlank() -> {
                            errorMessage = "Please enter a password."
                            return@Button
                        }

                        password.length < 8 -> {
                            errorMessage = "Password must be at least 8 characters."
                            return@Button
                        }

                        password != confirmPassword -> {
                            errorMessage = "Passwords do not match."
                            return@Button
                        }
                    }

                    coroutineScope.launch {
                        try {
                            val db = database

                            if (db == null) {
                                Log.e("SignUpScreen", "Database was null")
                                errorMessage = "Unable to access the database."
                                return@launch
                            }

                            val existingUser = db.userDao().getUserByEmail(normalizedEmail)

                            if (existingUser != null) {
                                errorMessage = "An account with this email already exists."
                                return@launch
                            }

                            val newUser = User(
                                name = normalizedName,
                                email = normalizedEmail,
                                password = password
                            )

                            db.userDao().insertUser(newUser)

                            onSignUpSuccess(normalizedEmail)
                        } catch (exception: SQLiteConstraintException) {
                            Log.e("SignUpScreen", "Duplicate user insertion attempted", exception)
                            errorMessage = "An account with this email already exists."
                        } catch (exception: Exception) {
                            Log.e("SignUpScreen", "Signup failed", exception)
                            errorMessage = "Unable to create your account. Please try again."
                        }
                    }
                },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Account")
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Already have an account? Log In")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SignUpScreenPreview() {
    MyApplicationTheme {
        SignUpScreen(database = null, onSignUpSuccess = {}, onNavigateBack = {})
    }
}
