package com.cloud.sync.manager

import com.cloud.communication.cryto.FileUploader
import com.cloud.communication.cryto.QrCodeHandler
import com.cloud.sync.manager.interfaces.ICloudManager
import java.io.InputStream
import javax.inject.Inject

class DataCenterCloudManager @Inject constructor() : ICloudManager {
    override suspend fun pair(
        encryptedQrCode: String,
        pin: Int,
        zeroKnowledgeChecksum: ByteArray?
    ): Result<Unit> {
        return try {
            QrCodeHandler.onQrCodeAcquired(encryptedQrCode, pin, zeroKnowledgeChecksum)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun uploadFile(
        inputStream: InputStream,
        fileName: String,
        unixLastWriteTimestampSeconds: Long,
        onProgressUpdate: (FileUploader.ChunkProgress) -> Unit
    ) {
        FileUploader.startSendFileWithProgressCallback(
            inputStream,
            fileName,
            unixLastWriteTimestampSeconds,
            onProgressUpdate
        )
    }
}