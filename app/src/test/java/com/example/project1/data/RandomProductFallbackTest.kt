package com.example.project1.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

/**
 * Tests [pickRandomProduct], the offline fallback for opening a pack.
 *
 * The trap this guards against: `/products/random` returns a different item every call,
 * so naively caching its response would make every offline pack contain the same card.
 * The fallback instead draws from the cached catalogue -- these tests check that it keeps
 * both the response *shape* (so OpenPackViewModel's parser is unchanged) and the
 * *randomness* (so offline packs still vary).
 *
 * Runs on the JVM because the real org.json is on the test classpath; see the
 * testImplementation(libs.org.json) comment in app/build.gradle.kts.
 */
class RandomProductFallbackTest {

    /** Same shape as the live `/products` response, trimmed to the fields that matter. */
    private val catalogue = """
        {"data":[
          {"id":"a","name":"Carrot Card","description":"d-a","image":"https://x/a.png"},
          {"id":"b","name":"Noodle Mouthwash","description":"d-b","image":"https://x/b.png"},
          {"id":"c","name":"Club Pinguin Sandwich","description":"d-c","image":"https://x/c.png"}
        ],"meta":{"page":1}}
    """.trimIndent()

    @Test
    fun matchesTheRandomEndpointShape_oneItemUnderData() {
        val picked = JSONObject(pickRandomProduct(catalogue)!!)
        // OpenPackViewModel reads getJSONArray("data") -- the fallback must look identical.
        assertEquals(1, picked.getJSONArray("data").length())
    }

    @Test
    fun keepsTheFieldsOpenPackReads() {
        val item = JSONObject(pickRandomProduct(catalogue)!!).getJSONArray("data").getJSONObject(0)
        assertNotNull(item.getString("name"))
        assertNotNull(item.getString("description"))
        assertNotNull(item.getString("image"))
    }

    @Test
    fun sameSeed_picksSameItem() {
        // Seeding proves the pick is driven by the Random we pass, not something hidden.
        assertEquals(
            pickRandomProduct(catalogue, Random(seed = 7)),
            pickRandomProduct(catalogue, Random(seed = 7)),
        )
    }

    @Test
    fun staysRandom_acrossManyDraws() {
        // The point of the fallback. Caching /products/random directly would pass every
        // shape test above and still fail this one: every pack would be the same card.
        val rng = Random(seed = 42)
        val names = List(DRAWS) {
            JSONObject(pickRandomProduct(catalogue, rng)!!)
                .getJSONArray("data").getJSONObject(0).getString("name")
        }.toSet()
        assertNotEquals("every draw returned the same card", 1, names.size)
    }

    @Test
    fun emptyCatalogue_returnsNull() {
        // Null lets the caller rethrow the real network error instead of a parse failure.
        assertNull(pickRandomProduct("""{"data":[]}"""))
    }

    @Test
    fun unreadableCache_returnsNull() {
        assertNull(pickRandomProduct("not json at all"))
    }

    @Test
    fun missingDataArray_returnsNull() {
        assertNull(pickRandomProduct("""{"meta":{}}"""))
    }

    private companion object {
        /** With 3 items, 20 seeded draws landing on one item is effectively impossible. */
        const val DRAWS = 20
    }
}
