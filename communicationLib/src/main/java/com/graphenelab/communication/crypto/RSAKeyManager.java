package com.graphenelab.communication.crypto;

import static com.graphenelab.communication.crypto.ConversionUtils.base64UrlDecode;
import static com.graphenelab.communication.crypto.ConversionUtils.base64UrlEncode;
import static com.graphenelab.communication.crypto.ConversionUtils.byteArrayToHex;
import static com.graphenelab.communication.crypto.CryptoUtils.stripLeadingZero;
import static com.graphenelab.communication.crypto.HashUtils.hash256;

import kotlin.Pair;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RSAKeyManager {

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    // Generate RSA keypair asynchronously
    public static CompletableFuture<KeyPair> generateKeyPair() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                keyGen.initialize(new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4));
                return keyGen.generateKeyPair();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    // Export public key modulus as Base64 (like exportCryptoKey)
    public static CompletableFuture<Pair<String, String>> exportCryptoKey(PublicKey key) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                RSAPublicKey rsaKey = (RSAPublicKey) key;

                byte[] nBytes = stripLeadingZero(rsaKey.getModulus().toByteArray());
                String nBase64Url = base64UrlEncode(nBytes);

                // Decode base64url to bytes (matching JavaScript decodeBase64Url behavior)
                byte[] decodedBytes = base64UrlDecode(nBase64Url);
                String publicKeyB64 = Base64.getEncoder().encodeToString(decodedBytes);

                // Decode base64 to bytes
                byte[] keyBin = Base64.getDecoder().decode(publicKeyB64);

                // Hash first 8 bytes of SHA-256 of keyBin for clientId
                byte[] digest = hash256(keyBin);
                byte[] clientIdBytes = new byte[8];
//                String fullKeyBase64 = Base64.getEncoder().encodeToString(key.getEncoded());
                System.arraycopy(digest, 0, clientIdBytes, 0, 8);

                return  new Pair<>(publicKeyB64, byteArrayToHex(clientIdBytes));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }
}


