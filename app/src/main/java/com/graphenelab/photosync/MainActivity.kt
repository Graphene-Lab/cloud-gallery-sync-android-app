package com.graphenelab.photosync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.graphenelab.photosync.common.AppStartupTrace
import com.graphenelab.photosync.ui.startup.StartupViewModel
import com.graphenelab.photosync.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val startupViewModel: StartupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        AppStartupTrace.mark("MainActivity.onCreate:start")
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        val startupVm = startupViewModel
        AppStartupTrace.mark("MainActivity.startupViewModelReady")
        splashScreen.setKeepOnScreenCondition {
            startupVm.uiState.value.keepSplashVisible
        }
        enableEdgeToEdge()
        AppStartupTrace.mark("MainActivity.enableEdgeToEdge")
        setContent {
            val startupState by startupVm.uiState.collectAsState()

            AppTheme(dynamicColor = false) {
                val startDestination = startupState.startDestination
                if (startDestination != null) {
                    AppNavigation(
                        startDestination = startDestination,
                        onInitialDestinationDisplayed = startupVm::onInitialDestinationDisplayed
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
        AppStartupTrace.mark("MainActivity.setContent:end")
    }
}
