package com.cloud.sync

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cloud.sync.ui.auth.AuthScreen
import com.cloud.sync.ui.login.LoginScreen
import com.cloud.sync.ui.profile.ProfileScreen
import com.cloud.sync.ui.mnemonic.MnemonicScreen
import com.cloud.sync.ui.subscription.SubscriptionScreen
import com.cloud.sync.ui.sync.SyncScreen
import com.cloud.sync.ui.scan.ScanScreen


@Composable
fun AppNavigation() {
    val navController = rememberNavController()


    NavHost(
        navController = navController,
//        startDestination = if (BuildConfig.DEBUG) "mnemonic" else "login"
        startDestination = "login"
    ) {

        composable("auth") {
            val qrEncrypted = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<String>("qr_encrypted")
            AuthScreen(
                qrEncrypted = qrEncrypted,
                onAuthenticationSuccess = {
                    // CSE setup is complete, go directly to sync
                    navController.navigate("sync") {
                        popUpTo("auth") {
                            inclusive = true
                        }
                    }
                },
                onAuthenticationSuccessWithoutCse = {
                    // Need to set up CSE, go to mnemonic screen first
                    navController.navigate("mnemonic") {
                        popUpTo("auth") {
                            inclusive = true
                        }
                    }
                }
            )
        }
        composable("scan") {
            ScanScreen(
                onNavigateToResult = { qrCode ->
                    navController.currentBackStackEntry?.savedStateHandle?.set("qr_encrypted", qrCode)
                    navController.navigate("auth")
                }
            )
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
            LoginScreen(
                onLoginAndCseKeyGenerated = {
                    Log.w("auth", "onLoginSuccess() called. Navigating to SyncScreen...")
                    navController.navigate("sync") {
                        popUpTo("login") {
                            inclusive = true
                        }
                    }
                },
                onLoginSuccess = {
                    navController.navigate("mnemonic") {
                        Log.w(
                            "auth",
                            "onLoginAndCseKeyGenerated() called. Navigating to MnemonicScreen..."
                        )

                        popUpTo("login") {}
                    }
                },
                onNavigateToScan = {
                    navController.navigate("scan")
                }
            )
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

        composable("mnemonic") {
            MnemonicScreen(onMnemonicConfirmed = {
                navController.navigate("sync") {
                    popUpTo("mnemonic") {
                        inclusive = true
                    }
                }
            })
        }
    }
}