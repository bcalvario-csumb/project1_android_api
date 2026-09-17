package com.example.project1.ui.login.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.project1.data.ApiStatus
import com.example.project1.data.ProductsCache
import com.example.project1.data.ProductsRepository
import com.example.project1.database.GameDatabase
import com.example.project1.database.UserSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Email treated as the administrator.
 *
 * TODO(team): replace with an `isAdmin: Boolean = false` column on the User entity.
 *   That is a schema change, so it needs GameDatabase's version bumped to 2 plus a
 *   migration (or fallbackToDestructiveMigration during development). A hardcoded
 *   constant keeps the panel reachable without touching the shared database yet.
 */
const val ADMIN_EMAIL = "bcalvario@csumb.edu"

sealed interface AdminUsersState {
    data object Loading : AdminUsersState
    data class Success(val users: List<UserSummary>) : AdminUsersState
    data class Error(val message: String) : AdminUsersState
}

sealed interface ApiStatusState {
    data object Checking : ApiStatusState
    data class Ready(val status: ApiStatus) : ApiStatusState
}

class AdminViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = ProductsRepository(ProductsCache(application.applicationContext))

    // Two independent flows rather than one combined state: the user list comes from
    // local SQLite in milliseconds, while the API probe can take seconds or time out.
    // Keeping them separate means a slow network never blocks the user list from
    // rendering, and either can be refreshed without re-running the other.
    private val _users = MutableStateFlow<AdminUsersState>(AdminUsersState.Loading)
    val users: StateFlow<AdminUsersState> = _users.asStateFlow()
    private val _apiStatus = MutableStateFlow<ApiStatusState>(ApiStatusState.Checking)
    val apiStatus: StateFlow<ApiStatusState> = _apiStatus.asStateFlow()

    fun loadUsers(database: GameDatabase?) = viewModelScope.launch {
        _users.value = AdminUsersState.Loading
        if (database == null) {
            _users.value = AdminUsersState.Error("Database not initialized")
            return@launch
        }
        _users.value = runCatching { database.userDao().getAllUsersWithCardCount() }
            .fold(
                onSuccess = { AdminUsersState.Success(it) },
                onFailure = { AdminUsersState.Error(it.message ?: "Unknown Error") },
            )
    }

    fun checkApiStatus() = viewModelScope.launch {
        _apiStatus.value = ApiStatusState.Checking
        // checkApiStatus() reports failure inside ApiStatus rather than throwing, so
        // there is no error branch to handle here.
        _apiStatus.value = ApiStatusState.Ready(repo.checkApiStatus())
    }
}
