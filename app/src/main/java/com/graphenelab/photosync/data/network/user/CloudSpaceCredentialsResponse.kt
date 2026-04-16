package com.graphenelab.photosync.data.network.user

data class CloudSpaceCredentialsResponse(
    val qrEncrypted: String,
    val pin: Int
)