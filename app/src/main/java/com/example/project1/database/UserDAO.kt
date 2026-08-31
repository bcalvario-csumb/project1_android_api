package com.example.project1.database
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UserDAO {
   @Insert
   suspend fun insertUser(user: User)

   @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
   suspend fun getUserByUsername(username: String): User?
}