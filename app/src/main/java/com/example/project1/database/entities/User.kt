package com.example.project1.database.entities
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val username: String,
    val password: String
)