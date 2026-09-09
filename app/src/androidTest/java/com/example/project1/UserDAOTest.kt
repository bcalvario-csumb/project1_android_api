package com.example.project1
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.project1.database.UserDAO
import com.example.project1.database.entities.User
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserDAOTest : DatabaseTest() {
    private lateinit var userDao: UserDAO

    @Before
    fun setupDao() {
        userDao = database.userDao()
    }

    @Test
    fun testUserDaoInsertAndRetrieve() = runBlocking {
        val user = User(id = 1, name = "TestUser", email = "test@domain.com", password = "123")
        userDao.insertUser(user)
        val retrievedUser = userDao.getUserByEmail("test@domain.com")
        assertEquals("TestUser", retrievedUser?.name)
    }
}