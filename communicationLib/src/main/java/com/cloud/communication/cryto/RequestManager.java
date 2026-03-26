package com.cloud.communication.cryto;

import kotlin.Pair;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.cloud.communication.cryto.Command.getCommandName;
import static com.cloud.communication.cryto.ConversionUtils.base64ToBuffer;
import static com.cloud.communication.cryto.ConversionUtils.int32ToBuffer;
import static com.cloud.communication.cryto.ConversionUtils.joinBuffers;
import static com.cloud.communication.cryto.CryptoUtils.alertBox;
import static com.cloud.communication.cryto.CryptoUtils.splitData;
import static com.cloud.communication.cryto.FileUploader.handleServerUploadResponse;
import static com.cloud.communication.cryto.QrCodeHandler.setClient;
import static com.cloud.communication.cryto.encryption.AesEncryption.decryptData;
import static com.cloud.communication.cryto.encryption.AesEncryption.encryptData;
import static com.cloud.communication.cryto.encryption.RsaEncryption.createRsaPublicKey;
import static com.cloud.communication.cryto.encryption.XorEncryption.decryptXorAB;

import com.cloud.communication.cryto.encryption.RsaEncryption;

public class RequestManager {

    private static final List<Pair<Integer, byte[]>> spooler = new ArrayList<>();
    private static final AtomicInteger concurrentRequest = new AtomicInteger(0);
    private static final int maxConcurrentRequest = 3;
    private static final boolean DEBUG_CONCURRENCY_LOGS = true;
    private static final OkHttpClient client = new OkHttpClient();
    private static ExecutorService executor = Executors.newCachedThreadPool();
    private static final Set<Call> activeCalls = ConcurrentHashMap.newKeySet();//    public static String proxy = "http://195.20.235.5:5050";

    public static String proxy = "http://proxy.tc0.it:5050";

    public static void updateProxyFromEntryPoint(String entryPoint) {
        if (entryPoint == null || entryPoint.trim().isEmpty()) {
            return;
        }
        String trimmed = entryPoint.trim();
        if(trimmed.equals("proxy")){
            trimmed = "proxy.tc0.it";
        }
        boolean hasScheme = trimmed.startsWith("http://") || trimmed.startsWith("https://");
        String normalized = hasScheme ? trimmed : "http://" + trimmed;

        HttpUrl parsed = HttpUrl.parse(normalized);
        if (parsed == null || parsed.host().isEmpty()) {
            return;
        }

        String withoutScheme = hasScheme ? trimmed.substring(trimmed.indexOf("://") + 3) : trimmed;
        String hostPortPart = withoutScheme.split("/", 2)[0];
        boolean portSpecified = hostPortPart.contains(":");

        String scheme = parsed.scheme();
        String host = parsed.host();
        int port = portSpecified ? parsed.port() : 5050;

        proxy = scheme + "://" + host + ":" + port;
        System.out.println("Proxy updated from entry point: " + proxy);
    }

    // Session persistence callback
    public static SessionPersistenceCallback persistenceCallback;

    public static void setSessionPersistenceCallback(SessionPersistenceCallback callback) {
        persistenceCallback = callback;
    }

    public static synchronized void enqueueRequest(Integer commandId, byte[] data) {
        spooler.add(new Pair<>(commandId, data));
        logConcurrencyState("enqueueRequest", commandId);
        tryStartNext();
    }

    private static synchronized void tryStartNext() {
        recreateExecutor();

        if (concurrentRequest.get() < maxConcurrentRequest) {
            Pair<Integer, byte[]> nextRequest = null;
            if (!spooler.isEmpty()) {
                nextRequest = spooler.remove(spooler.size() - 1);
            }
            if (nextRequest != null) {
                concurrentRequest.incrementAndGet();
                logConcurrencyState("dispatch", nextRequest.getFirst());
                executeRequest(nextRequest.getFirst(), nextRequest.getSecond());
            }
        }
    }

