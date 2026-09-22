package com.example.project1.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import kotlin.random.Random


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
// `open` so tests can subclass this with a fake that returns canned data or throws,
// instead of making real network calls. See HomeViewModelTest.
open class ProductsRepository(
    private val cache: ProductsCache,
    // Defaults to the app-wide shared client so the three ViewModels that each build a
    // ProductsRepository still end up sharing one connection pool. Injectable so a test
    // can pass a client pointed at a MockWebServer. See NetworkClient.
    private val client: OkHttpClient = NetworkClient.shared) {
    /**
     * The product catalogue, cache-first.
     *
     *  1. Cache younger than [CATALOGUE_TTL_MILLIS] -> returned with **no network call**.
     *  2. Otherwise fetch, save (which restamps the cache), and return.
     *  3. Fetch failed -> fall back to the cache however stale; throw only if there is none.
     *
     * Step 1 is new. Before it, every call hit the network and the cache was only ever the
     * step-3 emergency fallback -- which is why the catalogue was being re-requested
     * constantly. This is the "cache-aside" pattern with a time-to-live.
     */
    open suspend fun fetchProducts(): String = withContext(Dispatchers.IO) {
        cache.getFreshProducts(CATALOGUE_TTL_MILLIS)?.let { fresh ->
            Log.d(TAG, "Catalogue served from cache (younger than TTL); no request made.")
            return@withContext fresh
        }

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

    /**
     * One random product, for opening a pack.
     *
     * Deliberately NOT cache-first: `/products/random` returns a different item on every
     * call, so caching its response would make every pack contain the same card.
     *
     * When the network fails, it instead draws a random item from the cached *catalogue*
     * and returns it in this endpoint's `{"data":[item]}` shape, so callers cannot tell
     * the difference -- packs stay random, and opening one keeps working offline.
     *
     * Catches [IOException] specifically: that covers no network, DNS failure, and
     * timeouts (SocketTimeoutException extends it), which are the offline cases. An HTTP
     * error from a reachable server still throws, since that is not an offline problem.
     */
    suspend fun fetchRandomProduct(): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://anycrap.shop/api/v1/products/random")
            .addHeader("Authorization", "Bearer $API_KEY")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("HTTP ${response.code}")
                }

                response.body?.string()
                    ?: error("Response body was empty.")
            }
        } catch (error: IOException) {
            Log.w(TAG, "Random product request failed; drawing from cached catalogue.", error)
            val catalogue = cache.getProducts() ?: throw error
            pickRandomProduct(catalogue) ?: throw error
        }
    }
}

/**
 * Picks one product from a cached `/products` response and wraps it in the
 * `{"data":[item]}` shape that `/products/random` returns.
 *
 * Returns null when the catalogue is empty or unreadable, so the caller can surface the
 * original network error rather than a confusing parse failure.
 *
 * Pure -- no network, no Android storage -- so it is unit-tested on the JVM.
 *
 * @param random injectable so tests can seed it and assert on which item is chosen.
 */
internal fun pickRandomProduct(catalogueJson: String, random: Random = Random): String? {
    val items = runCatching { JSONObject(catalogueJson).getJSONArray("data") }.getOrNull()
    if (items == null || items.length() == 0) return null
    val picked = items.getJSONObject(random.nextInt(items.length()))
    return JSONObject().put("data", JSONArray().put(picked)).toString()
}

