package com.example.project1.database
import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.project1.database.entities.Card
import com.example.project1.database.entities.User
import com.example.project1.database.entities.UserHasCard

@Database(entities = [User::class, Card::class, UserHasCard::class], version = 1, exportSchema = true)
abstract class GameDatabase : RoomDatabase() {
    abstract fun userDao(): UserDAO
    abstract fun cardDao(): CardDAO
    abstract fun userHasCardDao(): UserHasCardDAO
}