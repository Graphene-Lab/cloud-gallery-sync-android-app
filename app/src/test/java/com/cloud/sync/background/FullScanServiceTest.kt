package com.cloud.sync.background

import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import com.cloud.sync.common.SyncStatusManager
import com.cloud.sync.domain.model.TimeInterval
import com.cloud.sync.manager.interfaces.IFullScanProcessManager
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("FullScanService Unit Tests")
class FullScanServiceTest {

    private lateinit var fullScanProcessor: IFullScanProcessManager
    // Mock for the Android NotificationManager to verify that notifications are sent.
    private lateinit var notificationManager: NotificationManager
    // The actual Service class under test,
    private lateinit var service: FullScanService

    // Test dispatcher that gives us full control over the execution of coroutines.
    private val testDispatcher = StandardTestDispatcher()
    // Testing scope that uses the [testDispatcher] and provides structured concurrency for tests.
    private val testScope = TestScope(testDispatcher)

    // helper StateFlows to simulate the updates from the real SyncStatusManager.
    private val isSyncingFlow = MutableStateFlow(false)
    private val successfulSyncPhotosCountFlow = MutableStateFlow(0)
    private val failedSyncPhotosCountFlow = MutableStateFlow(0)
    private val discoveredPhotosCountFlow = MutableStateFlow(0)

    @BeforeEach
    fun setup() {
        // --- Initialize Mocks ---
        fullScanProcessor = mock()
        notificationManager = mock()

        // --- Mock Singleton/Object Dependencies ---
        // Using MockK to mock the 'SyncStatusManager' object, as Mockito cannot mock Kotlin objects.
        mockkObject(SyncStatusManager)
        every { SyncStatusManager.isSyncing } returns isSyncingFlow
        every { SyncStatusManager.successfulSyncPhotosCount } returns successfulSyncPhotosCountFlow
        every { SyncStatusManager.failedSyncPhotosCount } returns failedSyncPhotosCountFlow
        every { SyncStatusManager.discoveredPhotosCount } returns discoveredPhotosCountFlow
        
        // Mock the update methods
        every { SyncStatusManager.updateSyncStatus(any()) } answers {
            isSyncingFlow.value = firstArg()
        }
        every { SyncStatusManager.updateSuccessfulSyncPhotosCount() } answers {
            successfulSyncPhotosCountFlow.value += 1
        }
        every { SyncStatusManager.updateFailedSyncPhotosCount(any()) } answers {
            failedSyncPhotosCountFlow.value += firstArg<Int>()
        }
        every { SyncStatusManager.increaseDiscoveredPhotosCount(any()) } answers {
            discoveredPhotosCountFlow.value += firstArg<Int>()
        }


        // --- Prepare the Service Under Test ---
        // Using spy() to create an instance of the real service that we can partially mock.
        service = spy(FullScanService())

        // --- Manual Dependency Injection ---
        // Since Hilt is not running in this unit test, we manually inject our mocks.
        service.fullScanProcessor = fullScanProcessor
        // Use reflection to inject mocks into private fields of the service instance.
        val nmField = FullScanService::class.java.getDeclaredField("notificationManager")
        nmField.isAccessible = true
        nmField.set(service, notificationManager)
        val scopeField = FullScanService::class.java.getDeclaredField("serviceScope")
        scopeField.isAccessible = true
        scopeField.set(service, testScope)

        // --- Stub Android Framework Methods ---
        // The following stubs are crucial for running this test on the JVM. They prevent calls to
        // the real Android framework code, which would crash the test.

        // Prevent crashes from NotificationCompat.Builder by intercepting the call.
        val mockNotification: Notification = mock()
        doReturn(mockNotification).`when`(service).createNotification(any())

        // Prevent crashes from base Service class methods.
        doNothing().`when`(service).startForeground(any(), any())
        doNothing().`when`(service).stopForeground(any<Int>())

        // The solution for UncompletedCoroutinesError:
        // When stopSelf() is called, we must model the service's lifecycle correctly.
        // Instead of cancelling the entire TestScope (which causes race conditions), we only
        // cancel its child coroutines. This cleanly stops the long-running 'progress.collect'
        // coroutine without destroying the test runner itself.
        doAnswer {
            testScope.coroutineContext.cancelChildren()
        }.`when`(service).stopSelf()
    }

    @AfterEach
    fun tearDown() {
        // Clean up mocks and scopes to ensure test isolation.
        unmockkObject(SyncStatusManager)
        if (testScope.isActive) {
            testScope.cancel()
        }
    }

