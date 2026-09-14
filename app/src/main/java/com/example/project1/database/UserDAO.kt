package com.example.project1.database
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.project1.database.entities.User

@Dao
interface UserDAO {
   @Insert
   suspend fun insertUser(user: User) : Long

   @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
   suspend fun getUserByEmail(email: String): User?

   @Query("SELECT * FROM users WHERE email = :email AND password = :password LIMIT 1")
   suspend fun validateLogin(email: String, password: String): User?

   @Query("UPDATE users SET points = points + :amount WHERE id = :userId")
   suspend fun updatePoints(userId: Int, amount: Int)

   @Query("UPDATE users SET totalCardsOpened = totalCardsOpened + :total, " + "commonOpened = commonOpened + :common, uniqueOpened = uniqueOpened + :unique, legendaryOpened = legendaryOpened + :legendary WHERE id = :userId")
   suspend fun incrementOpenedStats(userId: Int, total: Int, common: Int, unique: Int, legendary: Int)

   @Query("UPDATE users SET totalCardsTraded = totalCardsTraded + 1 WHERE id = :userId")
   suspend fun incrementTradedStat(userId: Int)
}