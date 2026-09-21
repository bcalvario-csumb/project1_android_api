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
import android.util.Log //importing log cat for personal reference - Carlos
//Adding in these so that I can use the cache files
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.project1.data.ProductsCache
sealed interface HomeUiState{
    data object Loading : HomeUiState
    data class Success (val userId: Int, val cards: List<Card>, val points: Int, val totalOpened: Int, val totalTraded: Int, val commonOpened: Int, val uniqueOpened: Int, val legendaryOpened: Int) : HomeUiState
    data class Error (val message : String) : HomeUiState
}
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = ProductsRepository(ProductsCache(application.applicationContext))
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun load(database: GameDatabase?, username: String) = viewModelScope.launch {
        _uiState.value = HomeUiState.Loading

        if (database == null) {
            _uiState.value = HomeUiState.Error("Database not initialized")
            return@launch
        }

        // NOTE: this used to call repo.fetchProducts() here and do nothing with the
        // result except log its length. That was a full ~8.6 KB network round trip on
        // every visit to Home and on every Retry, whose only output was a log line --
        // the cards below come from Room, not from that response. Removed.
        //
        // It did have one side effect: saving the response kept the catalogue cache
        // warm. That job now lives, deliberately and TTL-guarded, in OpenPackViewModel,
        // the only screen that reads the catalogue.
        //
        // Cards are read from the local database, which is the single source of truth
        // for this screen. Per Android's data-layer guidance, network data should reach
        // the UI *through* that local store rather than alongside it:
        // https://developer.android.com/topic/architecture/data-layer/offline-first

        _uiState.value = runCatching {
            val user =
                database.userDao().getUserByEmail(username) ?: throw Exception("User not found")
            val userCards = database.userHasCardDao().getCardsForUser(user.id)
            HomeUiState.Success(userId = user.id, cards = userCards, points = user.points, totalOpened = user.totalCardsOpened, totalTraded = user.totalCardsTraded, commonOpened = user.commonOpened, uniqueOpened = user.uniqueOpened, legendaryOpened = user.legendaryOpened)
        }.fold(onSuccess = { it }, onFailure = { HomeUiState.Error(it.message ?: "Unknown Error") })
    }

    fun tradeCard(database: GameDatabase?, currentUserId: Int, targetUserId: Int, cardId: Int, cardCost: Int, username: String) = viewModelScope.launch {
        if (database == null) {
            return@launch
        }

        try {
            database.userHasCardDao().removeCardFromUser(currentUserId, cardId)
            database.userDao().updatePoints(currentUserId, -cardCost)
            database.userDao().incrementTradedStat(currentUserId)
            try {
                database.userHasCardDao().insertUserCard(UserHasCard(userId = targetUserId, cardId = cardId))
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Target user $targetUserId does not exist. Card discarded.")
            }
            load(database, username)
        } catch (e: Exception) {
            _uiState.value = HomeUiState.Error(e.message ?: "Trade transaction failed")
        }
    }
}
