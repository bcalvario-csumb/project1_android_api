package com.example.project1.ui.login.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project1.data.ApiStatus
import com.example.project1.database.GameDatabase
import com.example.project1.database.UserSummary

/**
 * Administrator panel: list and search users, and check API health.
 *
 * Card removal is not wired up yet — see the TODO at the bottom of the user row.
 */
@Composable
fun AdminScreen(
    database: GameDatabase?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdminViewModel = viewModel(),
) {
    val usersState by viewModel.users.collectAsStateWithLifecycle()
    val apiState by viewModel.apiStatus.collectAsStateWithLifecycle()
    // Search text is ephemeral UI state, so it lives here rather than in the ViewModel —
    // the same way LoginScreen holds its email/password fields.
    var search by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(database) {
        viewModel.loadUsers(database)
        viewModel.checkApiStatus()
    }
    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Admin Panel", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            ApiStatusCard(
                state = apiState,
                onRefresh = { viewModel.checkApiStatus() },
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("Search users by name or email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            when (val s = usersState) {
                is AdminUsersState.Loading -> CircularProgressIndicator()
                is AdminUsersState.Error -> Button(onClick = { viewModel.loadUsers(database) }) {
                    Text("Retry: ${s.message}")
                }
                is AdminUsersState.Success -> {
                    // Filtering happens here because it is a pure function of already
                    // loaded data. If the user table ever grows large, move this into a
                    // SQL `WHERE email LIKE :q` query so SQLite does the work instead.
                    val matches = s.users.filter { user ->
                        search.isBlank() ||
                            user.email.contains(search, ignoreCase = true) ||
                            user.name.contains(search, ignoreCase = true)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("${matches.size} of ${s.users.size} users")
                        TextButton(onClick = { viewModel.loadUsers(database) }) {
                            Text("Refresh")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (matches.isEmpty()) {
                        Text(
                            if (s.users.isEmpty()) "No users registered yet."
                            else "No users match \"$search\".",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(matches, key = { it.id }) { user ->
                                UserRow(user)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back")
            }
        }
    }
}

@Composable
private fun ApiStatusCard(
    state: ApiStatusState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Colour comes from the theme's semantic roles, so the card stays readable in both
    // light and dark rather than hardcoding green/red.
    val container = when (state) {
        is ApiStatusState.Checking -> MaterialTheme.colorScheme.surfaceVariant
        is ApiStatusState.Ready ->
            if (state.status.reachable) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.errorContainer
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("API Status", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onRefresh) { Text("Check") }
            }
            when (state) {
                is ApiStatusState.Checking -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    CircularProgressIndicator()
                }
                is ApiStatusState.Ready -> ApiStatusDetail(state.status)
            }
        }
    }
}

@Composable
private fun ApiStatusDetail(status: ApiStatus) {
    Text(
        text = if (status.reachable) "Reachable" else "Unreachable",
        style = MaterialTheme.typography.bodyLarge,
    )
    Text(
        text = buildString {
            status.httpCode?.let { append("HTTP $it · ") }
            append("${status.latencyMs} ms")
        },
        style = MaterialTheme.typography.bodyMedium,
    )
    if (!status.reachable) {
        Text(status.detail, style = MaterialTheme.typography.bodySmall)
        // The distinction that matters operationally: with a cache the app still works
        // for users, without one it is fully broken.
        Text(
            text = if (status.cachedDataAvailable) {
                "Cached data available — app is degraded, not down."
            } else {
                "No cached data — the app cannot serve products."
            },
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun UserRow(user: UserSummary, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(user.name.ifBlank { "(no name)" }, style = MaterialTheme.typography.bodyLarge)
                Text(user.email, style = MaterialTheme.typography.bodyMedium)
            }
            Text("${user.cardCount} cards", style = MaterialTheme.typography.bodyMedium)
        }
        // TODO(team): card removal. UserHasCardDAO already has everything needed —
        //   getCardsForUser(userId) and removeCardFromUser(userId, cardId). The step is
        //   to expand this row into the user's cards and put a Remove button on each,
        //   calling a viewModel.removeCard(database, user.id, card.id) that reloads the
        //   list afterwards, the way HomeViewModel.tradeCard() already does.
    }
}
