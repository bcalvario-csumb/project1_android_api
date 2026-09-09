package com.example.project1.database
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.project1.database.entities.Card

@Dao
interface CardDAO {
    @Insert
    suspend fun insertCard(card: Card) : Long

    @Query("SELECT * FROM cards")
    suspend fun getAllCards(): List<Card>
}