package com.graphenelab.communication.crypto;

import static com.graphenelab.communication.crypto.RSAKeyManager.exportCryptoKey;
import static com.graphenelab.communication.crypto.RSAKeyManager.generateKeyPair;

import java.util.concurrent.CompletableFuture;

public class SessionManager {

    private static Session currentSession = new Session();
    private static boolean sessionLoaded = false;

    public static Session getCurrentSession() {
        // Try to load from persistence if available and not loaded yet
        if (!sessionLoaded && RequestManager.persistenceCallback != null) {
            tryLoadSession();
        }
        return currentSession;
    }

    public static void tryLoadSession() {
        try {
            RequestManager.persistenceCallback.loadSession()
                    .thenAccept(loadedSession -> {
                        if (loadedSession != null && loadedSession.getClientId() != null) {
                            // Only replace current session if we got a valid one
                            currentSession = loadedSession;
                            System.out.println("Loaded saved session with clientId: " + loadedSession.getClientId());
                            // Mark as loaded to prevent infinite recursion
                            sessionLoaded = true;
                        }
                    }).exceptionally(ex -> {
                        System.err.println("Error loading session: " + ex.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            System.err.println("Exception while trying to load session: " + e.getMessage());
            // Continue with current session on error
        }
    }

    public static CompletableFuture<Void> resetSession(Object id) {
        currentSession = new Session(id);
        sessionLoaded = true; // Mark as loaded since we just created a new session

        return generateKeyPair()
                .thenCompose(keyPair -> {
                    currentSession.setPublicKey(keyPair.getPublic());
                    currentSession.setPrivateKey(keyPair.getPrivate());
                    return exportCryptoKey(currentSession.getPublicKey());
                })
                .thenAccept(pair -> {
                    currentSession.setPublicKeyB64(pair.getFirst());
                    currentSession.setClientId(pair.getSecond());
                });
    }

    public static CompletableFuture<Void> resetSession() {
        return resetSession(0);
    }

    public static void clearSession() {
        currentSession = new Session();
        sessionLoaded = true;
    }
}
