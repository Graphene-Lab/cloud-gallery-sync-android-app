package com.cloud.sync.background

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.cloud.communication.cryto.RequestManager
import com.cloud.sync.common.SyncStatusManager
import com.cloud.sync.manager.interfaces.IFullScanProcessManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject
import kotlin.coroutines.coroutineContext // Required for coroutineContext extension property

@AndroidEntryPoint
class FullScanService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject
    lateinit var fullScanProcessor: IFullScanProcessManager

    private lateinit var notificationManager: NotificationManager

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        private const val NOTIFICATION_CHANNEL_ID = "FullScanChannel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        // Create notification channel for ongoing service updates.
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Full Sync Service",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> if (!SyncStatusManager.isSyncing.value) {
                startServiceLogic()
            }
            ACTION_STOP -> stopSync()
            null -> {
                // Service was restarted by system, check if we should continue syncing
                if (SyncStatusManager.isSyncing.value) {
                    startServiceLogic()
                }
            }
        }
        return START_STICKY
    }


    /**
     * Initiates the full scan logic and starts observing processor status updates.
     */
    private fun startServiceLogic() {
        // clear old scan process
        SyncStatusManager.resetSyncSession()

        // Start foreground notification immediately
        val initialNotification = createNotification("Starting full scan...")
        startForeground(NOTIFICATION_ID, initialNotification)

        // Collect status updates from the processor to manage the foreground notification.
        serviceScope.launch {
            SyncStatusManager.successfulSyncPhotosCount.collect { successfulSyncPhotosCount ->
                updateForegroundNotification("Synced: $successfulSyncPhotosCount photos")
            }
        }
        // Launch the core full scan operation in a separate coroutine.
        serviceScope.launch {
            startFullScanLogicInternal()
        }

        serviceScope.launch {
            SyncStatusManager.isSyncing.collect { it ->
                if (!it) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    SyncStatusManager.updateSyncStatus(false)
                }
            }
        }
    }

    /**
     * Stops the full scan, cancels ongoing coroutines, and removes the foreground notification.
     */
    private fun stopSync() {
        RequestManager.cancelAllPendingRequests()
        serviceScope.coroutineContext.cancelChildren()
        stopForeground(STOP_FOREGROUND_REMOVE)
        SyncStatusManager.updateSyncStatus(false)
        stopSelf()
    }

    /**
     * Executes the main full scan and synchronization logic.
     * This function delegates core operations to [fullScanProcessor].
     */
    private suspend fun startFullScanLogicInternal() {
        SyncStatusManager.updateSyncStatus(true)

        try {
            var allIntervals = fullScanProcessor.initializeIntervals()
            if (allIntervals.isEmpty()) {
                SyncStatusManager.updateSyncStatus(false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return
            }
            // Process gaps and merge intervals until less than two remain.
            while (allIntervals.size >= 2) {
                coroutineContext.ensureActive() // Ensure the coroutine is still active for cancellation.
                allIntervals =
                    fullScanProcessor.processNextTwoIntervals(allIntervals, coroutineContext)
            }

            coroutineContext.ensureActive() // Ensure active before processing tail.
            // Process any remaining photos at the end of the timeline.
            fullScanProcessor.processTailEnd(allIntervals, coroutineContext)
            if (SyncStatusManager.discoveredPhotosCount.value == 0) {
                SyncStatusManager.updateNoPhotosFoundToSync(true)
                SyncStatusManager.updateSyncStatus(false)
                return
            }

        } catch (e: Exception) {
            // Re-throw CancellationException to propagate cancellation correctly.
            if (e is CancellationException) throw e
            SyncStatusManager.updateSyncStatus(false)
        } finally {
        }
    }

    /**
     * Updates the persistent foreground notification with the current sync status.
     * @param text The status message to display in the notification.
     */
    private fun updateForegroundNotification(text: String) {
        val notification = createNotification(text)
        notificationManager.notify(NOTIFICATION_ID, notification)
        startForeground(NOTIFICATION_ID, notification) // Keep service in foreground.
    }

    internal fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Gallery Sync")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        notificationManager.cancel(NOTIFICATION_ID) // for reliable notification cancellation
        serviceScope.cancel() // Cancel all coroutines when the service is destroyed.
    }
}