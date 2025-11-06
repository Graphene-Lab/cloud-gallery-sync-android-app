package com.cloud.communication.cryto;


import org.bouncycastle.crypto.digests.Blake2bDigest;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

public class ZeroKnowledgeProof {

    private final byte[] encryptionMasterKey;
    private final byte[] filenameObfuscationKey;

        public ZeroKnowledgeProof(byte[] encryptionMasterKey) {
        this.filenameObfuscationKey = hash256(encryptionMasterKey); // 32 bytes
        this.encryptionMasterKey = blake2b(
                concatenate(encryptionMasterKey, this.filenameObfuscationKey)
        ); // 64 bytes
    }

    public byte[] DerivedEncryptionKey(File file) throws IOException {
        long unixLastWriteTimestamp = file.lastModified() / 1000;
        String relativeName = file.getCanonicalPath();
        byte[] bytes = relativeName.getBytes(StandardCharsets.UTF_8);
        byte[] len = BitConverter.getBytes(bytes.length);
        byte[] date = BitConverter.getBytes(unixLastWriteTimestamp);

        byte[] concat = concatenate(bytes, len, date, this.encryptionMasterKey);
        return blake2b(concat);
    }

    // overloaded method with timestamp parameter for decryption
    public byte[] DerivedEncryptionKey(File file, long unixLastWriteTimestamp) throws IOException {
        // Use provided timestamp instead of file.lastModified()
        String relativeName = file.getCanonicalPath();
        byte[] bytes = relativeName.getBytes(StandardCharsets.UTF_8);
        byte[] len = BitConverter.getBytes(bytes.length);
        byte[] date = BitConverter.getBytes(unixLastWriteTimestamp);  // Use parameter

        byte[] concat = concatenate(bytes, len, date, this.encryptionMasterKey);
        return blake2b(concat);
    }

    public void EncryptFile(File inputFile, String outputFile) throws IOException {
        byte[] key = DerivedEncryptionKey(inputFile);
        processFile(inputFile, outputFile, key);
    }

    // Fix DecryptFile to pass the encrypted file's timestamp
    public void DecryptFile(File inputFile, String outputFile) throws IOException {
        long encryptedFileTimestamp = inputFile.lastModified() / 1000;
        byte[] key = DerivedEncryptionKey(new File(outputFile), encryptedFileTimestamp);
        processFile(inputFile, outputFile, key);
    }

    private void processFile(File inputFile, String outputFile, byte[] key)
            throws IOException {
        if (key == null || key.length != 64) {
            throw new IllegalArgumentException("Key must be 64 bytes.");
        }

        final int blockSize = 8;
        final int cyclesPerHash = 8;

        byte[] salt = blake2b(key);
        byte[] currentKey = blake2b(salt);

        try (
                FileInputStream inputStream = new FileInputStream(inputFile);
                FileOutputStream outputStream = new FileOutputStream(outputFile)
        ) {
            byte[] inputBuffer = new byte[blockSize];
            int cycleCounter = 0;
            int bytesRead;

            while ((bytesRead = inputStream.read(inputBuffer, 0, blockSize)) > 0) {
                long inputBlock = BitConverter.toUInt64(inputBuffer);
                long keyBlock = BitConverter.toUInt64(
                        Arrays.copyOfRange(
                                currentKey,
                                cycleCounter * blockSize,
                                cycleCounter * blockSize +
                                        blockSize
                        )
                );

                long outputBlock = inputBlock ^ keyBlock;
                byte[] outputBytes = BitConverter.getBytes(outputBlock);
                outputStream.write(outputBytes, 0, bytesRead);

                cycleCounter++;

                if (cycleCounter >= cyclesPerHash) {
                    currentKey = blake2b(salt, currentKey);
                    cycleCounter = 0;
                }
            }
        }
    }

    public String EncryptFullFileName(String fullFileName) {
        return processFullFileName(fullFileName, this.filenameObfuscationKey, true);
    }

    public String DecryptFullFileName(String fullFileName) {
        return processFullFileName(fullFileName, this.filenameObfuscationKey, false);
    }

    private String processFullFileName(
            String fullFileName,
            byte[] key,
            boolean isEncrypt
    ) {
        Path path = Paths.get(fullFileName);
        List<String> result = new ArrayList<>();
        for (Path part : path) {
            result.add(
                    isEncrypt
                            ? EncryptFileName(part.toString(), key)
                            : DecryptFileName(part.toString(), key)
            );
        }
        return String.join("/", result);
    }

    private static final char EncryptFileNameEndChar = '⁇';
    private static final String Set256Chars =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyzÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿĀāĂăĄąĆćĈĉĊċČčĎďĐđĒēĔĕĖėĘęĚěĜĝĞğĠġĢģĤĥĦħĨĩĪīĬĭĮįİıĲĳĴĵĶķĹĺĻļĽľŁłŃńŅņŇňŌōŎŏŐőŒœŔŕŖŗŘřŚśŜŝŞşŠšŢţŤťŦŧŨũŪūŬŭŮůŰűŲųŴŵŶŷŸŹźŻżŽžǍǎǏǐǑǒǓǔǕǖǗ";
    private static Dictionary<Character, Byte> DecryptHelper = null;

    private String EncryptFileName(String fileName, byte[] key) {
        if (fileName == null || fileName.isEmpty()) {
            return fileName;
        }
        boolean hasLeadingDot = fileName.startsWith(".");
        String namePart = hasLeadingDot ? fileName.substring(1) : fileName;
        String encryptName = PerformEncryptText(namePart, key);
        return (hasLeadingDot ? "." : "") + encryptName + EncryptFileNameEndChar;
    }

