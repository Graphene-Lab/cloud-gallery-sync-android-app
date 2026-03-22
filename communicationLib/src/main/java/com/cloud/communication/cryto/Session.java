package com.cloud.communication.cryto;

import javax.crypto.SecretKey;
import java.security.PrivateKey;
import java.security.PublicKey;

public class Session {
    private Object id;
    private String entryPoint;
    private String clientId;
    private String publicKeyB64;
    private String encryptionType;
    private byte[] deviceKey;
    private byte[] IV;
    private byte[] QRkey;
    private String serverId;
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private SecretKey symmetricKey;

    private int pin;
    private byte[] zeroKnowledgeChecksum;

    public Session() {
        this(null);
    }

    public Session(Object id) {
        this.id = id;
    }

    // Getters and Setters

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }

    public String getEntryPoint() {
        return entryPoint;
    }

    public void setEntryPoint(String entryPoint) {
        this.entryPoint = entryPoint;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getPublicKeyB64() {
        return publicKeyB64;
    }

    public void setPublicKeyB64(String publicKeyB64) {
        this.publicKeyB64 = publicKeyB64;
    }

    public String getEncryptionType() {
        return encryptionType;
    }

    public void setEncryptionType(String encryptionType) {
        this.encryptionType = encryptionType;
    }

    public byte[] getDeviceKey() {
        return deviceKey;
    }

    public void setDeviceKey(byte[] deviceKey) {
        this.deviceKey = deviceKey;
    }

    public byte[] getIV() {
        return IV;
    }

    public void setIV(byte[] IV) {
        this.IV = IV;
    }

    public byte[] getQRkey() {
        return QRkey;
    }

    public void setQRkey(byte[] QRkey) {
        this.QRkey = QRkey;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public SecretKey getSymmetricKey() {
        return symmetricKey;
    }

    public void setSymmetricKey(SecretKey symmetricKey) {
        this.symmetricKey = symmetricKey;
    }

    public int getPin() {
        return pin;
    }
    public void setPin(int pin) {
        this.pin = pin;
    }

    public byte[] getZeroKnowledgeChecksum() {
        return zeroKnowledgeChecksum;
    }

    public void setZeroKnowledgeChecksum(byte[] zeroKnowledgeChecksum) {
        this.zeroKnowledgeChecksum = zeroKnowledgeChecksum;
    }
}
