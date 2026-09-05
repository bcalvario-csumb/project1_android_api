package com.example.project1
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.project1.database.GameDatabase
import org.junit.After
import org.junit.Before

open class DatabaseTest {
    protected lateinit var database: GameDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, GameDatabase::class.java).build()
    }

    @After
    fun closeDb() {
        database.close()
    }
}