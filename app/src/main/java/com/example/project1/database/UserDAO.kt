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
}