    public static void executeRequest(Integer commandId, byte[] data) {
        if (commandId == null) {
            alertBox("Command does not exist");
            requestDone("null-command");
            return;
        }
        byte[] requestData = data != null ? data : new byte[0];
        String proxyUrl = proxy + "/data";
        HttpUrl baseUrl = HttpUrl.parse(proxyUrl);
        if (baseUrl == null) {
            alertBox("Invalid proxy URL");
            requestDone("invalid-proxy-url");
            return;
        }

        var urlBuilder = baseUrl.newBuilder();
        urlBuilder.addQueryParameter("cid", SessionManager.getCurrentSession().getClientId());

        String purpose = null;
        if (commandId == Command.SetClient.getId() || commandId == Command.GetEncryptedQR.getId()) {
            urlBuilder.addQueryParameter("sid", SessionManager.getCurrentSession().getServerId());
            purpose = getCommandName(commandId);
            urlBuilder.addQueryParameter("purpose", purpose);
        }

        HttpUrl url = urlBuilder.build();
        System.out.println("Executing request URL: " + url);
        Request.Builder requestBuilder = new Request.Builder().url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36")
                .header("Accept", "*/*")
                .header("Accept-Language", "en-US,en;q=0.9");

        boolean isGet = commandId == Command.GetPushNotifications.getId();
        if (isGet) {
            getRequest(requestBuilder);
            return;
        }

        if (purpose != null) {
            postRequest(requestBuilder, requestData);
            return;
        }

        if (SessionManager.getCurrentSession().getDeviceKey() == null) {
            alertBox("Unregistered user. You need to log in to the server to initialize the encryption.");
            requestDone("missing-device-key");
            return;
        }

        byte[] cmdBuffer = int32ToBuffer(commandId);
        if (requestData.length == 0) {
            requestData = cmdBuffer;
        } else {
            requestData = joinBuffers(cmdBuffer, requestData);
        }

        byte[] finalRequestData = requestData;
        byte[] encrypted;
        try {
            encrypted = encryptData(finalRequestData);
            postRequest(requestBuilder, encrypted);
        } catch (Exception e) {
            requestDone("execute-exception");
            throw new RuntimeException(e);
        }
    }

    private static void getRequest(Request.Builder requestBuilder) {
        getEnqueue(requestBuilder.build());
    }

    private static void postRequest(Request.Builder requestBuilder, byte[] data) {
        MediaType mediaType = MediaType.parse("application/octet-stream");
        RequestBody body = RequestBody.create(data, mediaType);
        Request request = requestBuilder.post(body).build();
        getEnqueue(request);
    }

