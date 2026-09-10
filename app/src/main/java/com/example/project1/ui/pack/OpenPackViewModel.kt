package com.example.project1.ui.pack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project1.database.GameDatabase
import com.example.project1.database.entities.Card
import com.example.project1.database.entities.UserHasCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.project1.data.ProductsRepository
import org.json.JSONObject

sealed interface PackUiState {
    data object Ready : PackUiState
    data object Opening : PackUiState
    data class Success(val cards: List<Card>) : PackUiState
    data class Error(val message: String) : PackUiState
}

class OpenPackViewModel(private val repo: ProductsRepository = ProductsRepository()) :
    ViewModel() {
    private val _uiState = MutableStateFlow<PackUiState>(PackUiState.Ready)
    val uiState: StateFlow<PackUiState> = _uiState.asStateFlow()

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

            val cardsToOpen = List(products.length()) { index ->
                val product = products.getJSONObject(index)

                Card(
                    name = product.getString("name"),
                    description = product.getString("description"),
                    cost = 20,
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

            PackUiState.Success(openedCards)
        }.getOrElse { error ->
            PackUiState.Error(
                error.message ?: "Could not open the pack, please try again."
            )
        }
    }
}