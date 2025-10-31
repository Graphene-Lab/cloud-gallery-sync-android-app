package com.cloud.sync.common

import android.util.Log
import com.cloud.communication.cryto.RequestManager
import com.cloud.communication.cryto.Session
import com.cloud.communication.cryto.SessionManager
import com.cloud.communication.cryto.SessionPersistenceCallback
import com.cloud.sync.data.local.secure.SessionRepository
import com.cloud.sync.domain.repositroy.ISessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.future.future
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunicationLibInitializer @Inject constructor(
    private val sessionRepository: ISessionRepository
) {

    companion object {
        private const val TAG = "CommunicationLibInitializer"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun initialize() {
        // Set up the callback for future session operations
        RequestManager.setSessionPersistenceCallback(object : SessionPersistenceCallback {
            override fun saveSession(session: Session) {
                scope.launch {
                    try {
                        sessionRepository.saveCommunicationSession(session)
                        Log.d(TAG, "Session saved successfully")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to save session", e)
                        onSaveError(e.message ?: "Unknown error")
                    }
                }
            }

            override fun onSaveError(error: String) {
                Log.e(TAG, "Session save error: $error")
            }

            override fun loadSession(): CompletableFuture<Session> {
                return scope.future {
                    try {
                        val session = sessionRepository.loadCommunicationSession()
                        if (session != null) {
                            Log.d(TAG, "Session loaded successfully")
                            session
                        } else {

                            Log.d(TAG, "No saved session found, creating new session")
                            val newSession = Session()
                            // Optionally initialize with default values
                            newSession
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load session", e)
                        Session() // Return empty session on error
                    }
                }
            }
        })
        //important! before requests are made we need to load session data.
        SessionManager.tryLoadSession()
    }

}