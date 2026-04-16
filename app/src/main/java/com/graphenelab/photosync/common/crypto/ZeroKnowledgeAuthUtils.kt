package com.graphenelab.photosync.common.crypto

import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object ZeroKnowledgeAuthUtils {
    private const val HMAC_ALGORITHM = "HmacSHA512"
    private const val BITCOIN_SEED = "Bitcoin seed"

    fun cleanPassphrase(passphrase: String): String {
        return passphrase
            .replace("\n", " ")
            .replace("\r", " ")
            .replace(",", "")
            .replace("!", "")
            .replace(".", "")
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    fun deriveMasterKey(passphrase: String): ByteArray {
        val cleanedPassphrase = cleanPassphrase(passphrase)
        require(cleanedPassphrase.isNotBlank()) {
            "Enter your zero-knowledge passphrase."
        }

        val mnemonicCode = Mnemonics.MnemonicCode(cleanedPassphrase)
        val seed = mnemonicCode.toSeed()
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        val bitcoinSeedKey = SecretKeySpec(
            BITCOIN_SEED.toByteArray(StandardCharsets.UTF_8),
            HMAC_ALGORITHM
        )
        mac.init(bitcoinSeedKey)

        return mac.doFinal(seed).copyOfRange(0, 32)
    }

    fun deriveAuthenticationChecksum(masterKey: ByteArray): ByteArray {
        val filenameObfuscationKey = sha256(masterKey)
        return sha256(filenameObfuscationKey).copyOfRange(0, 4)
    }

    private fun sha256(data: ByteArray): ByteArray {
        return MessageDigest.getInstance("SHA-256").digest(data)
    }
}