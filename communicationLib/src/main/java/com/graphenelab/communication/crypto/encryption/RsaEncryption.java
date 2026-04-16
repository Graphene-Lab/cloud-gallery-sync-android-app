package com.graphenelab.communication.crypto.encryption;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.RSAPublicKeySpec;

public class RsaEncryption {


    //importRsaPublicKey
    public static PublicKey createRsaPublicKey(byte[] modulus, byte[] exponent) {
        try {
            var keyFactory = KeyFactory.getInstance("RSA");
            var modBigInt = new BigInteger(1, modulus);
            var expBigInt = new BigInteger(1, exponent);
            var keySpec = new RSAPublicKeySpec(modBigInt, expBigInt);
            return keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to import RSA public key", e);
        }
    }



    //encryptRsa
    public static byte[] encryptData(PublicKey publicKey, byte[] data) throws Exception {
        int blockSize = 190; // Must match RSA key size minus padding (e.g., 2048-bit key => 256 bytes - padding)

        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
        // Configure OAEP to match JavaScript Web Crypto defaults
        OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                "SHA-256",                          // Main hash algorithm
                "MGF1",                             // Mask generation function
                new MGF1ParameterSpec("SHA-256"),   // MGF1 hash (this is the key!)
                PSource.PSpecified.DEFAULT          // No label
        );
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepParams);
        return getBytes(data, blockSize, cipher);
    }


    //decryptRsa
    public static byte[] decryptData(PrivateKey privateKey, byte[] ciphertext) throws Exception {
        int blockSize = 256; // For 2048-bit RSA, encrypted blocks are 256 bytes (matches JS)

        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
        // Configure OAEP to match JavaScript Web Crypto defaults
        OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                "SHA-256",                          // Main hash algorithm
                "MGF1",                             // Mask generation function
                new MGF1ParameterSpec("SHA-256"),   // MGF1 hash (must match JS default)
                PSource.PSpecified.DEFAULT          // No label
        );
        cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepParams);
        return getBytes(ciphertext, blockSize, cipher);
    }

    private static byte[] getBytes(byte[] data, int blockSize, Cipher cipher) throws IllegalBlockSizeException, BadPaddingException, IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        int offset = 0;
        while (offset < data.length) {
            int len = Math.min(blockSize, data.length - offset);
            byte[] chunk = cipher.doFinal(data, offset, len);
            outputStream.write(chunk);
            offset += len;
        }

        return outputStream.toByteArray();
    }
}
