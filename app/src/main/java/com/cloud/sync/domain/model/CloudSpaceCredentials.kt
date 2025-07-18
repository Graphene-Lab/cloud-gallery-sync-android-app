package com.cloud.sync.domain.model

data class CloudSpaceCredentials(
    val qrEncrypted: String,
    val pin: Int = 1234 // Default pin for now
)