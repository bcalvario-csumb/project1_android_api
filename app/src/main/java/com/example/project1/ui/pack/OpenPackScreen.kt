package com.example.project1.ui.pack

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.example.project1.database.GameDatabase

@Composable
fun OpenPackScreen(
    database: GameDatabase,
    username: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OpenPackViewModel = viewModel(),
) {
    val uiState by
    viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Text(
                text = "Open a Card Pack",
                style = MaterialTheme.typography.headlineMedium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("Signed in as $username")

            Spacer(modifier = Modifier.height(24.dp))

            when (val state = uiState) {
                PackUiState.Ready -> {
                    Button(
                        onClick = {
                            viewModel.openPack(
                                database,
                                username
                            )
                        }
                    ) {
                        Text("Open Pack")
                    }
                }

                PackUiState.Opening -> {
                    CircularProgressIndicator()

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Opening your pack...")
                }

                is PackUiState.Success -> {
                    Text(
                        text = "You opened ${state.cards.size} cards !",
                        style = MaterialTheme.typography.titleLarge,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement =
                            Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.cards) { card ->
                            Card(
                                modifier =
                                    Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier =
                                        Modifier.padding(16.dp),
                                ) {
                                    Text(
                                        text = card.name,
                                        style =
                                            MaterialTheme.typography.titleMedium,
                                    )
                                    Text(card.description)
                                    Text("Cost: ${card.cost}")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.openPack(
                                database,
                                username
                            )
                        }
                    ) {
                        Text("Open Another Pack")
                    }
                }

                is PackUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.openPack(
                                database,
                                username
                            )
                        }
                    ) {
                        Text("Try Again")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onNavigateBack) {
                Text("Back to Home")
            }
        }
    }
}