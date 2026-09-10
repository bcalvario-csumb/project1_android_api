package com.example.project1.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import android.util.Log


//Credit: Carlos Solian, moved and modified by Brandon Calvario

//This is where I want to start working on the API
const val API_KEY = "ak_5130f2d332404035a08e8d030472082a"
private const val TAG = "ProductsRepository"
class ProductsRepository(
    private val cache: ProductsCache,
    private val client: OkHttpClient = OkHttpClient()) {
    suspend fun fetchProducts(): String = withContext(Dispatchers.IO) {
        Log.d(TAG, "+++ Starting API REQUEST +++")
        val request = Request.Builder()
            .url("https://anycrap.shop/api/v1/products")
            .addHeader("Authorization", "Bearer $API_KEY")
            .build()
        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                Log.d(TAG, "API response code: ${response.code}")
                Log.d(TAG, "API response length: ${body?.length ?: 0}")
                Log.d(TAG, "Response preview: ${body?.take(500)}")
                if (!response.isSuccessful) {
                    error("HTTP ${response.code}")
                }
                val productsJson = body ?: error("Response body was empty.")
                cache.saveProducts(productsJson)
                productsJson
            }
        } catch (error: Exception) {
            Log.e(TAG, "API request failed. Loading cached data.", error)
            cache.getProducts() ?: error("No API or cached data available.")
        }
    }
    suspend fun fetchRandomProduct(): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://anycrap.shop/api/v1/products/random")
            .addHeader("Authorization", "Bearer $API_KEY")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("HTTP ${response.code}")
            }

            response.body?.string()
                ?: error("Response body was empty.")
        }
    }
}

