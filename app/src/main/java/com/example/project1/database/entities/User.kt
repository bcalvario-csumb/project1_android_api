package com.example.project1.database.entities
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val email: String,
    val password: String,
    val name: String,
    val points: Int = 0,
    val totalCardsOpened: Int = 0,
    val totalCardsTraded: Int = 0,
    val commonOpened: Int = 0,
    val uniqueOpened: Int = 0,
    val legendaryOpened: Int = 0
)
