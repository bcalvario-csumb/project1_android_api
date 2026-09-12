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

/**
 * Outcome of a single API probe. See [ProductsRepository.checkApiStatus].
 *
 * @param cachedDataAvailable whether the app could still serve data from the cache if
 *   the API is unreachable — the difference between "degraded" and "broken".
 */
data class ApiStatus(
    val reachable: Boolean,
    val httpCode: Int?,
    val latencyMs: Long,
    val detail: String,
    val cachedDataAvailable: Boolean,
)
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
    /**
     * Probes the API and reports what actually happened, for the admin panel.
     *
     * Unlike [fetchProducts], this deliberately does NOT fall back to the cache. That
     * fallback is right for the app (users get data either way) but wrong for a status
     * readout — an admin needs to know the API is down even while cached data is being
     * served, and fetchProducts() succeeds in both cases so it cannot tell them apart.
     */
    suspend fun checkApiStatus(): ApiStatus = withContext(Dispatchers.IO) {
        val startedAt = System.nanoTime()
        val cachedAvailable = cache.getProducts() != null
        fun elapsedMs() = (System.nanoTime() - startedAt) / 1_000_000

        val request = Request.Builder()
            .url("https://anycrap.shop/api/v1/products")
            .addHeader("Authorization", "Bearer $API_KEY")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                ApiStatus(
                    reachable = response.isSuccessful,
                    httpCode = response.code,
                    latencyMs = elapsedMs(),
                    detail = if (response.isSuccessful) "OK" else "HTTP ${response.code}",
                    cachedDataAvailable = cachedAvailable,
                )
            }
        } catch (error: Exception) {
            Log.e(TAG, "API status probe failed", error)
            ApiStatus(
                reachable = false,
                httpCode = null,
                latencyMs = elapsedMs(),
                // No network at all gives UnknownHostException, whose message is just the
                // hostname — the class name is more use to whoever is reading the panel.
                detail = error.message ?: error::class.simpleName ?: "Unknown error",
                cachedDataAvailable = cachedAvailable,
            )
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

