package com.example.project1.ui.login

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
import com.example.project1.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

/**
 * Login screen.
 *
 * Takes lambdas rather than a NavController or an Intent, so it knows nothing
 * about navigation and can be previewed and unit-tested on its own.
 *
 * @param onLoginSuccess called with the identifier of the user who just logged in.
 * @param onNavigateToSignUp called when the user wants the sign-up screen instead.
 */
@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    onNavigateToSignUp: () -> Unit,
    modifier: Modifier = Modifier,
    database: GameDatabase?,
) {
    // Credentials stay in screen state and never travel through a navigation route.
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    var errorMessage by rememberSaveable { mutableStateOf("") }
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = "Login Screen",
                textAlign = TextAlign.Center,
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

            // TODO(team): there is no credential check yet — this accepts anything
            //   non-blank. Real auth goes in a LoginViewModel that calls
            //   UserDAO.getUserByUsername() and compares the password, exposing a
            //   LoginUiState (Idle / Submitting / Error) the same way HomeViewModel
            //   does. Only call onLoginSuccess once the check actually passes.
            Button(
                onClick = {
                    coroutineScope.launch {
                        val validUser = database?.userDao()?.validateLogin(email, password)
                        if (validUser != null) {
                            onLoginSuccess(validUser.email)
                        } else {
                            errorMessage = "Invalid email or password"
                        }
                    }
                },
                enabled = email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Log In")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onNavigateToSignUp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("No Account? Sign Up")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    // Passing empty lambdas is exactly why the screen takes callbacks instead of
    // a NavController, there is no navigation graph in a preview.
    MyApplicationTheme {
        LoginScreen(database = null, onLoginSuccess = {}, onNavigateToSignUp = {})
    }
}
