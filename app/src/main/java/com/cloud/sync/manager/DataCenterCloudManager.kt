package com.cloud.sync.manager

import com.cloud.communication.cryto.FileUploader
import com.cloud.communication.cryto.QrCodeHandler
import com.cloud.sync.manager.interfaces.ICloudManager
import java.io.File
import javax.inject.Inject

class DataCenterCloudManager @Inject constructor() : ICloudManager {
    override suspend fun pair(encryptedQrCode: String, pin: Int): Result<Unit> {
        return try {
            QrCodeHandler.onQrCodeAcquired(encryptedQrCode, pin)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadFile(file: File): Result<Unit> {
        return try {
            FileUploader.startSendFileAsync(file)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}