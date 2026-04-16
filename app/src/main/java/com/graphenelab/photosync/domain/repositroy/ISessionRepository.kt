package com.graphenelab.photosync.domain.repositroy

import com.graphenelab.communication.crypto.Session
import com.graphenelab.photosync.domain.model.CloudSpaceCredentials
import com.graphenelab.photosync.domain.model.SessionDataModel

interface ISessionRepository {
    suspend fun saveSession(session: SessionDataModel): Int
    suspend fun saveCommunicationSession(session: Session): Int
    suspend fun hasSession(): Boolean
    suspend fun loadSession(): SessionDataModel?
    suspend fun loadCommunicationSession(): Session?
    suspend fun saveCloudSpaceCredentials(credentials: CloudSpaceCredentials): Int
    suspend fun hasCloudSpaceCredentials(): Boolean
    suspend fun loadCloudSpaceCredentials(): CloudSpaceCredentials?
    fun clearAuthState()
}
