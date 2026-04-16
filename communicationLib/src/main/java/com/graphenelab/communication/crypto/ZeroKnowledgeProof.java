package com.graphenelab.communication.crypto;

import org.bouncycastle.crypto.digests.Blake2bDigest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

public class ZeroKnowledgeProof implements IZeroKnowledgeProof {
    private static final List<String> SPECIAL_DIRECTORIES = List.of(".cloud_cache");
    private static final char EncryptFileNameEndChar = '⁇';
    private static final String Set256Chars =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz" +
                    "ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿ" +
                    "ĀāĂăĄąĆćĈĉĊċČčĎďĐđĒēĔĕĖėĘęĚěĜĝĞğĠġĢģĤĥĦħĨĩĪīĬĭĮįİıĲĳĴĵĶķĹĺ" +
                    "ĻļĽľŁłŃńŅņŇňŌōŎŏŐőŒœŔŕŖŗŘřŚśŜŝŞşŠšŢţŤťŦŧŨũŪūŬŭŮůŰűŲųŴŵŶŷ" +
                    "ŸŹźŻżŽžǍǎǏǐǑǒǓǔǕǖǗ";

    private final byte[] encryptionMasterKey;
    private final byte[] filenameObfuscationKey;
    private static Dictionary<Character, Byte> DecryptHelper = null;

    public ZeroKnowledgeProof(byte[] encryptionMasterKey) {
        this.filenameObfuscationKey = hash256(encryptionMasterKey);
        this.encryptionMasterKey = blake2b(concatenate(encryptionMasterKey, this.filenameObfuscationKey));
    }

    private byte[] DerivedEncryptionKey(File file) throws IOException {
        long unixLastWriteTimestamp = file.lastModified() / 1000;
        return deriveContentKeyFromClearPath(file.getCanonicalPath(), unixLastWriteTimestamp);
    }

    private byte[] DerivedEncryptionKey(File file, long unixLastWriteTimestamp) throws IOException {
        return deriveContentKeyFromClearPath(file.getCanonicalPath(), unixLastWriteTimestamp);
    }

    @Override
    public void EncryptFile(File inputFile, String outputFile) throws IOException {
        byte[] key = DerivedEncryptionKey(inputFile);
        processFile(inputFile, outputFile, key);
    }

    @Override
    public void EncryptFile(File inputFile, String outputFile, String virtualRelativeName) throws IOException {
        long unixLastWriteTimestamp = inputFile.lastModified() / 1000;
        byte[] key = DerivedEncryptionKey(virtualRelativeName, unixLastWriteTimestamp);
        processFile(inputFile, outputFile, key);
    }

    @Override
    public void DecryptFile(File inputFile, String outputFile) throws IOException {
        long encryptedFileTimestamp = inputFile.lastModified() / 1000;
        byte[] key = DerivedEncryptionKey(new File(outputFile), encryptedFileTimestamp);
        processFile(inputFile, outputFile, key);
    }

    @Override
    public void DecryptFile(File inputFile, String outputFile, String virtualRelativeName) throws IOException {
        long encryptedFileTimestamp = inputFile.lastModified() / 1000;
        byte[] key = DerivedEncryptionKey(virtualRelativeName, encryptedFileTimestamp);
        processFile(inputFile, outputFile, key);
    }

