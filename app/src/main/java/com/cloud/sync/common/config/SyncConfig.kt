package com.cloud.sync.common.config

data class SyncConfig(
    val batchSize: Int = 10,
    val isEncryptionEnabled: Boolean = true,
    val photoFolderPath: String = "Photos/"
)