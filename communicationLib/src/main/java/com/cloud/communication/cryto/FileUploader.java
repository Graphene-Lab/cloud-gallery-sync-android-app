package com.cloud.communication.cryto;

import static com.cloud.communication.cryto.ConversionUtils.bufferToString;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class FileUploader {

    private static final int CHUNK_SIZE = 1024 * 1024; // 1MB
    private static final long CHUNK_ACK_TIMEOUT_MS = 30_000L;
    private static final int MAX_CHUNK_ACK_RETRIES = 3;
    private static final String DEFAULT_PHOTO_DIR = "Photos";

    // Internal state
    private static final Map<String, byte[]> upload = new ConcurrentHashMap<>();
    private static final Map<String, Integer> chunkParts = new ConcurrentHashMap<>();
    private static final Map<String, Integer> chunkLength = new ConcurrentHashMap<>();
    private static final Map<String, Long> uploadUnixTimestampByTransport = new ConcurrentHashMap<>();
    private static final Map<String, BiConsumer<String, Integer>> chunkRequest = new ConcurrentHashMap<>();
    private static final Map<String, Integer> inFlightChunkByFile = new ConcurrentHashMap<>();
    private static final Map<String, Integer> chunkAckRetryCountByFile = new ConcurrentHashMap<>();
    private static final Map<String, ScheduledFuture<?>> chunkAckTimeoutTasks = new ConcurrentHashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ScheduledExecutorService timeoutScheduler =
            Executors.newSingleThreadScheduledExecutor();

    private static final Map<String, Consumer<ChunkProgress>> progressCallbacks = new ConcurrentHashMap<>();

    // Progress data class
    public static class ChunkProgress {
        public final String filename;
        public final int currentChunk;
        public final int totalChunks;
        public final boolean isCompleted;
        public final boolean hasError;
        public final String errorMessage;

        public ChunkProgress(String filename, int currentChunk, int totalChunks, boolean isCompleted) {
            this(filename, currentChunk, totalChunks, isCompleted, false, null);
        }

        public ChunkProgress(
                String filename,
                int currentChunk,
                int totalChunks,
                boolean isCompleted,
                boolean hasError,
                String errorMessage
        ) {
            this.filename = filename;
            this.currentChunk = currentChunk;
            this.totalChunks = totalChunks;
            this.isCompleted = isCompleted;
            this.hasError = hasError;
            this.errorMessage = errorMessage;
        }
    }

    // New method that accepts progress callback
    public static void startSendFileWithProgressCallback(
            InputStream inputStream,
            String filename,
            Consumer<ChunkProgress> onProgressUpdate) {
        startSendFileWithProgressCallback(inputStream, filename, 0L, onProgressUpdate);
    }

    public static void startSendFileWithProgressCallback(
            byte[] fileData,
            String filename,
            Consumer<ChunkProgress> onProgressUpdate) {
        startSendFileWithProgressCallback(fileData, filename, 0L, onProgressUpdate);
    }

    public static void startSendFileWithProgressCallback(
            InputStream inputStream,
            String filename,
            long unixLastWriteTimestampSeconds,
            Consumer<ChunkProgress> onProgressUpdate) {

        // Store the progress callback
        if (onProgressUpdate != null) {
            progressCallbacks.put(filename, onProgressUpdate);
        }

        // Start the regular upload process
        startSendFile(inputStream, filename, unixLastWriteTimestampSeconds);
    }

    public static void startSendFileWithProgressCallback(
            byte[] fileData,
            String filename,
            long unixLastWriteTimestampSeconds,
            Consumer<ChunkProgress> onProgressUpdate) {

        if (onProgressUpdate != null) {
            progressCallbacks.put(filename, onProgressUpdate);
        }

        startSendFile(fileData, filename, unixLastWriteTimestampSeconds);
    }


    public static void startSendFileAsync(File file) {
        new Thread(() -> startSendFile(file)).start();
    }

    public static void startSendFileAsync(InputStream inputStream, String filename) {
        new Thread(() -> startSendFile(inputStream, filename)).start();
    }

    public static void startSendFileAsync(byte[] fileData, String filename) {
        new Thread(() -> startSendFile(fileData, filename)).start();
    }

    public static void startSendFile(File file) {
        try {
            byte[] fileData = Files.readAllBytes(file.toPath());

            String fullPath = file.getName(); // Or construct full path if needed

            upload.put(fullPath, fileData);
            uploadUnixTimestampByTransport.put(fullPath, file.lastModified() / 1000);

            // Start sending from chunk 1
            chunkRequestCallback(fullPath, 1);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void startSendFile(InputStream inputStream, String filename) {
        startSendFile(inputStream, filename, 0L);
    }

    public static void startSendFile(byte[] fileData, String filename) {
        startSendFile(fileData, filename, 0L);
    }

    public static void startSendFile(InputStream inputStream, String filename, long unixLastWriteTimestampSeconds) {
        try (inputStream) {
            try {
                byte[] fileData = readAllBytesCompat(inputStream);

                upload.put(filename, fileData);
                uploadUnixTimestampByTransport.put(filename, Math.max(unixLastWriteTimestampSeconds, 0L));

                chunkRequestCallback(filename, 1);

            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void startSendFile(byte[] fileData, String filename, long unixLastWriteTimestampSeconds) {
        upload.put(filename, fileData);
        uploadUnixTimestampByTransport.put(filename, Math.max(unixLastWriteTimestampSeconds, 0L));
        chunkRequestCallback(filename, 1);
    }

    private static byte[] readAllBytesCompat(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[8192];
        int bytesRead;
        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        return outputStream.toByteArray();
    }

    private static void chunkRequestCallback(String fullFileName, int chunkNumber) {
        // This simulates the JavaScript chunkRequestCallback
        setFile(fullFileName, chunkNumber, upload.get(fullFileName));
    }

    private static void setFile(String fullFileName, int chunkNumber, byte[] data) {
        int fileLength = data.length;
        int position = (chunkNumber - 1) * CHUNK_SIZE;
        int toTake = Math.min(CHUNK_SIZE, fileLength - position);

        byte[] chunkData = new byte[toTake];
        System.arraycopy(data, position, chunkData, 0, toTake);

        uploadFile(fullFileName, chunkData, chunkNumber, fileLength, FileUploader::chunkRequestCallback);

        // Waiting for server response to continue (will call chunkRequestCallback)
    }

    private static void uploadFile(String fullFileName, byte[] chunkData, int chunkNumber, int fileLength,
                                   BiConsumer<String, Integer> chunkRequestCallback) {
        if (chunkNumber <= 0) {
            throw new IllegalArgumentException("Chunk number must be >= 1");
        }

        if (!chunkRequest.containsKey(fullFileName) && chunkRequestCallback != null) {
            chunkRequest.put(fullFileName, chunkRequestCallback);
        }

        int lengthOfChunks;
        if (chunkData.length == fileLength) {
            lengthOfChunks = fileLength;
        } else if (chunkNumber == 1) {
            lengthOfChunks = chunkData.length;
            chunkLength.put(fullFileName, lengthOfChunks);
        } else {
            lengthOfChunks = chunkLength.getOrDefault(fullFileName, CHUNK_SIZE);
        }

        int parts = (int) Math.ceil((double) fileLength / lengthOfChunks);
        parts = Math.max(parts, 1);
        chunkParts.put(fullFileName, parts);

        //  Call progress callback when sending chunk
        Consumer<ChunkProgress> progressCallback = progressCallbacks.get(fullFileName);
        if (progressCallback != null) {
            ChunkProgress progress = new ChunkProgress(fullFileName, chunkNumber, parts, false);
            progressCallback.accept(progress);
        }

        scheduleChunkAckTimeout(fullFileName, chunkNumber);

        String base64Chunk = Base64.getEncoder().encodeToString(chunkData);
        Long unixLastWriteTimestamp = chunkNumber == parts
                ? uploadUnixTimestampByTransport.getOrDefault(fullFileName, 0L)
                : null;
        FileChunk fileChunkObject = new FileChunk(
                fullFileName,
                base64Chunk,
                chunkNumber,
                parts,
                unixLastWriteTimestamp
        );

        System.out.printf("Uploading chunk %d/%d for file: %s\n", chunkNumber, parts, fullFileName);


        try {
            // STEP 1: Serialize the Java object to a JSON String. (Equivalent to JSON.stringify)
            String jsonString = objectMapper.writeValueAsString(fileChunkObject);

            // STEP 2: Convert the JSON String to a byte array using UTF-8. (Equivalent to TextEncoder.encode)
            byte[] payload = jsonString.getBytes(StandardCharsets.UTF_8);

            // Now, send the final byte array payload
            RequestManager.enqueueRequest(Command.SetFile.getId(), payload);
//            RequestManager.executeRequest(Command.SetFile.getId(), payload);

        } catch (Exception e) {
            e.printStackTrace();
            failUpload(fullFileName, "Failed to serialize/send chunk payload: " + e.getMessage());
        }

    }

    public static void handleServerUploadResponse(List<byte[]> parts) {
        // Server responds with: "fileName\tcurrentChunkNumber"

        if (parts == null || parts.isEmpty()) {
            return;
        }

        String responseText = bufferToString(parts.get(0));
        int separator = responseText.lastIndexOf('\t');
        if (separator <= 0 || separator >= responseText.length() - 1) {
            System.out.println("Invalid upload ACK payload: " + responseText);
            return;
        }

        String fullFileName = responseText.substring(0, separator);
        int nextChunkNumber;
        try {
            nextChunkNumber = Integer.parseInt(responseText.substring(separator + 1)) + 1;
        } catch (NumberFormatException e) {
            System.out.println("Invalid upload ACK chunk index: " + responseText);
            return;
        }
        int totalChunks = chunkParts.getOrDefault(fullFileName, -1);
        if (totalChunks == -1) {
            // Late ACK for already-cleaned upload (e.g., after retry race). Safe to ignore.
            System.out.println("Ignoring stale upload ACK for " + fullFileName);
            return;
        }

        cancelChunkAckTimeout(fullFileName);
        inFlightChunkByFile.remove(fullFileName);
        chunkAckRetryCountByFile.remove(fullFileName);

        if (nextChunkNumber > totalChunks) {
            System.out.println("Upload completed for " + fullFileName);
            // Call progress callback for completion
            Consumer<ChunkProgress> progressCallback = progressCallbacks.get(fullFileName);
            if (progressCallback != null) {
                ChunkProgress progress = new ChunkProgress(fullFileName, totalChunks, totalChunks, true);
                progressCallback.accept(progress);
            }

            clearUploadState(fullFileName);
            refreshDirectory(fullFileName);

        } else if (chunkRequest.containsKey(fullFileName)) {
            BiConsumer<String, Integer> callback = chunkRequest.get(fullFileName);
            if (callback != null) {
                callback.accept(fullFileName, nextChunkNumber);
            }
        }
    }

    public static boolean handleServerErrorResponse(String errorMessage) {
        if (errorMessage == null || errorMessage.trim().isEmpty()) {
            return false;
        }

        if (!isMissingDirectoryError(errorMessage)) {
            return false;
        }

        enqueueCreateDir();
        return true;
    }

    private static void refreshDirectory(String fullFileName) {
        String[] parts = fullFileName.split("/");
        StringBuilder pathBuilder = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (i > 0) pathBuilder.append("/");
            pathBuilder.append(parts[i]);
        }
        String path = pathBuilder.toString();
        System.out.println("Refreshing directory: " + path);
    }

    private static boolean isMissingDirectoryError(String errorMessage) {
        return errorMessage.contains("Could not find a part of the path");
    }

    private static void enqueueCreateDir() {
        String payloadText = "\t" + FileUploader.DEFAULT_PHOTO_DIR;
        byte[] payload = payloadText.getBytes(StandardCharsets.UTF_8);
        System.out.println(
                "Requesting server directory creation: path='" + "', folder='" + FileUploader.DEFAULT_PHOTO_DIR + "'"
        );
        RequestManager.enqueueRequest(Command.CreateDir.getId(), payload);
    }


    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class FileChunk {
        // Fields are not public
        private final String fullName;
        private final String data;
        private final int chunkPart;
        private final int totalChunk;
        private final Long unixLastWriteTimestamp;


        public FileChunk(
                String fullName,
                String data,
                int chunkPart,
                int totalChunk,
                Long unixLastWriteTimestamp
        ) {
            this.fullName = fullName;
            this.data = data;
            this.chunkPart = chunkPart;
            this.totalChunk = totalChunk;
            this.unixLastWriteTimestamp = unixLastWriteTimestamp;
        }

        // Jackson will discover these public getters
        @JsonProperty("FullName")
        public String getFullName() {
            return fullName;
        }

        @JsonProperty("Data")
        public String getData() {
            return data;
        }

        @JsonProperty("ChunkPart")
        public int getChunkPart() {
            return chunkPart;
        }

        @JsonProperty("TotalChunk")
        public int getTotalChunk() {
            return totalChunk;
        }

        @JsonProperty("UnixLastWriteTimestamp")
        public Long getUnixLastWriteTimestamp() {
            return unixLastWriteTimestamp;
        }
    }

    private static void scheduleChunkAckTimeout(String fullFileName, int chunkNumber) {
        inFlightChunkByFile.put(fullFileName, chunkNumber);
        ScheduledFuture<?> previousTimeout = chunkAckTimeoutTasks.remove(fullFileName);
        if (previousTimeout != null) {
            previousTimeout.cancel(false);
        }

        ScheduledFuture<?> timeoutTask = timeoutScheduler.schedule(
                () -> onChunkAckTimeout(fullFileName, chunkNumber),
                CHUNK_ACK_TIMEOUT_MS,
                TimeUnit.MILLISECONDS
        );
        chunkAckTimeoutTasks.put(fullFileName, timeoutTask);
    }

    private static void cancelChunkAckTimeout(String fullFileName) {
        ScheduledFuture<?> timeoutTask = chunkAckTimeoutTasks.remove(fullFileName);
        if (timeoutTask != null) {
            timeoutTask.cancel(false);
        }
    }

    private static void onChunkAckTimeout(String fullFileName, int chunkNumber) {
        Integer currentInFlightChunk = inFlightChunkByFile.get(fullFileName);
        if (currentInFlightChunk == null || currentInFlightChunk != chunkNumber) {
            return;
        }

        int retryAttempt = chunkAckRetryCountByFile.getOrDefault(fullFileName, 0);
        if (retryAttempt >= MAX_CHUNK_ACK_RETRIES) {
            failUpload(
                    fullFileName,
                    "No ACK for chunk " + chunkNumber + " after " + MAX_CHUNK_ACK_RETRIES + " retries"
            );
            return;
        }

        int nextAttempt = retryAttempt + 1;
        chunkAckRetryCountByFile.put(fullFileName, nextAttempt);
        int totalChunks = chunkParts.getOrDefault(fullFileName, -1);
        System.out.printf(
                "ACK timeout for chunk %d/%d of %s. Retrying (%d/%d)\n",
                chunkNumber,
                totalChunks,
                fullFileName,
                nextAttempt,
                MAX_CHUNK_ACK_RETRIES
        );

        byte[] fileData = upload.get(fullFileName);
        if (fileData == null) {
            failUpload(fullFileName, "Upload payload missing while retrying chunk " + chunkNumber);
            return;
        }

        setFile(fullFileName, chunkNumber, fileData);
    }

    private static void failUpload(String fullFileName, String errorMessage) {
        System.out.println("Upload failed for " + fullFileName + ": " + errorMessage);
        Consumer<ChunkProgress> progressCallback = progressCallbacks.get(fullFileName);
        if (progressCallback != null) {
            int currentChunk = inFlightChunkByFile.getOrDefault(fullFileName, 0);
            int totalChunks = chunkParts.getOrDefault(fullFileName, 0);
            ChunkProgress errorProgress = new ChunkProgress(
                    fullFileName,
                    currentChunk,
                    totalChunks,
                    false,
                    true,
                    errorMessage
            );
            progressCallback.accept(errorProgress);
        }
        clearUploadState(fullFileName);
    }

    private static void clearUploadState(String fullFileName) {
        cancelChunkAckTimeout(fullFileName);
        upload.remove(fullFileName);
        chunkLength.remove(fullFileName);
        chunkParts.remove(fullFileName);
        uploadUnixTimestampByTransport.remove(fullFileName);
        chunkRequest.remove(fullFileName);
        inFlightChunkByFile.remove(fullFileName);
        chunkAckRetryCountByFile.remove(fullFileName);
        progressCallbacks.remove(fullFileName);
    }

    public static void main(String[] args) {
        initMockSession();

        // Initialize session persistence for testing
//        RequestManager.setSessionPersistenceCallback(new SessionPersistenceCallback() {
//            @Override
//            public void saveSession(Session session) {
//                SessionStorage.saveSession(session);
//            }
//
//            @Override
//            public void onSaveError(String error) {
//                System.err.println("Session save error: " + error);
//            }
//
//            @Override
//            public CompletableFuture<Session> loadSession() {
//                return SessionStorage.loadSession();
//            }
//        });
        File file = new File("C:/Users/Ramazan/Downloads/the_cloud_ppt.pdf");
        File file2 = new File("C:/Users/Ramazan/Downloads/the_cloud_ppt (1).pdf");
//        startSendFileAsync(file);
        startSendFile(file);
        startSendFile(file2);
//        var qrCode = "Avxfz60f3Hk4cyCIOCzrRWTynhulljCgQSoRCMNmiLwNMTk1LjIwLjIzNS41";
//        var pin = 428717;
//        QrCodeHandler.onQrCodeAcquired(qrCode, pin).thenRun(() -> {
//                    CompletableFuture.delayedExecutor(40, TimeUnit.SECONDS).execute(() -> {
//                        System.out.println("Timer expired, starting file upload...");
//                        File file = new File("C:/Users/Ramazan/Downloads/the_cloud_ppt.pdf");
//                        startSendFileAsync(file);
//                    });
//                }
//        );
    }


    private static void initMockSession() {
        String entryPoint = "proxy";
        String clientId = "445f4a39f4a46932";
        String publicKeyB64 = "sENlSQVGEwCJdzJd4h9R7EfhpVc9VJUSwPLp2vimrA0qMkeMz68qQ94JL/IUYajsgEJiqnyK0H35a0CR7LVL+Pn9SzK8YYgFi/txEG9gLXYFpsCyyYDBsYqlztqgqpSuWKD8hXosDuWk0oLc2oLfFOFAqC48xR8ZvYdZwaHiOLW3NbGtgHAMm6vqcjhdKEbfrsgEq++eFSiA/KbBAAKuhzrWDWipydHAXqsE/68v6p61Txq0IFrsXV99eciUh/T1KzDPQSjgeMGOt9FKHF7RKNmCFtwqRoK9kvj73FNVVti/P15dJEa/IWBFdmzoUziHKhCHnBter4l2xzAAYfGrww==";
        String encryptionType = "aes";
        byte[] deviceKey = {-83, 112, -48, 43, 29, -33, 84, 127, -50, 125, -52, -86, 52, -4, -81, 61, 97, 15, -63, -20, 107, 121, 112, 35, 46, 54, -3, -80, -72, -71, -23, 36};
        byte[] IV = {-8, 92, 2, 103, 68, -48, 21, -12, -52, -32, -60, -24, -63, 33, 94, 54};
        String serverId = "c45f68a68eeb15a9";

//        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyB64);
//        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
//        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
//        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        // Convert the deviceKey into SecretKey using the provided AES encryption type
        SecretKey symmetricKey = new SecretKeySpec(deviceKey, 0, deviceKey.length, encryptionType);

        var session = SessionManager.getCurrentSession();
        session.setEntryPoint(entryPoint);
        session.setClientId(clientId);
//        session.setPublicKeyB64(publicKeyB64);
//        session.setEncryptionType(encryptionType);
//        session.setDeviceKey(deviceKey);
        session.setIV(IV);
        session.setServerId(serverId);
        session.setPin(617075);
//        session.setPublicKey(publicKey);
        session.setSymmetricKey(symmetricKey);
    }
}

class SessionStorage {
    private static Session storedSession = null;

    public static void saveSession(Session session) {
        storedSession = session;
        System.out.println("Session saved in memory for testing");
    }

    public static CompletableFuture<Session> loadSession() {
        return CompletableFuture.completedFuture(storedSession);
    }

    public static void clearSession() {
        storedSession = null;
    }
}

