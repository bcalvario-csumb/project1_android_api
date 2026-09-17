package com.example.project1.ui.home

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.project1.database.GameDatabase
import com.example.project1.database.entities.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [HomeViewModel] is an `AndroidViewModel` that builds `ProductsRepository(ProductsCache(context))`
 * internally -- there is no injection seam, so it cannot be constructed on the JVM. This
 * lives in androidTest instead, using a real `Application` context and an in-memory Room
 * database, and it will only run against a connected device/emulator.
 *
 * `load()` always attempts `repo.fetchProducts()` first, but that call's outcome is
 * swallowed internally (logged, not propagated) -- the final `HomeUiState` depends only
 * on the local database lookup, so these assertions do not depend on network access.
 */
@RunWith(AndroidJUnit4::class)
class HomeViewModelTest {
    private lateinit var database: GameDatabase
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        database = Room.inMemoryDatabaseBuilder(context, GameDatabase::class.java).build()
        viewModel = HomeViewModel(context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun load_withNullDatabase_yieldsDatabaseNotInitializedError() = runBlocking {
        viewModel.load(null, "someone@example.com")

        val state = withTimeout(10_000) {
            viewModel.uiState.first { it !is HomeUiState.Loading }
        }

        assertEquals(HomeUiState.Error("Database not initialized"), state)
    }

    @Test
    fun load_withUnknownUser_yieldsUserNotFoundError() = runBlocking {
        viewModel.load(database, "nobody@example.com")

        val state = withTimeout(10_000) {
            viewModel.uiState.first { it !is HomeUiState.Loading }
        }

        assertTrue(state is HomeUiState.Error)
        assertEquals("User not found", (state as HomeUiState.Error).message)
    }

    @Test
    fun load_withKnownUser_yieldsSuccessWithEmptyDeck() = runBlocking {
        val userId = database.userDao().insertUser(
            User(email = "known@example.com", password = "pw", name = "Known User")
        ).toInt()

        viewModel.load(database, "known@example.com")

        val state = withTimeout(10_000) {
            viewModel.uiState.first { it !is HomeUiState.Loading }
        }

        assertTrue(state is HomeUiState.Success)
        val success = state as HomeUiState.Success
        assertEquals(userId, success.userId)
        assertTrue(success.cards.isEmpty())
    }
}