    private static void getEnqueue(Request request) {
        var call = client.newCall(request);
        activeCalls.add(call);
        call.enqueue(new Callback() {

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                activeCalls.remove(call);
                alertBox("HTTP Request error: " + e.getMessage());
                requestDone("http-failure");
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                activeCalls.remove(call);
                requestDone("http-response-" + response.code());
                int code = response.code();
                switch (code) {
                    case 404:
                        alertBox("Status 404: Cloud not found by SID. No cloud with this User ID has registered in the proxy.");
                        break;
                    case 503:
                        alertBox("Status 503: Max request concurrent limit reached.");
                        break;
                    case 421:
                        alertBox("Status 421: The cloud is not logged into the proxy. Please restart it.");
                        break;
                    case 200:
                        handleSuccessfulResponse(response);
                        break;
                    default:
                        alertBox("HTTP error code: " + code);
                        break;
                }
            }
        });
    }

    private static void handleSuccessfulResponse(Response response) {
        try (ResponseBody body = response.body()) {
            if (body == null) {
                alertBox("Response body is null.");
                return;
            }

            String responseText = body.string();
            if (responseText.isEmpty()) {
                alertBox("Response body is empty.");
                return;
            }

            executor.submit(() -> {
                try {
                    handleResponse(responseText);
                } catch (Exception e) {
                    e.printStackTrace();
                    alertBox("Error processing response: " + e.getMessage());
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            alertBox("Error reading response body: " + e.getMessage());
        }
    }

    private static void handleResponse(String responseText) {
        try {
            Session session = SessionManager.getCurrentSession();
            if (session == null) {
                alertBox("No active session found.");
                return;
            }

            if (session.getQRkey() != null) {
                getEncryptedQR(responseText);
            } else if (session.getDeviceKey() != null || session.getEncryptionType() != null) {
                var decrypted = decryptData(base64ToBuffer(responseText));
                onResponse(decrypted);
            } else {
                var response = base64ToBuffer(responseText);
                var decrypted = RsaEncryption.decryptData(SessionManager.getCurrentSession().getPrivateKey(), response);
                onResponse(decrypted);
            }

        } catch (Exception e) {
            e.printStackTrace();
            alertBox("Error handling response: " + e.getMessage());
        }
    }

    private static synchronized void requestDone() {
        requestDone("unspecified");
    }

    private static synchronized void requestDone(String reason) {
        int current = concurrentRequest.decrementAndGet();
        if (current < 0) {
            System.out.println("[RequestManager][Concurrency] WARNING: concurrentRequest is negative (" + current + ")");
        }
        logConcurrencyState("requestDone:" + reason, null);
        tryStartNext();
    }

    private static synchronized void logConcurrencyState(String event, Integer commandId) {
        if (!DEBUG_CONCURRENCY_LOGS) return;
        String command = commandId == null ? "-" : getCommandName(commandId) + "(" + commandId + ")";
        System.out.println(
                "[RequestManager][Concurrency] " + event +
                        " cmd=" + command +
                        " concurrent=" + concurrentRequest.get() + "/" + maxConcurrentRequest +
                        " queue=" + spooler.size() +
                        " activeCalls=" + activeCalls.size()
        );
    }

    public static void getEncryptedQR(String encryptedDataB64) throws Exception {
        byte[] encryptedData = base64ToBuffer(encryptedDataB64);
        byte[] decryptedData = decryptXorAB(SessionManager.getCurrentSession().getQRkey(), encryptedData);
        SessionManager.getCurrentSession().setQRkey(null);

        int offset = 0;
        int type = decryptedData[offset] & 0xFF;
        if (type != 2) {
            throw new RuntimeException("QR code format not supported!");
        }
        offset += 1;

        int mSize = 2048 / 8;
        byte[] modulus = Arrays.copyOfRange(decryptedData, offset, offset + mSize);
        offset += mSize;
        byte[] exponent = Arrays.copyOfRange(decryptedData, offset, offset + 3);
        PublicKey rsaPubKey = createRsaPublicKey(modulus, exponent);
        setClient(rsaPubKey);
        
        // Save session after QR processing
        saveSessionIfCallbackExists();
    }

    public static void onResponse(byte[] binary) {
        ByteBuffer buffer = ByteBuffer.wrap(binary);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        int commandId = buffer.getInt();
        String command = getCommandName(commandId);

        System.out.println("Command ID: " + commandId);
        System.out.println("Command: " + command);
        if (Command.Authentication.getId() == commandId) {
            System.out.println("Authentication success!");
            AuthSuccess();
            return;
        }

        byte[] data = Arrays.copyOfRange(binary, 4, binary.length);
        var params = splitData(data);
        System.out.println("Params: " + params);

        if(Command.SetFile.getId() == commandId){
            handleServerUploadResponse(params);
        }
        if(Command.Pair.getId() == commandId){
            Pairing.Pair(params);
        }
    }

    private static void AuthSuccess() {
        System.out.println("Authentication success!, saving session...");
        saveSessionIfCallbackExists();
    }
    
    private static void saveSessionIfCallbackExists() {
        if (persistenceCallback != null) {
            try {
                persistenceCallback.saveSession(SessionManager.getCurrentSession());
                System.out.println("Session saved successfully!");
            } catch (Exception e) {
                persistenceCallback.onSaveError("Failed to save session: " + e.getMessage());
            }
        }
    }


    public static synchronized void cancelAllPendingRequests() {
        // Clear all pending requests from the spooler
        spooler.clear();

        // Interrupt running requests by shutting down and recreating the executor
        try {
            // Attempt graceful shutdown first
            executor.shutdown();

            // Wait up to 2 seconds for current tasks to complete
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                // Force shutdown if tasks don't complete gracefully
                List<Runnable> remainingTasks = executor.shutdownNow();
                System.out.println("Forcibly cancelled " + remainingTasks.size() + " pending upload tasks");

                // Wait a bit more for tasks to respond to interruption
                if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                    System.err.println("Some upload tasks did not terminate gracefully");
                }
            }

            // Reset concurrent request counter since we're stopping everything
            concurrentRequest.set(0);

            System.out.println("All upload requests cancelled successfully");

        } catch (InterruptedException e) {
            // Current thread was interrupted, re-interrupt and force shutdown
            Thread.currentThread().interrupt();
            executor.shutdownNow();
            concurrentRequest.set(0);
            System.err.println("Cancellation was interrupted, forced shutdown");
        }

        for (Call call : activeCalls) {
            if (!call.isCanceled()) {
                call.cancel();
            }
        }
        activeCalls.clear();
    }

    private static synchronized void recreateExecutor() {
        if (executor.isShutdown()) {
            executor = Executors.newCachedThreadPool();
        }
    }


}
