package com.graphenelab.photosync.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CloudSpaceCredentials(
    val qrEncrypted: String,
    val pin: Int = 1234 // Default pin for now
)