    private void processFile(File inputFile, String outputFile, byte[] key) throws IOException {
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
            byte[] outputBuffer = new byte[blockSize];
            int cycleCounter = 0;
            int bytesRead;

            while ((bytesRead = inputStream.read(inputBuffer, 0, blockSize)) > 0) {
                long inputBlock = BitConverter.toUInt64(inputBuffer, 0, bytesRead);
                long keyBlock = BitConverter.toUInt64(currentKey, cycleCounter * blockSize);

                long outputBlock = inputBlock ^ keyBlock;
                BitConverter.writeBytes(outputBlock, outputBuffer, 0, bytesRead);
                outputStream.write(outputBuffer, 0, bytesRead);

                cycleCounter++;
                if (cycleCounter >= cyclesPerHash) {
                    // The web client uses keyed BLAKE2b(current, key=salt) for stream rollover.
                    currentKey = blake2bKeyed(currentKey, salt);
                    cycleCounter = 0;
                }
            }
        }
    }

    @Override
    public String EncryptFullFileName(String fullFileName) {
        return processFullFileName(fullFileName, this.filenameObfuscationKey, true);
    }

    @Override
    public String DecryptFullFileName(String fullFileName) {
        return processFullFileName(fullFileName, this.filenameObfuscationKey, false);
    }

    private String processFullFileName(String fullFileName, byte[] key, boolean isEncrypt) {
        List<String> result = new ArrayList<>();
        boolean clearFolder = false;

        for (String part : splitPath(fullFileName)) {
            if (SPECIAL_DIRECTORIES.contains(part)) {
                clearFolder = true;
            }
            result.add(
                    clearFolder
                            ? part
                            : (isEncrypt ? EncryptFileName(part, key) : DecryptFileName(part, key))
            );
        }

        return String.join("/", result);
    }

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
        if (!encryptedFileName.endsWith(String.valueOf(EncryptFileNameEndChar))) {
            return encryptedFileName;
        }

        encryptedFileName = encryptedFileName.substring(0, encryptedFileName.length() - 1);
        if (encryptedFileName.isEmpty()) {
            return encryptedFileName;
        }

        boolean hasLeadingDot = encryptedFileName.startsWith(".");
        String encodedPart = hasLeadingDot ? encryptedFileName.substring(1) : encryptedFileName;
        String namePart = PerformDecryptText(encodedPart, key);
        return (hasLeadingDot ? "." : "") + namePart;
    }

    private String PerformEncryptText(String text, byte[] key) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        byte[] masterKey = concatenate(key, new byte[]{(byte) bytes.length});
        byte[] mask = new byte[0];

        do {
            masterKey = blake2b(masterKey);
            mask = concatenate(mask, masterKey);
        } while (mask.length < bytes.length);

        StringBuilder result = new StringBuilder(bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            int index = (bytes[i] ^ mask[i]) & 0xFF;
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
        byte[] masterKey = concatenate(key, new byte[]{(byte) bytes.length});
        byte[] mask = new byte[0];

        do {
            masterKey = blake2b(masterKey);
            mask = concatenate(mask, masterKey);
        } while (mask.length < bytes.length);

        for (int i = 0; i < bytes.length; i++) {
            byte b = DecryptHelper.get(text.charAt(i));
            bytes[i] = (byte) (b ^ mask[i]);
        }

        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static byte[] hash256(byte[] data) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            return digest.digest(data);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] blake2b(byte[]... arrays) {
        Blake2bDigest digest = new Blake2bDigest(512);
        byte[] input = concatenate(arrays);
        digest.update(input, 0, input.length);
        byte[] result = new byte[digest.getDigestSize()];
        digest.doFinal(result, 0);
        return result;
    }

    private static byte[] blake2bKeyed(byte[] input, byte[] key) {
        Blake2bDigest digest = new Blake2bDigest(key);
        digest.update(input, 0, input.length);
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

    @Override
    public byte[] encryptBytes(byte[] inputBytes, String originalFilename, long timestamp) throws IOException {
        byte[] key = DerivedEncryptionKey(originalFilename, timestamp);
        return processBytes(inputBytes, key);
    }

    @Override
    public byte[] decryptBytes(byte[] encryptedBytes, String originalFilename, long timestamp) throws IOException {
        byte[] key = DerivedEncryptionKey(originalFilename, timestamp);
        return processBytes(encryptedBytes, key);
    }

    private byte[] DerivedEncryptionKey(String clearFullPath, long unixLastWriteTimestampSeconds) {
        return deriveContentKeyFromClearPath(clearFullPath, unixLastWriteTimestampSeconds);
    }

    private byte[] processBytes(byte[] inputBytes, byte[] key) {
        if (key == null || key.length != 64) {
            throw new IllegalArgumentException("Key must be 64 bytes.");
        }

        final int blockSize = 8;
        final int cyclesPerHash = 8;

        byte[] salt = blake2b(key);
        byte[] currentKey = blake2b(salt);

        byte[] result = new byte[inputBytes.length];
        int cycleCounter = 0;

        for (int i = 0; i < inputBytes.length; i += blockSize) {
            int remainingBytes = Math.min(blockSize, inputBytes.length - i);
            long inputBlock = BitConverter.toUInt64(inputBytes, i, remainingBytes);
            long keyBlock = BitConverter.toUInt64(currentKey, cycleCounter * blockSize);

            long outputBlock = inputBlock ^ keyBlock;
            BitConverter.writeBytes(outputBlock, result, i, remainingBytes);

            cycleCounter++;
            if (cycleCounter >= cyclesPerHash) {
                currentKey = blake2bKeyed(currentKey, salt);
                cycleCounter = 0;
            }
        }

        return result;
    }

    private byte[] deriveContentKeyFromClearPath(String clearFullPath, long unixLastWriteTimestampSeconds) {
        String normalizedClearPath = normalizeRelativeUnixPath(clearFullPath);
        String virtualFullPath = processFullFileName(normalizedClearPath, this.filenameObfuscationKey, true);
        byte[] relativeNameBytes = utf16LeBytes(virtualFullPath);
        byte[] pathLengthBytes = BitConverter.getBytes((long) relativeNameBytes.length);
        byte[] dateBytes = BitConverter.getBytes((int) unixLastWriteTimestampSeconds);
        byte[] payload = concatenate(relativeNameBytes, pathLengthBytes, dateBytes, this.encryptionMasterKey);
        return blake2b(payload);
    }

    private static byte[] utf16LeBytes(String text) {
        return text.getBytes(StandardCharsets.UTF_16LE);
    }

    private static String normalizeRelativeUnixPath(String path) {
        return path.replace('\\', '/');
    }

    private static List<String> splitPath(String fullFileName) {
        String normalized = normalizeRelativeUnixPath(fullFileName);
        String[] parts = normalized.split("/");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            if (!part.isEmpty()) {
                result.add(part);
            }
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
            return toUInt64(bytes, 0, bytes.length);
        }

        public static long toUInt64(byte[] bytes, int offset) {
            return toUInt64(bytes, offset, Long.BYTES);
        }

        public static long toUInt64(byte[] bytes, int offset, int length) {
            long value = 0;
            int safeLength = Math.min(length, Long.BYTES);
            for (int i = 0; i < safeLength; i++) {
                value |= ((long) bytes[offset + i] & 0xFF) << (i * Byte.SIZE);
            }
            return value;
        }

        public static void writeBytes(long value, byte[] destination, int offset, int length) {
            for (int i = 0; i < length; i++) {
                destination[offset + i] = (byte) (value >>> (i * Byte.SIZE));
            }
        }
    }
}
