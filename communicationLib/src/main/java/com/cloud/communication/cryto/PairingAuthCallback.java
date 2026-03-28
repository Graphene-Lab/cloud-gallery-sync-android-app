package com.cloud.communication.cryto;

public interface PairingAuthCallback {
    void onAuthenticationSuccess();

    void onAuthenticationError(String message);
}
