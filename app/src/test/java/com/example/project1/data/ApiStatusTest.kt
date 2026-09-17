package com.example.project1.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [ApiStatus] is a plain data class produced by [ProductsRepository.checkApiStatus] for
 * the admin panel. The semantic the panel relies on is `cachedDataAvailable`: it is what
 * distinguishes "the API is down but users still see data" (degraded) from "the API is
 * down and there is nothing to serve" (down). These tests lock that down along with
 * ordinary equality/copy semantics.
 */
class ApiStatusTest {

    @Test
    fun unreachableWithCache_isDegradedNotDown() {
        val degraded = ApiStatus(
            reachable = false,
            httpCode = null,
            latencyMs = 50L,
            detail = "UnknownHostException",
            cachedDataAvailable = true,
        )

        assertFalse(degraded.reachable)
        assertTrue("cached data must still be available when degraded", degraded.cachedDataAvailable)
    }

    @Test
    fun unreachableWithoutCache_isFullyDown() {
        val down = ApiStatus(
            reachable = false,
            httpCode = null,
            latencyMs = 50L,
            detail = "UnknownHostException",
            cachedDataAvailable = false,
        )

        assertFalse(down.reachable)
        assertFalse("no cached data means the app has nothing to serve", down.cachedDataAvailable)
    }

    @Test
    fun reachable_reportsHttpCodeAndDetail() {
        val ok = ApiStatus(
            reachable = true,
            httpCode = 200,
            latencyMs = 123L,
            detail = "OK",
            cachedDataAvailable = true,
        )

        assertEquals(200, ok.httpCode)
        assertEquals("OK", ok.detail)
    }

    @Test
    fun equality_isBasedOnAllProperties() {
        val a = ApiStatus(true, 200, 10L, "OK", true)
        val b = ApiStatus(true, 200, 10L, "OK", true)
        val c = a.copy(cachedDataAvailable = false)

        assertEquals(a, b)
        assertEquals(a, a.copy())
        assertTrue(a != c)
    }
}
