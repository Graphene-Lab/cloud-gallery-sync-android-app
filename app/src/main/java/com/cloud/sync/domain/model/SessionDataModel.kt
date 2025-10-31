package com.cloud.sync.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SessionDataModel(
    val clientId: String,
    val serverId: String? = null,
    val deviceKey: ByteArray? = null,
    val encryptionType: String? = null,
    val symmetricKey: ByteArray? = null,
    val iv: ByteArray? = null,
    val pin: Int? = null,
    val privateKey: ByteArray? = null,
    val qrKey: ByteArray? = null,
    val publicKeyB64: String? = null
)