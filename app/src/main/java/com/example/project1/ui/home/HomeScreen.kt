package com.example.project1.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Home screen, shown once the user is signed in.
 *
 * @param username who is signed in, arrives as a navigation route argument.
 * @param onLogout called when the user signs out.
 */
@Composable
fun HomeScreen(
    username: String,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Signed in as $username")
            Spacer(modifier = Modifier.height(24.dp))
            // Exhaustive over HomeUiState, no `else` branch needed, and adding a
            // fourth state will stop this compiling until it is handled here.
            when (val s = state) {
                is HomeUiState.Loading -> CircularProgressIndicator()

                is HomeUiState.Error -> Button(onClick = { viewModel.load() }) {
                    Text("Retry: ${s.message}")
                }
                is HomeUiState.Success -> Text("Loaded ${s.products.length} chars of products")
            }
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = onLogout) {
                Text("Log Out")
            }
        }
    }
}