    private String DecryptFileName(String encryptedFileName, byte[] key) {
        if (
                !encryptedFileName.endsWith(String.valueOf(EncryptFileNameEndChar))
        ) {
            return encryptedFileName;
        }
        encryptedFileName = encryptedFileName.substring(
                0,
                encryptedFileName.length() - 1
        );
        if (encryptedFileName.isEmpty()) {
            return encryptedFileName;
        }
        boolean hasLeadingDot = encryptedFileName.startsWith(".");
        String encodedPart = hasLeadingDot
                ? encryptedFileName.substring(1)
                : encryptedFileName;
        String namePart = PerformDecryptText(encodedPart, key);
        return (hasLeadingDot ? "." : "") + namePart;
    }

    private String PerformEncryptText(String text, byte[] key) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        byte[] masterKey = concatenate(key, new byte[] { (byte) bytes.length });
        byte[] masc = new byte[0];
        do {
            masterKey = blake2b(masterKey);
            masc = concatenate(masc, masterKey);
        } while (masc.length < bytes.length);

        StringBuilder result = new StringBuilder(bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            // Fix: Ensure the XOR result is treated as unsigned byte (0-255)
            int index = (b ^ masc[i]) & 0xFF;
            result.append(Set256Chars.charAt(index));
        }
        return result.toString();
    }

    private String PerformDecryptText(String text, byte[] key) {
        if (DecryptHelper == null) {
            DecryptHelper = new Hashtable<>();
            for (int i = 0; i < Set256Chars.length(); i++) {
                DecryptHelper.put(Set256Chars.charAt(i), (byte) i);
            }
        }

        byte[] bytes = new byte[text.length()];
        byte[] masterKey = concatenate(key, new byte[] { (byte) bytes.length });
        byte[] masc = new byte[0];
        do {
            masterKey = blake2b(masterKey);
            masc = concatenate(masc, masterKey);
        } while (masc.length < bytes.length);

        for (int i = 0; i < bytes.length; i++) {
            byte b = DecryptHelper.get(text.charAt(i));
            byte m = masc[i];
            bytes[i] = (byte) (b ^ m);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    // Helper methods
    private static byte[] hash256(byte[] data) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance(
                    "SHA-256"
            );
            return digest.digest(data);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] blake2b(byte[]... arrays) {
        Blake2bDigest digest = new Blake2bDigest(512);
        byte[] concatenated = concatenate(arrays);
        digest.update(concatenated, 0, concatenated.length);
        byte[] result = new byte[digest.getDigestSize()];
        digest.doFinal(result, 0);
        return result;
    }

    private static byte[] concatenate(byte[]... arrays) {
        int totalLength = 0;
        for (byte[] array : arrays) {
            totalLength += array.length;
        }
        byte[] result = new byte[totalLength];
        int destPos = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, destPos, array.length);
            destPos += array.length;
        }
        return result;
    }

    private static class BitConverter {

        public static byte[] getBytes(long value) {
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putLong(value);
            return buffer.array();
        }

        public static byte[] getBytes(int value) {
            ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(value);
            return buffer.array();
        }

        public static long toUInt64(byte[] bytes) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            return buffer.getLong();
        }
    }

    // Encrypt bytes in memory (no file system)
    public byte[] encryptBytes(byte[] inputBytes, String originalFilename, long timestamp) throws IOException {
        byte[] key = DerivedEncryptionKey(originalFilename, timestamp);
        return processBytes(inputBytes, key);
    }

    // Decrypt bytes in memory (no file system)
    public byte[] decryptBytes(byte[] encryptedBytes, String originalFilename, long timestamp) throws IOException {
        byte[] key = DerivedEncryptionKey(originalFilename, timestamp);
        return processBytes(encryptedBytes, key);
    }

    // New method: derive key from filename only (no File object)
    public byte[] DerivedEncryptionKey(String filename, long timestamp) throws IOException {
        byte[] bytes = filename.getBytes(StandardCharsets.UTF_8);
        byte[] len = BitConverter.getBytes(bytes.length);
        byte[] date = BitConverter.getBytes(timestamp / 1000);

        byte[] concat = concatenate(bytes, len, date, this.encryptionMasterKey);
        return blake2b(concat);
    }

    // Process bytes in memory instead of files
    private byte[] processBytes(byte[] inputBytes, byte[] key) throws IOException {
        if (key == null || key.length != 64) {
            throw new IllegalArgumentException("Key must be 64 bytes.");
        }

        final int blockSize = 8;
        final int cyclesPerHash = 8;

        byte[] salt = blake2b(key);
        byte[] currentKey = blake2b(salt);

        List<Byte> outputList = new ArrayList<>();
        int cycleCounter = 0;

        for (int i = 0; i < inputBytes.length; i += blockSize) {
            int remainingBytes = Math.min(blockSize, inputBytes.length - i);
            byte[] inputBuffer = new byte[blockSize];
            System.arraycopy(inputBytes, i, inputBuffer, 0, remainingBytes);

            long inputBlock = BitConverter.toUInt64(inputBuffer);
            long keyBlock = BitConverter.toUInt64(
                    Arrays.copyOfRange(currentKey, cycleCounter * blockSize, cycleCounter * blockSize + blockSize)
            );

            long outputBlock = inputBlock ^ keyBlock;
            byte[] outputBytes = BitConverter.getBytes(outputBlock);

            for (int j = 0; j < remainingBytes; j++) {
                outputList.add(outputBytes[j]);
            }

            cycleCounter++;
            if (cycleCounter >= cyclesPerHash) {
                currentKey = blake2b(salt, currentKey);
                cycleCounter = 0;
            }
        }

        // Convert List<Byte> to byte[]
        byte[] result = new byte[outputList.size()];
        for (int i = 0; i < outputList.size(); i++) {
            result[i] = outputList.get(i);
        }
        return result;
    }

}