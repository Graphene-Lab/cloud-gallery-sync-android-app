package com.cloud.sync

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.cloud.sync.common.AppStartupTrace
import com.cloud.sync.common.CommunicationLibInitializer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()

    @Inject
    lateinit var communicationLibInitializer: CommunicationLibInitializer


    // Manually initialize WorkManager
    override fun onCreate() {
        AppStartupTrace.reset("MyApp.beforeSuperOnCreate")
        super.onCreate()
        AppStartupTrace.mark("MyApp.afterSuperOnCreate")
        // WorkManager.initialize(this, workManagerConfiguration) is now implicitly called
        // because we implement Configuration.Provider. The key is that this now happens
        // AFTER Hilt injection is complete.
        AppStartupTrace.mark("CommunicationLibInitializer.initialize:start")
        communicationLibInitializer.initialize()
        AppStartupTrace.mark("CommunicationLibInitializer.initialize:end")
    }
}
