package com.graphenelab.communication.crypto;

import java.util.concurrent.CompletableFuture;

public interface SessionPersistenceCallback {
    void saveSession(Session session);
    void onSaveError(String error);
    CompletableFuture<Session> loadSession();
}