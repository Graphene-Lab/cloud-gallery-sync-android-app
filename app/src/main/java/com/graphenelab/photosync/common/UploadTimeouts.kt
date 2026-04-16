package com.graphenelab.photosync.common

object UploadTimeouts {
    private const val BYTES_PER_MB = 1024 * 1024
    private const val FILE_UPLOAD_BASE_TIMEOUT_MS = 180_000L
    private const val FILE_UPLOAD_TIMEOUT_PER_MB_MS = 10_000L
    private const val FILE_UPLOAD_MAX_TIMEOUT_MS = 15 * 60_000L

    fun forSizeBytes(sizeBytes: Int): Long {
        val sizeMb = (sizeBytes.toLong() + BYTES_PER_MB - 1) / BYTES_PER_MB
        return (FILE_UPLOAD_BASE_TIMEOUT_MS + sizeMb * FILE_UPLOAD_TIMEOUT_PER_MB_MS)
            .coerceAtMost(FILE_UPLOAD_MAX_TIMEOUT_MS)
    }
}