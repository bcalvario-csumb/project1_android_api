package com.example.project1.ui.pack

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.project1.database.GameDatabase
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
 * [OpenPackViewModel] is an `AndroidViewModel` with no injection seam (see
 * [com.example.project1.ui.home.HomeViewModelTest]), so it is tested here against a real
 * `Application` context and an in-memory Room database.
 *
 * Unlike `HomeViewModel.load`, `openPack()` takes a non-null `GameDatabase`, so there is
 * no null-database branch to exercise. It looks up the user *before* calling the network
 * (`repo.fetchRandomProduct()`), so the "unknown user" failure path never reaches the
 * network and is safe to assert here.
 */
@RunWith(AndroidJUnit4::class)
class OpenPackViewModelTest {
    private lateinit var database: GameDatabase
    private lateinit var viewModel: OpenPackViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        database = Room.inMemoryDatabaseBuilder(context, GameDatabase::class.java).build()
        viewModel = OpenPackViewModel(context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun initialState_isReady() {
        assertEquals(PackUiState.Ready, viewModel.uiState.value)
    }

    @Test
    fun openPack_withUnknownUser_yieldsUserNotFoundError() = runBlocking {
        viewModel.openPack(database, "nobody@example.com")

        val state = withTimeout(10_000) {
            viewModel.uiState.first { it !is PackUiState.Ready && it !is PackUiState.Opening }
        }

        assertTrue(state is PackUiState.Error)
        assertEquals("Signed in user was not found", (state as PackUiState.Error).message)
    }
}
