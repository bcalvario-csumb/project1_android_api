package com.example.project1.ui.pack

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.project1.data.ProductsCache
import com.example.project1.database.GameDatabase
import com.example.project1.database.entities.Card
import com.example.project1.database.entities.UserHasCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.project1.data.ProductsRepository
import okhttp3.OkHttpClient
import org.json.JSONObject

sealed interface PackUiState {
    data object Ready : PackUiState
    data object Opening : PackUiState
    data class Success(val cards: List<Card>) : PackUiState
    data class Error(val message: String) : PackUiState
}

class OpenPackViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = ProductsRepository(ProductsCache(application.applicationContext))
    private val _uiState = MutableStateFlow<PackUiState>(PackUiState.Ready)
    val uiState: StateFlow<PackUiState> = _uiState.asStateFlow()

    init {
        // Keep the offline fallback pool warm. When the network fails,
        // fetchRandomProduct() draws a card from the cached catalogue -- but only if one
        // has been cached. HomeViewModel used to refresh it as a side effect of a fetch
        // whose result it discarded; that fetch was removed, so the warm-up lives here,
        // in the one screen that actually needs the catalogue.
        //
        // Cheap: fetchProducts() is TTL-guarded, so this makes at most one request per
        // CATALOGUE_TTL_MILLIS however often the screen opens. Fire-and-forget in the
        // background, so it never delays opening a pack.
        //
        // runCatching, not try/catch, because an uncaught exception in viewModelScope
        // crashes the app -- and fetchProducts() throws on a first-ever offline launch,
        // when there is neither network nor cache.
        viewModelScope.launch { runCatching { repo.fetchProducts() } }
    }

    fun openPack(database: GameDatabase, username: String) = viewModelScope.launch {
        _uiState.value = PackUiState.Opening

        _uiState.value = runCatching {
            val user =
                database.userDao().getUserByEmail(username)
                    ?: throw Exception("Signed in user was not found")

            val responseText = repo.fetchRandomProduct()

            val responseJson = JSONObject(responseText)

            val products = responseJson.getJSONArray("data")

            if (products.length() == 0) {
                throw Exception("The API did not return any products")
            }

            var commonCount = 0
            var uniqueCount = 0
            var legendaryCount = 0
            val cardsToOpen = List(products.length()) { index ->
                val product = products.getJSONObject(index)

                val rng = (1..100).random()
                val tierCost = when (rng) {
                    100 -> { legendaryCount++; 100 }
                    in 90..99 -> { uniqueCount++; 50 }
                    else -> { commonCount++; 20 }
                }
                Card(
                    name = product.getString("name"),
                    description = product.getString("description"),
                    cost = tierCost,
                    img = product.getString("image"),
                )
            }

            val openedCards = cardsToOpen.map { card ->
                val newCardId = database.cardDao().insertCard(card).toInt()

                database.userHasCardDao().insertUserCard(
                    UserHasCard(
                        userId = user.id,
                        cardId = newCardId,
                    )
                )

                card.copy(id = newCardId)
            }

            val pointsEarned = openedCards.sumOf { it.cost }
            database.userDao().updatePoints(user.id, pointsEarned)
            database.userDao().incrementOpenedStats(user.id, openedCards.size, commonCount, uniqueCount, legendaryCount)

            PackUiState.Success(openedCards)
        }.getOrElse { error ->
            PackUiState.Error(
                error.message ?: "Could not open the pack, please try again."
            )
        }
    }
}
