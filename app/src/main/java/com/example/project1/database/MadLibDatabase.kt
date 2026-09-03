package com.example.project1.database
import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.project1.database.entities.User

class MadLibDatabase {
    @Database(entities = [User::class], version = 1)
    abstract class AppDatabase : RoomDatabase() {
        abstract fun userDao(): UserDAO
    }
}