package com.cloud.sync.manager.interfaces

import com.cloud.communication.cryto.FileUploader
import java.io.InputStream

interface ICloudManager {
    suspend fun pair(
        encryptedQrCode: String,
        pin: Int,
        zeroKnowledgeChecksum: ByteArray? = null
    ): Result<Unit>

    fun uploadFile(
        inputStream: InputStream,
        fileName: String,
        onProgressUpdate: (FileUploader.ChunkProgress) -> Unit
    )
}
