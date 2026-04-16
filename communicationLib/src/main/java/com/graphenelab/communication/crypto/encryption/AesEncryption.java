package com.graphenelab.communication.crypto.encryption;


import com.graphenelab.communication.crypto.SessionManager;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AesEncryption {

    public static SecretKey createKey(byte[] rawKey) {
        return new SecretKeySpec(rawKey, "AES");
    }

    public static byte[] encryptData(byte[] data) throws Exception {
        var session = SessionManager.getCurrentSession();
        byte[] ivBytes = session.getIV();
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, session.getSymmetricKey(), ivSpec);

        return cipher.doFinal(data);
    }

    public static byte[] decryptData(byte[] encryptedData) throws Exception {
        var session = SessionManager.getCurrentSession();
        byte[] ivBytes = session.getIV();
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, session.getSymmetricKey(), ivSpec);

        return cipher.doFinal(encryptedData);
    }
}
