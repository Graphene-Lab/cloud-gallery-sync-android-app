package com.cloud.communication.cryto;

import java.util.Arrays;
import java.util.List;

import static com.cloud.communication.cryto.ConversionUtils.byteArrayToHex;
import static com.cloud.communication.cryto.ConversionUtils.int32ToBuffer;
import static com.cloud.communication.cryto.ConversionUtils.joinBuffers;
import static com.cloud.communication.cryto.CryptoUtils.alertBox;
import static com.cloud.communication.cryto.HashUtils.hash256;
import static com.cloud.communication.cryto.RequestManager.executeRequest;
import static com.cloud.communication.cryto.encryption.AesEncryption.createKey;

public class Pairing {
    public static void Pair(List<byte[]> params) {
        byte[] clientIdBytes = params.get(0);
        byte[] deviceIV = params.get(2);
        byte[] auth = params.get(3);
        String clientIdHex = byteArrayToHex(clientIdBytes);

        var session = SessionManager.getCurrentSession();
        if (!clientIdHex.equals(session.getClientId())) {
            alertBox("Wrong connection, key verification failed!");
        } else if (session.getDeviceKey() != null) {
            alertBox("Attempt to change the encryption key!");
        } else {
            byte[] deviceKeyBytes = params.get(1);
            if (deviceKeyBytes.length > 0) {
                session.setEncryptionType("aes");
                session.setDeviceKey(deviceKeyBytes);
                try {
                    session.setSymmetricKey(createKey(deviceKeyBytes));
                    session.setIV(deviceIV);
                } catch (Exception e) {
                    e.printStackTrace();
                    // Handle exception
                }
            } else {
                session.setEncryptionType("xorAB");
            }
        }
        // Authentication
        alertBox("Pairing complete. Authenticating with PIN: " + session.getPin() + " ...");
        authenticate(auth);
    }


    public static void authenticate(byte[] auth) {
        var session = SessionManager.getCurrentSession();
        var pin = session.getPin();
        byte[] pin4 = int32ToBuffer(pin);
        byte[] authentication = joinBuffers(auth, pin4);

        byte[] hash = hash256(authentication);
        byte[] verify = Arrays.copyOfRange(hash, 0, 4);
        byte[] zeroKnowledgeChecksum = session.getZeroKnowledgeChecksum();
        byte[] payload = zeroKnowledgeChecksum != null && zeroKnowledgeChecksum.length > 0
                ? joinBuffers(verify, zeroKnowledgeChecksum)
                : verify;
        executeRequest(Command.Authentication.getId(), payload);
    }
}
