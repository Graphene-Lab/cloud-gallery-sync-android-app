package com.cloud.sync.manager.interfaces

import java.io.File

interface ICloudManager {
    suspend fun pair(encryptedQrCode: String, pin: Int): Result<Unit>
    suspend fun uploadFile(file: File): Result<Unit>
}
