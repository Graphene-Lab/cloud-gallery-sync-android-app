package com.graphenelab.photosync.common

import android.os.SystemClock
import android.util.Log

object AppStartupTrace {
    private const val TAG = "StartupTrace"

    @Volatile
    private var startTimeMs: Long = 0L

    fun reset(origin: String) {
        startTimeMs = SystemClock.elapsedRealtime()
        Log.d(TAG, "$origin at +0ms")
    }

    fun mark(stage: String) {
        val now = SystemClock.elapsedRealtime()
        if (startTimeMs == 0L) {
            startTimeMs = now
        }
        Log.d(TAG, "$stage at +${now - startTimeMs}ms")
    }
}
