package com.example.project1.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.project1.ui.home.HomeScreen
import com.example.project1.ui.login.LoginScreen
import com.example.project1.ui.signup.SignUpScreen
import kotlinx.serialization.Serializable

// Type-safe navigation routes (Navigation Compose 2.8+).
// Each destination is a @Serializable type instead of a String route, so the
// compiler guarantees routes are distinct and arguments are correctly typed.
// Requires the kotlin-serialization plugin (applied in app/build.gradle.kts).
//
// Route arguments are serialized into the back stack and persisted to disk on
// process death, so they carry identifiers only — never a password.
//
// Login and SignUp take no arguments: they are where credentials get *collected*,
// so there is nothing to pass in. What flows forward is the *result* of signing in,
// which is why HomeRoute is the one carrying a value.

@Serializable
object LoginRoute

@Serializable
object SignUpRoute

@Serializable
data class HomeRoute(val username: String)

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = LoginRoute,
        modifier = modifier,
    ) {
        composable<LoginRoute> {
            LoginScreen(
                onLoginSuccess = { username ->
                    navController.navigate(HomeRoute(username)) {
                        // Drop Login from the back stack so Back from Home exits the
                        // app instead of returning to the login form while signed in.
                        popUpTo<LoginRoute> { inclusive = true }
                    }
                },
                onNavigateToSignUp = { navController.navigate(SignUpRoute) },
            )
        }
        composable<SignUpRoute> {
            SignUpScreen(
                onSignUpSuccess = { username ->
                    navController.navigate(HomeRoute(username)) {
                        // SignUp sits above Login on the stack, so popping to Login
                        // inclusively clears both and leaves Home as the only entry.
                        popUpTo<LoginRoute> { inclusive = true }
                    }
                },
                // Plain back, Login is still on the stack underneath.
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable<HomeRoute> { backStackEntry ->
            // toRoute() deserializes the route back into the data class.
            // This is the type-safe equivalent of Express's req.params.
            val home = backStackEntry.toRoute<HomeRoute>()
            HomeScreen(
                username = home.username,
                onLogout = {
                    navController.navigate(LoginRoute) {
                        popUpTo<HomeRoute> { inclusive = true }
                    }
                },
            )
        }
    }
}