    @Nested
    @DisplayName("Intent Handling")
    inner class IntentHandlingTests {

        @Test
        @DisplayName("onStartCommand with ACTION_START should do nothing if already syncing")
        fun onStartCommand_startActionWhenSyncing_doesNothing() = testScope.runTest {
            // Arrange
            val startIntent: Intent = mock()
            whenever(startIntent.action).thenReturn(FullScanService.ACTION_START)
            isSyncingFlow.value = true // Key condition for this test

            // Act
            service.onStartCommand(startIntent, 0, 1)
            advanceUntilIdle()

            // Assert
            verify(fullScanProcessor, never()).initializeIntervals()
        }

        @Test
        @DisplayName("onStartCommand with ACTION_START should start logic if not syncing")
        fun onStartCommand_startAction_startsLogic() = testScope.runTest {
            // Arrange
            val startIntent: Intent = mock()
            whenever(startIntent.action).thenReturn(FullScanService.ACTION_START)
            whenever(fullScanProcessor.initializeIntervals()).thenReturn(mutableListOf())

            // Act
            service.onStartCommand(startIntent, 0, 1)
            advanceUntilIdle()

            // Assert
            verify(fullScanProcessor).initializeIntervals()
            verify(service, atLeastOnce()).startForeground(any(), any())
        }

        @Test
        @DisplayName("onStartCommand with ACTION_STOP should stop the sync")
        fun onStartCommand_stopAction_stopsSync() = testScope.runTest {
            // Arrange
            val stopIntent: Intent = mock()
            whenever(stopIntent.action).thenReturn(FullScanService.ACTION_STOP)

            // Act
            // The spy will call the real onStartCommand, which internally calls the private stopSync().
            service.onStartCommand(stopIntent, 0, 1)
            advanceUntilIdle()

            // Assert
            // We verify the public effects of the private stopSync() method.
            verify(service).stopSelf()
            verify(service).stopForeground(any<Int>())
            io.mockk.verify { SyncStatusManager.updateSyncStatus(false) }
        }
    }

    @Nested
    @DisplayName("Full Scan Execution Logic")
    inner class FullScanExecutionTests {

        @Test
        @DisplayName("should run full scan process correctly and complete")
        fun startFullScan_happyPath_completesSuccessfully() = testScope.runTest {
            // Arrange
            val startIntent: Intent = mock()
            whenever(startIntent.action).thenReturn(FullScanService.ACTION_START)
            val initialIntervals = mutableListOf(TimeInterval(0, 100), TimeInterval(150, 200))
            val afterMerge = mutableListOf(TimeInterval(0, 200))
            whenever(fullScanProcessor.initializeIntervals()).thenReturn(initialIntervals)
            whenever(fullScanProcessor.processNextTwoIntervals(any(), any())).thenReturn(afterMerge)
            whenever(fullScanProcessor.processTailEnd(any(), any())).thenReturn(afterMerge)

            // Act
            service.onStartCommand(startIntent, 0, 1)
            advanceUntilIdle()

            // Assert
            // Use inOrder to verify the sequence of processing steps is correct.
            inOrder(fullScanProcessor) {
                verify(fullScanProcessor).initializeIntervals()
                // Use eq() for raw values when mixing with other matchers like any().
                verify(fullScanProcessor).processNextTwoIntervals(eq(initialIntervals), any())
                verify(fullScanProcessor).processTailEnd(eq(afterMerge), any())
            }
            // Verify that the service correctly shuts itself down upon completion.
            verify(service).stopSelf()
        }

        @Test
        @DisplayName("should handle exceptions from processor and stop")
        fun startFullScan_processorThrowsException_updatesStatusAndStops() = testScope.runTest {
            // Arrange
            val startIntent: Intent = mock()
            whenever(startIntent.action).thenReturn(FullScanService.ACTION_START)
            val exception = IOException("DB Error")
            // Use doAnswer to throw a checked exception from a suspend function.
            doAnswer { throw exception }.`when`(fullScanProcessor).initializeIntervals()

            // Act
            service.onStartCommand(startIntent, 0, 1)
            advanceUntilIdle()

            // Assert
            io.mockk.verify { SyncStatusManager.updateSyncStatus(true) }
            io.mockk.verify { SyncStatusManager.updateSyncStatus(false) }
            verify(service).stopSelf()
        }
    }

    @Nested
    @DisplayName("Notification Updates")
    inner class NotificationUpdateTests {

        @Test
        @DisplayName("should update notification when progress flow emits")
        fun onProgressUpdate_shouldUpdateNotification() = testScope.runTest {
            // Arrange
            val startIntent: Intent = mock()
            whenever(startIntent.action).thenReturn(FullScanService.ACTION_START)
            // Return an empty list to prevent the scan logic from completing immediately.
            whenever(fullScanProcessor.initializeIntervals()).thenReturn(mutableListOf())

            // Act
            service.onStartCommand(startIntent, 0, 1)
            advanceUntilIdle() // Ensure the collector coroutine is running.
            // Simulate a status update.
            successfulSyncPhotosCountFlow.value = 50
            advanceUntilIdle() // Allow the collector to process the update.

            // Assert
            // Verify that the notification manager was told to display a notification.
            val notificationCaptor = argumentCaptor<Notification>()
            verify(notificationManager, atLeastOnce()).notify(any(), notificationCaptor.capture())
            // Verify the service was kept in the foreground.
            verify(service, atLeastOnce()).startForeground(any(), any())
        }
    }
}