package com.example.project1.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

//Credit: Carlos Solian, moved and modified by Brandon Calvario

//This is where I want to start working on the API
const val API_KEY = "ak_5130f2d332404035a08e8d030472082a"
class ProductsRepository (private val client : OkHttpClient = OkHttpClient()){
    suspend fun fetchProducts(): String = withContext(Dispatchers.IO){
        val request = Request.Builder()
            .url("https://anycrap.shop/api/v1/products")
            .addHeader("Authorization", "Bearer $API_KEY")
            .build()
        client.newCall(request).execute().use {
            r -> if (!r.isSuccessful) error("HTTP ${r.code}")
            r.body?.string() ?: error("Response body was empty.")
        }
    }
}

