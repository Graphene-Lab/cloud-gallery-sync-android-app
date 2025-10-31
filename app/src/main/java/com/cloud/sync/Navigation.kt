package com.cloud.sync

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cloud.sync.ui.auth.AuthScreen
import com.cloud.sync.ui.login.LoginScreen
import com.cloud.sync.ui.profile.ProfileScreen
import com.cloud.sync.ui.scan.ScanScreen
import com.cloud.sync.ui.subscription.SubscriptionScreen
import com.cloud.sync.ui.sync.SyncScreen


@Composable
fun AppNavigation() {
    val navController = rememberNavController()


    NavHost(
        navController = navController,
        startDestination = if (BuildConfig.DEBUG) "login" else "scan"
    ) {

        composable("scan") {
            ScanScreen(onNavigateToResult = { scannedContent ->
                navController.navigate("sync/$scannedContent")
            })
        }

        composable("auth") {
            AuthScreen(onAuthenticationSuccess = {
                // When authentication is successful, navigate to the SyncScreen
                navController.navigate("sync") {
                    // This is important! It clears the navigation stack so the user
                    // can't press the back button to go back to the AuthScreen.
                    popUpTo("auth") {
                        inclusive = true
                    }
                }
            })
        }
        composable(
            route = "sync",
//            deepLinks = listOf(navDeepLink { uriPattern = "test://debug/sync-screen/{content}" }),
        ) { backStackEntry ->
            SyncScreen(
                onNavigateToProfile = {
                    navController.navigate("profile")
                }
            )
        }

        composable("login") {
            LoginScreen(onLoginSuccess = {
                Log.w("auth", "onLoginSuccess() called. Navigating to SyncScreen...")
                navController.navigate("sync") {
                    popUpTo("login") {
                        inclusive = true
                    }
                }
            })
        }
        composable("profile") {
            ProfileScreen(
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) // Clear the entire back stack
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSubscription = {
                    navController.navigate("subscription")
                }
            )
        }

        composable("subscription") {
            SubscriptionScreen()
        }
    }
}