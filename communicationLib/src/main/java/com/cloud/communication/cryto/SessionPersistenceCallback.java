package com.cloud.communication.cryto;

import java.util.concurrent.CompletableFuture;

public interface SessionPersistenceCallback {
    void saveSession(Session session);
    void onSaveError(String error);
    CompletableFuture<Session> loadSession();
}