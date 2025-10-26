package com.cloud.communication.cryto;

import static com.cloud.communication.cryto.ConversionUtils.bufferToString;

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

import java.util.function.BiConsumer;

public class FileUploader {

    private static final int CHUNK_SIZE = 1024 * 1024; // 1MB

    // Internal state
    private static final Map<String, byte[]> upload = new ConcurrentHashMap<>();
    private static final Map<String, Integer> chunkParts = new ConcurrentHashMap<>();
    private static final Map<String, Integer> chunkLength = new ConcurrentHashMap<>();
    private static final Map<String, BiConsumer<String, Integer>> chunkRequest = new ConcurrentHashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void startSendFileAsync(File file) {
        new Thread(() -> startSendFile(file)).start();
    }

    public static void startSendFileAsync(InputStream inputStream, String filename) {
        new Thread(() -> startSendFile(inputStream, filename)).start();
    }

    public static void startSendFile(File file) {
        try {
            byte[] fileData = Files.readAllBytes(file.toPath());

            String fullPath = file.getName(); // Or construct full path if needed

            upload.put(fullPath, fileData);

            // Start sending from chunk 1
            chunkRequestCallback(fullPath, 1);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void startSendFile(InputStream inputStream, String filename) {
        try (inputStream) {
            try {
                byte[] fileData = readAllBytesCompat(inputStream);

                upload.put(filename, fileData);

                chunkRequestCallback(filename, 1);

            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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

        String base64Chunk = Base64.getEncoder().encodeToString(chunkData);
        FileChunk fileChunkObject = new FileChunk(fullFileName, base64Chunk, chunkNumber, parts);

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
            // Handle serialization error, maybe abort the upload
        }

    }

    public static void handleServerUploadResponse(List<byte[]> parts) {
        // Server responds with: "fileName\tcurrentChunkNumber"


        String[] partsStr = bufferToString(parts.get(0)).split("\t");
        if (partsStr.length != 2) return;

        String fullFileName = partsStr[0];
        int nextChunkNumber = Integer.parseInt(partsStr[1]) + 1;
        int totalChunks = chunkParts.getOrDefault(fullFileName, -1);
        if (totalChunks == -1) {
            System.out.println("Upload state missing for " + fullFileName);
            return;
        }

        if (nextChunkNumber > totalChunks) {
            System.out.println("Upload completed for " + fullFileName);
            upload.remove(fullFileName);
            chunkLength.remove(fullFileName);
            chunkParts.remove(fullFileName);
            chunkRequest.remove(fullFileName);
            refreshDirectory(fullFileName);
        } else if (chunkRequest.containsKey(fullFileName)) {
            BiConsumer<String, Integer> callback = chunkRequest.get(fullFileName);
            if (callback != null) {
                callback.accept(fullFileName, nextChunkNumber);
            }
        }
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


    static class FileChunk {
        // Fields are not public
        private final String fullName;
        private final String data;
        private final int chunkPart;
        private final int totalChunk;


        public FileChunk(String fullName, String data, int chunkPart, int totalChunk) {
            this.fullName = fullName;
            this.data = data;
            this.chunkPart = chunkPart;
            this.totalChunk = totalChunk;
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

