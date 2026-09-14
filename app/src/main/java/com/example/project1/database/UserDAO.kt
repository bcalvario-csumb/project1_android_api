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

   /**
    * Every user plus how many cards they own, for the admin panel.
    *
    * A LEFT JOIN (not INNER) so users with an empty collection still appear with
    * cardCount = 0. One query rather than getAllUsers() followed by a count per user,
    * which would be N+1 round trips to SQLite.
    */
   @Query("""
       SELECT users.id AS id,
              users.email AS email,
              users.name AS name,
              COUNT(user_has_card.cardId) AS cardCount
       FROM users
       LEFT JOIN user_has_card ON users.id = user_has_card.userId
       GROUP BY users.id, users.email, users.name
       ORDER BY users.email
   """)
   suspend fun getAllUsersWithCardCount(): List<UserSummary>
}
