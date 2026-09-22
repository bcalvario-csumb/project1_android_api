package com.example.project1.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests [isFresh], the rule that decides whether the catalogue cache can be served with
 * no network request.
 *
 * This is the whole of the "stop re-calling the API constantly" behaviour, so each edge
 * case gets its own test. A regression that made everything look fresh would freeze the
 * catalogue forever; one that made everything look stale would silently restore the
 * refetch-on-every-call behaviour this was written to remove.
 */
class ProductsCacheTest {

    private companion object {
        const val SAVED_AT = 1_000_000L
        const val TTL = CATALOGUE_TTL_MILLIS
    }

    @Test
    fun justSaved_isFresh() {
        assertTrue(isFresh(savedAtMillis = SAVED_AT, nowMillis = SAVED_AT, maxAgeMillis = TTL))
    }

    @Test
    fun withinTtl_isFresh() {
        assertTrue(isFresh(SAVED_AT, nowMillis = SAVED_AT + TTL / 2, maxAgeMillis = TTL))
    }

    @Test
    fun exactlyAtTtl_isStillFresh() {
        // Boundary is inclusive. Pinned so an off-by-one change is a deliberate decision.
        assertTrue(isFresh(SAVED_AT, nowMillis = SAVED_AT + TTL, maxAgeMillis = TTL))
    }

    @Test
    fun oneMillisecondPastTtl_isStale() {
        assertFalse(isFresh(SAVED_AT, nowMillis = SAVED_AT + TTL + 1, maxAgeMillis = TTL))
    }

    @Test
    fun neverSaved_isStale() {
        // Caches written before timestamps existed hold JSON but no time. They must be
        // treated as stale so the first fetch records a time, rather than trusted forever.
        assertFalse(isFresh(NEVER_SAVED, nowMillis = SAVED_AT, maxAgeMillis = TTL))
    }

    @Test
    fun clockMovedBackwards_isStale() {
        // "now" earlier than "saved" means the device clock changed. The true age is
        // unknowable, so refetching is safer than trusting a negative age.
        assertFalse(isFresh(SAVED_AT, nowMillis = SAVED_AT - 1, maxAgeMillis = TTL))
    }

    @Test
    fun zeroTtl_meansAlwaysRefetchOnceTimePasses() {
        // Documents the knob: with no TTL, anything older than "this instant" is stale.
        assertFalse(isFresh(SAVED_AT, nowMillis = SAVED_AT + 1, maxAgeMillis = 0))
    }
}
