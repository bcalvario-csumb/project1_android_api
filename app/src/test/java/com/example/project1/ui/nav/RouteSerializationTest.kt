package com.example.project1.ui.nav

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Navigation Compose resolves a `KSerializer` for each route type at RUNTIME (when the
 * NavHost is built), not at compile time. If the `kotlin-serialization` plugin is ever
 * removed from app/build.gradle.kts, the app still compiles -- it only crashes the first
 * time a route is navigated to. These tests turn that into a one-second local failure by
 * round-tripping every route through the same `kotlinx.serialization.json.Json` machinery
 * Navigation Compose uses internally.
 */
class RouteSerializationTest {
    private val json = Json

    @Test
    fun loginRoute_roundTrips() {
        val encoded = json.encodeToString(LoginRoute)
        val decoded = json.decodeFromString<LoginRoute>(encoded)
        assertEquals(LoginRoute, decoded)
    }

    @Test
    fun signUpRoute_roundTrips() {
        val encoded = json.encodeToString(SignUpRoute)
        val decoded = json.decodeFromString<SignUpRoute>(encoded)
        assertEquals(SignUpRoute, decoded)
    }

    @Test
    fun adminRoute_roundTrips() {
        val encoded = json.encodeToString(AdminRoute)
        val decoded = json.decodeFromString<AdminRoute>(encoded)
        assertEquals(AdminRoute, decoded)
    }

    @Test
    fun homeRoute_roundTrips() {
        val route = HomeRoute(username = "someone@example.com")
        val encoded = json.encodeToString(route)
        val decoded = json.decodeFromString<HomeRoute>(encoded)
        assertEquals(route, decoded)
    }

    @Test
    fun openPackRoute_roundTrips() {
        val route = OpenPackRoute(username = "someone@example.com")
        val encoded = json.encodeToString(route)
        val decoded = json.decodeFromString<OpenPackRoute>(encoded)
        assertEquals(route, decoded)
    }

    // Route arguments end up URL-shaped on the back stack, so a username containing
    // characters that are meaningful in a URL (space, slash, ?, =, &, #) must survive
    // the round trip untouched.
    @Test
    fun homeRoute_survivesAwkwardCharactersInArgument() {
        val awkward = "a b/c?d=e&f#g"
        val route = HomeRoute(username = awkward)
        val encoded = json.encodeToString(route)
        val decoded = json.decodeFromString<HomeRoute>(encoded)
        assertEquals(awkward, decoded.username)
    }

    @Test
    fun openPackRoute_survivesAwkwardCharactersInArgument() {
        val awkward = "a b/c?d=e&f#g"
        val route = OpenPackRoute(username = awkward)
        val encoded = json.encodeToString(route)
        val decoded = json.decodeFromString<OpenPackRoute>(encoded)
        assertEquals(awkward, decoded.username)
    }
}
