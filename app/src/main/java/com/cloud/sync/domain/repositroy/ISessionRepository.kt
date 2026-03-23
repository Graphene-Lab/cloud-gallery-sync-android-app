package com.cloud.sync.domain.repositroy

import com.cloud.communication.cryto.Session
import com.cloud.sync.domain.model.CloudSpaceCredentials
import com.cloud.sync.domain.model.SessionDataModel

interface ISessionRepository {
    suspend fun saveSession(session: SessionDataModel): Int
    suspend fun saveCommunicationSession(session: Session): Int
    suspend fun loadSession(): SessionDataModel?
    suspend fun loadCommunicationSession(): Session?
    suspend fun saveCloudSpaceCredentials(credentials: CloudSpaceCredentials): Int
    suspend fun loadCloudSpaceCredentials(): CloudSpaceCredentials?
    fun clearAuthState()
}
