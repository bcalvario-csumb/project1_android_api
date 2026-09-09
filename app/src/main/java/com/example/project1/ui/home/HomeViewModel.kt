package com.example.project1.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project1.data.ProductsRepository
import com.example.project1.database.GameDatabase
import com.example.project1.database.entities.Card
import com.example.project1.database.entities.UserHasCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState{
    data object Loading : HomeUiState
    data class Success (val userId: Int, val cards: List<Card>) : HomeUiState
    data class Error (val message : String) : HomeUiState
}
class HomeViewModel (private val repo: ProductsRepository =
ProductsRepository()) : ViewModel(){
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun load (database: GameDatabase?, username: String) = viewModelScope.launch {
        _uiState.value = HomeUiState.Loading

        if (database == null) {
            _uiState.value = HomeUiState.Error("Database not initialized")
            return@launch
        }

        _uiState.value = runCatching {
            val user = database.userDao().getUserByEmail(username) ?: throw Exception("User not found")
            val userCards = database.userHasCardDao().getCardsForUser(user.id)
            HomeUiState.Success(userId = user.id, cards = userCards)
        }.fold(onSuccess = { it }, onFailure = { HomeUiState.Error(it.message ?: "Unknown Error") })
    }

    fun tradeCard(database: GameDatabase?, currentUserId: Int, targetUserId: Int, cardId: Int, username: String) = viewModelScope.launch {
        if (database != null) {
            database.userHasCardDao().removeCardFromUser(currentUserId, cardId)
            database.userHasCardDao().insertUserCard(
                UserHasCard(
                    userId = targetUserId,
                    cardId = cardId
                )
            )
            load(database, username)
        }
    }
}
