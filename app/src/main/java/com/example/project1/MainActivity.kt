package com.example.project1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.project1.ui.theme.MyApplicationTheme
//API Imports
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
//Logcat Imports
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }

        }
        lifecycleScope.launch {
            runCatching {
                fetchProducts()
            }.onSuccess { response ->
                Log.d("API_TEST:", response)
            }.onFailure { error ->
                Log.e("API_TEST", "API did not work ;/", error)
            }
        }
    }
}


//This is where I want to start working on the API
const val apiKey = "ak_5130f2d332404035a08e8d030472082a"

//This code came partly from the anycrap API website
// https://anycrap.shop/api/v1/docs#GET/products
val client = OkHttpClient()
private suspend fun fetchProducts(): String =
    withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://anycrap.shop/api/v1/products")
            .get()
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        val response = client.newCall(request).execute()

        response.use {
            if (!it.isSuccessful) {
                error("API did not work ;/ Error Code: ${it.code}")
            }
            //I had to add in checks becasue the body could be empty and andriod didnt know how to handle that
            it.body?.string() ?: error("Response body was empty")
        }
    }


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        Greeting("Android")
    }
}