package com.example.project1.data

import android.content.Context

/**
 * How long a cached catalogue counts as fresh.
 *
 * While the cache is younger than this, [ProductsRepository.fetchProducts] serves it with
 * no network request at all. The catalogue is a 10-item list that changes rarely, so a
 * quarter of an hour of staleness is invisible to a player, and it caps catalogue traffic
 * at four requests an hour no matter how often screens ask for it.
 *
 * Raising it saves requests but delays catalogue changes reaching the app; lowering it
 * does the reverse. Zero would mean "always refetch", which is what the app did before.
 */
const val CATALOGUE_TTL_MILLIS: Long = 15 * 60 * 1000L

/** Stored timestamp meaning "no timestamp recorded". */
internal const val NEVER_SAVED: Long = 0L

// Credit: Carlos Solian (original cache), timestamping added by Brandon Calvario.
/**
 * Stores the last successful `/products` response on disk so the app keeps working
 * when the API is unreachable.
 *
 * Two ways to read it, for two different questions:
 *  - [getFreshProducts] -- *"can I skip the network entirely?"* Only returns data younger
 *    than a given age. This is what prevents constant re-calling.
 *  - [getProducts] -- *"the network failed, what can I show?"* Returns whatever is stored,
 *    however old. Stale data beats no data when offline.
 *
 * Before timestamps were added, the cache could only answer the second question, so every
 * caller hit the network first and used the cache only as an emergency fallback.
 *
 * @param clock injectable so tests can control "now". Production uses wall-clock time.
 */
class ProductsCache(
    context: Context,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val p = context.getSharedPreferences("products_cache", Context.MODE_PRIVATE)

    fun saveProducts(json: String) {
        p.edit()
            .putString(KEY_JSON, json)
            .putLong(KEY_SAVED_AT, clock())
            .apply()
    }

    /** Whatever is cached, however old. The offline fallback. */
    fun getProducts(): String? {
        return p.getString(KEY_JSON, null)
    }

    /** The cached catalogue only if it is younger than [maxAgeMillis]; otherwise null. */
    fun getFreshProducts(maxAgeMillis: Long): String? {
        val json = getProducts() ?: return null
        val savedAt = p.getLong(KEY_SAVED_AT, NEVER_SAVED)
        return json.takeIf { isFresh(savedAt, clock(), maxAgeMillis) }
    }

    private companion object {
        // Unchanged from the original cache, so data saved by older builds still loads.
        const val KEY_JSON = "cached_products"
        const val KEY_SAVED_AT = "cached_products_saved_at"
    }
}

/**
 * Whether something saved at [savedAtMillis] is still fresh at [nowMillis].
 *
 * Pure -- no Android types -- so it is unit-tested on the JVM. See ProductsCacheTest.
 *
 * Two edge cases are deliberately *not* fresh:
 *  - [NEVER_SAVED]: caches written by builds from before timestamps existed have the JSON
 *    but no time. Treating them as stale forces one refetch, which then records a time.
 *  - negative age: the device clock moved backwards (manual change, timezone bug). The
 *    true age is unknowable, so refetch rather than trust it.
 */
internal fun isFresh(savedAtMillis: Long, nowMillis: Long, maxAgeMillis: Long): Boolean {
    if (savedAtMillis == NEVER_SAVED) return false
    val ageMillis = nowMillis - savedAtMillis
    return ageMillis in 0..maxAgeMillis
}
