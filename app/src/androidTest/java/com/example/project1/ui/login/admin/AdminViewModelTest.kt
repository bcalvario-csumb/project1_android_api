package com.example.project1.ui.login.admin

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
 * [AdminViewModel] is an `AndroidViewModel` with no injection seam (see
 * [com.example.project1.ui.home.HomeViewModelTest] for the same constraint), so it is
 * tested here with a real `Application` context and an in-memory Room database rather
 * than on the JVM. Only `loadUsers()` is covered -- it touches only local SQLite.
 * `checkApiStatus()` requires the network and is intentionally not exercised here.
 */
@RunWith(AndroidJUnit4::class)
class AdminViewModelTest {
    private lateinit var database: GameDatabase
    private lateinit var viewModel: AdminViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        database = Room.inMemoryDatabaseBuilder(context, GameDatabase::class.java).build()
        viewModel = AdminViewModel(context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun loadUsers_withNullDatabase_yieldsDatabaseNotInitializedError() = runBlocking {
        viewModel.loadUsers(null)

        val state = withTimeout(10_000) {
            viewModel.users.first { it !is AdminUsersState.Loading }
        }

        assertEquals(AdminUsersState.Error("Database not initialized"), state)
    }

    @Test
    fun loadUsers_withDatabase_yieldsUserSummaries() = runBlocking {
        database.userDao().insertUser(
            User(email = "a@example.com", password = "pw", name = "Alice")
        )

        viewModel.loadUsers(database)

        val state = withTimeout(10_000) {
            viewModel.users.first { it !is AdminUsersState.Loading }
        }

        assertTrue(state is AdminUsersState.Success)
        val users = (state as AdminUsersState.Success).users
        assertEquals(1, users.size)
        assertEquals("a@example.com", users[0].email)
        assertEquals(0, users[0].cardCount)
    }
}
