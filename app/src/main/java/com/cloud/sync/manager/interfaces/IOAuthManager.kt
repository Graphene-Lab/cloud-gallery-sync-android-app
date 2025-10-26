package com.cloud.sync.manager.interfaces

import android.content.Intent

interface IOAuthManager {
    fun getAuthIntent(): Intent
    suspend fun exchangeCodeForToken(intent: Intent)
}