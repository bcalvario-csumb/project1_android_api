package com.example.project1

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
    // COMMENTED OUT — HomeDestination/LoginDestination/SignUpDestination were the
    // String-route design, which AppNavHost.kt has since replaced with type-safe
    // @Serializable routes. This test guarded against two destinations sharing the
    // same route string; with @Serializable route *types* that can no longer happen,
    // because the compiler enforces it. Safe to delete once the team has seen it.
    //
    // @Test fun destinations_haveDistinctRoutes(){
    //     val routes = listOf(HomeDestination.ROUTE, LoginDestination.ROUTE, SignUpDestination.ROUTE)
    //     assertEquals(routes.size, routes.toSet().size)
    // }

    // TODO(team): replace with a serializer round-trip once the routes settle. This
    // turns a runtime SerializationException (app crashes on launch when the
    // kotlin-serialization plugin is missing) into a one-second test failure:
    //
    // @Test fun routes_areActuallySerializable() {
    //     assertEquals(HomeRoute, Json.decodeFromString<HomeRoute>(Json.encodeToString(HomeRoute)))
    // }
}