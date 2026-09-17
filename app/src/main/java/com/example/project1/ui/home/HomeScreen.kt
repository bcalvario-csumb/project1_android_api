package com.example.project1.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card as ComposeCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.project1.database.GameDatabase
import com.example.project1.database.entities.Card
import com.example.project1.ui.login.admin.ADMIN_EMAIL
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue


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
    onOpenPack: () -> Unit,
    onOpenAdmin: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var expandedCardId by rememberSaveable {
        mutableStateOf<Int?>(null)
    }

    androidx.compose.runtime.LaunchedEffect(database) {
        viewModel.load(database, username)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (expandedCardId != null) {
                    Modifier.blur(10.dp)
                } else {
                    Modifier
                }
            ),
    ) { padding ->
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
            Button(
                onClick = onOpenPack,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Open Card Pack")
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Exhaustive over HomeUiState, no `else` branch needed, and adding a
            // fourth state will stop this compiling until it is handled here.
            when (val s = state) {
                is HomeUiState.Loading -> CircularProgressIndicator()

                is HomeUiState.Error -> Button(onClick = { viewModel.load(database, username) }) {
                    Text("Retry: ${s.message}")
                }

                is HomeUiState.Success -> {
                    ComposeCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                            Text("Lifetime Stats", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Cards Opened: ${s.totalOpened}", style = MaterialTheme.typography.headlineSmall)
                            Text("Cards Traded: ${s.totalTraded}", style = MaterialTheme.typography.headlineSmall)

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Common Pulls: ${s.commonOpened}", style = MaterialTheme.typography.bodyMedium)
                            Text("Unique Pulls: ${s.uniqueOpened}", style = MaterialTheme.typography.bodyMedium)
                            Text("Legendary Pulls: ${s.legendaryOpened}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Total Points: ${s.points}", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Your Deck (${s.cards.size} Cards)")
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(
                            items = s.cards,
                            key = { card -> card.id },
                        ) { card ->
                            PlayingCard(
                                card = card,
                                onClick = {
                                    expandedCardId =
                                        if (expandedCardId == card.id) {
                                            null
                                        } else {
                                            card.id
                                        }
                                },
                            )
                        }
                    }

                    s.cards.firstOrNull { it.id == expandedCardId }?.let { card ->
                        ExpandedCardDialog(
                            card = card,
                            onDismiss = {
                                expandedCardId = null
                            },
                            onTrade = {
                                viewModel.tradeCard(
                                    database = database,
                                    currentUserId = s.userId,
                                    targetUserId = 2,
                                    cardId = card.id,
                                    cardCost = card.cost,
                                    username = username,
                                )

                                expandedCardId = null
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (username == ADMIN_EMAIL) {
                TextButton(onClick = onOpenAdmin) {
                    Text("Admin Panel")
                }
            }

            TextButton(onClick = onLogout) {
                Text("Log Out")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PlayingCard(
    card: Card,
    onClick: () -> Unit,
) {
    ComposeCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(5f / 7f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        PlayingCardContent(
            card = card,
            expanded = false,
        )
    }
}

@Composable
private fun ExpandedCardDialog(
    card: Card,
    onDismiss: () -> Unit,
    onTrade: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ComposeCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(5f / 7f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                PlayingCardContent(
                    card = card,
                    expanded = true,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Back")
                }

                Button(
                    onClick = onTrade,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Trade")
                }
            }
        }
    }
}

@Composable
private fun PlayingCardContent(
    card: Card,
    expanded: Boolean,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
    ) {
        val titleHeight = 24.dp

        // The title, spacer, and image use no more than half
        // of the card's available content height.
        val imageHeight =
            (maxHeight / 2 - titleHeight - 4.dp)
                .coerceAtLeast(1.dp)

        val descriptionScrollState = rememberScrollState()

        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                text = card.name,
                style = if (expanded) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.labelLarge
                },
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(titleHeight)
                    .padding(horizontal = 2.dp)
                    .basicMarquee(
                        iterations = Int.MAX_VALUE,
                    ),
            )

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (card.img.isBlank()) {
                    Text(
                        text = "No image",
                        style = MaterialTheme.typography.labelSmall,
                    )
                } else {
                    AsyncImage(
                        model = card.img,
                        contentDescription = "Image of ${card.name}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .then(
                        if (expanded) {
                            Modifier
                                .verticalScroll(descriptionScrollState)
                                .testTag("expanded_card_description")
                        } else {
                            Modifier
                        }
                    ),
            ) {
                Text(
                    text = card.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = if (expanded) {
                        Int.MAX_VALUE
                    } else {
                        5
                    },
                    overflow = if (expanded) {
                        TextOverflow.Clip
                    } else {
                        TextOverflow.Ellipsis
                    },
                )
            }
        }

        // This is now a layered child of BoxWithConstraints.
        if (card.cost > 0) {
            LegendaryFoilOverlay(
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun LegendaryFoilOverlay(
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(
        label = "legendaryFoil",
    )

    val foilOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2200,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "foilOffset",
    )

    Canvas(
        modifier = modifier,
    ) {
        val diagonal = size.width + size.height
        val startX = foilOffset * diagonal

        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0x44FF4D8D),
                    Color(0x444DFFFF),
                    Color(0x44A64DFF),
                    Color(0x44FFF04D),
                    Color.Transparent,
                ),
                start = Offset(
                    x = startX,
                    y = 0f,
                ),
                end = Offset(
                    x = startX + diagonal * 0.6f,
                    y = diagonal,
                ),
            ),
        )
    }
}
