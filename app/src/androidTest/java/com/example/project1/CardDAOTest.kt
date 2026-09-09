package com.example.project1
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.project1.database.CardDAO
import com.example.project1.database.entities.Card
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CardDAOTest : DatabaseTest() {
    private lateinit var cardDao: CardDAO

    @Before
    fun setupDao() {
        cardDao = database.cardDao()
    }

    @Test
    fun testCardDaoInsertAndRetrieve() = runBlocking {
        val card = Card(id = 1, name = "Ace of Spades", description = "High card", cost = 10, img = "ace.png")
        cardDao.insertCard(card)
        val allCards = cardDao.getAllCards()
        assertTrue(allCards.contains(card))
    }
}