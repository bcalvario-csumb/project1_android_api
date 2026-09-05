package com.example.project1
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.project1.database.UserHasCardDAO
import com.example.project1.database.entities.Card
import com.example.project1.database.entities.User
import com.example.project1.database.entities.UserHasCard
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserHasCardDAOTest : BaseDatabaseTest() {
    private lateinit var userHasCardDao: UserHasCardDAO

    @Before
    fun setupDao() {
        userHasCardDao = database.userHasCardDao()
    }

    @Test
    fun testUserHasCardDaoRetrieval() = runBlocking {
        val user = User(id = 1, name = "TestUser", email = "test@domain.com", password = "123")
        val card = Card(id = 1, name = "Ace of Spades", description = "High card", cost = 10, img = "ace.png")
        database.userDao().insertUser(user)
        database.cardDao().insertCard(card)
        val userCard = UserHasCard(userId = 1, cardId = 1)
        userHasCardDao.insertUserCard(userCard)
        val userCards = userHasCardDao.getCardsForUser(1)
        assertEquals(1, userCards.size)
        assertEquals("Ace of Spades", userCards[0].name)
    }
}