package com.example.project1.database
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.project1.database.entities.Card
import com.example.project1.database.entities.UserHasCard

@Dao
interface UserHasCardDAO {
    @Insert
    suspend fun insertUserCard(userHasCard: UserHasCard)

    @Query("""
        SELECT cards.* FROM cards
        INNER JOIN user_has_card ON cards.id = user_has_card.cardId
        WHERE user_has_card.userId = :userId
        """)
    suspend fun getCardsForUser(userId: Int): List<Card>
}