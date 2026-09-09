package com.example.project1.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project1.database.GameDatabase
import com.example.project1.database.entities.Card

/**
 * Home screen, shown once the user is signed in.
 *
 * @param username who is signed in, arrives as a navigation route argument.
 * @param onLogout called when the user signs out.
 */
@Composable
fun HomeScreen(
    database: GameDatabase?,
    username: String,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(database) {
        viewModel.load(database, username)
    }
    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Signed in as $username")
            Spacer(modifier = Modifier.height(16.dp))
            // Exhaustive over HomeUiState, no `else` branch needed, and adding a
            // fourth state will stop this compiling until it is handled here.
            when (val s = state) {
                is HomeUiState.Loading -> CircularProgressIndicator()

                is HomeUiState.Error -> Button(onClick = { viewModel.load(database, username) }) {
                    Text("Retry: ${s.message}")
                }
                is HomeUiState.Success -> {
                    Text("Your Deck (${s.cards.size} Cards)")
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(s.cards) {
                            card -> Card (modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text(card.name, style = MaterialTheme.typography.bodyLarge)
                                        Text("Cost: ${card.cost}", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Button(onClick = {
                                        viewModel.tradeCard(database, s.userId, 2, card.id, username)
                                    }) {
                                        Text("Trade Away")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = onLogout) {
                Text("Log Out")
            }
        }
    }
}
