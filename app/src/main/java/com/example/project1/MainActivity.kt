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

        // GameDatabase has been bumped 1 -> 2 -> 3 as the stats columns landed, but no
        // Migration was ever written for either step. Without a fallback, Room throws
        // "A migration from 1 to 3 was required but not found" and the app crashes on
        // launch for anyone upgrading in place. Fresh installs were unaffected, which is
        // why it went unnoticed.
        //
        // Destructive migration is the right call here rather than hand-written
        // Migrations: the v2 schema was never exported (app/schemas has 1.json and
        // 3.json only), so there is no recorded 1 -> 2 step to migrate against, and this
        // is a class project where local card collections are disposable.
        //
        // TODO(team): before any real release, replace this with proper Migration objects
        //   and stop deleting user data on upgrade.
        val database = Room.databaseBuilder(applicationContext, GameDatabase::class.java, "game_database")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

        setContent {
            MyApplicationTheme {
                AppNavHost(database = database);
            }
        }
    }
}
