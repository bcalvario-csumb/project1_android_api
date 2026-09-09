package com.example.project1.database.entities
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "user_has_card", foreignKeys = [ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE), ForeignKey(entity = Card::class, parentColumns = ["id"], childColumns = ["cardId"], onDelete = ForeignKey.CASCADE)])
data class UserHasCard (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val cardId: Int
)