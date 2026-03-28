package com.cloud.sync.manager

import com.cloud.communication.cryto.FileUploader
import com.cloud.communication.cryto.PairingAuthCallback
import com.cloud.communication.cryto.QrCodeHandler
import com.cloud.communication.cryto.RequestManager
import com.cloud.sync.manager.interfaces.ICloudManager
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout

class DataCenterCloudManager @Inject constructor() : ICloudManager {
    companion object {
        private const val PAIRING_AUTH_TIMEOUT_MS = 30_000L
    }

    override suspend fun pair(
        encryptedQrCode: String,
        pin: Int,
        zeroKnowledgeChecksum: ByteArray?
    ): Result<Unit> {
        return try {
            withTimeout(PAIRING_AUTH_TIMEOUT_MS) {
                suspendCancellableCoroutine { continuation ->
                    val completed = AtomicBoolean(false)

                    val callback = object : PairingAuthCallback {
                        override fun onAuthenticationSuccess() {
                            if (completed.compareAndSet(false, true) && continuation.isActive) {
                                continuation.resume(Result.success(Unit))
                            }
                        }

                        override fun onAuthenticationError(message: String) {
                            if (completed.compareAndSet(false, true) && continuation.isActive) {
                                val resolvedMessage = message.ifBlank {
                                    "Authentication failed. Please recheck your credentials."
                                }
                                continuation.resume(
                                    Result.failure(IllegalStateException(resolvedMessage))
                                )
                            }
                        }
                    }

                    RequestManager.setPairingAuthCallback(callback)

                    continuation.invokeOnCancellation {
                        if (completed.compareAndSet(false, true)) {
                            RequestManager.clearPairingAuthCallback()
                        }
                    }

                    QrCodeHandler.onQrCodeAcquired(encryptedQrCode, pin, zeroKnowledgeChecksum)
                        .exceptionally { throwable ->
                            if (completed.compareAndSet(false, true) && continuation.isActive) {
                                RequestManager.clearPairingAuthCallback()
                                val errorMessage = throwable?.message
                                    ?: "Failed to start pairing. Please try again."
                                continuation.resume(
                                    Result.failure(IllegalStateException(errorMessage, throwable))
                                )
                            }
                            null
                        }
                }
            }
        } catch (timeout: TimeoutCancellationException) {
            RequestManager.clearPairingAuthCallback()
            Result.failure(
                IllegalStateException(
                    "Pairing timed out. Please recheck credentials and try again.",
                    timeout
                )
            )
        } catch (e: Exception) {
            RequestManager.clearPairingAuthCallback()
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

    override fun uploadFileBytes(
        fileBytes: ByteArray,
        fileName: String,
        unixLastWriteTimestampSeconds: Long,
        onProgressUpdate: (FileUploader.ChunkProgress) -> Unit
    ) {
        FileUploader.startSendFileWithProgressCallback(
            fileBytes,
            fileName,
            unixLastWriteTimestampSeconds,
            onProgressUpdate
        )
    }
}
