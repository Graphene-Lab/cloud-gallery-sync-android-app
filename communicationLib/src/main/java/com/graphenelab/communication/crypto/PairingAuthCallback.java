package com.graphenelab.communication.crypto;

public interface PairingAuthCallback {
    void onAuthenticationSuccess();

    void onAuthenticationError(String message);
}
