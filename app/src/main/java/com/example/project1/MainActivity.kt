package com.example.project1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.project1.ui.theme.MyApplicationTheme
import com.example.project1.ui.nav.AppNavHost
import androidx.room.Room
import com.example.project1.database.GameDatabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = Room.databaseBuilder(applicationContext, GameDatabase::class.java, "game_database").build()

        setContent {
            MyApplicationTheme {
                AppNavHost(database = database);
            }
        }
    }
}