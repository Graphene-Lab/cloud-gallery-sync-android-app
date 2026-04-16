package com.graphenelab.communication.crypto;

import java.util.Base64;

public class ConversionUtils {
    // Convert byte array to int array (4 bytes per int)
    public static long[] toLongArray(byte[] input) {
        int length = input.length / 4; // each int in the result is made up of 4 bytes
        long[] result = new long[length];

        for (int i = 0; i < length; i++) {
            long b0 = input[i * 4] & 0xFF;
            long b1 = input[i * 4 + 1] & 0xFF;
            long b2 = input[i * 4 + 2] & 0xFF;
            long b3 = input[i * 4 + 3] & 0xFF;

            result[i] = (b0) | (b1 << 8) | (b2 << 16) | (b3 << 24);
        }

        return result;
    }

    // Convert int array to byte array
    public static byte[] toByteArray(long[] input) {
        byte[] result = new byte[input.length * 4];
        for (int i = 0; i < input.length; i++) {
            long value = input[i];
            result[i * 4] = (byte) (value & 0xFF);
            result[i * 4 + 1] = (byte) ((value >> 8) & 0xFF);
            result[i * 4 + 2] = (byte) ((value >> 16) & 0xFF);
            result[i * 4 + 3] = (byte) ((value >> 24) & 0xFF);
        }
        return result;
    }

    public static int[] toIntArray(byte[] bytes) {
        int length = bytes.length / 4;
        int[] result = new int[length];
        for (int i = 0; i < length; i++) {
            result[i] = (bytes[i * 4] & 0xFF) |
                    ((bytes[i * 4 + 1] & 0xFF) << 8) |
                    ((bytes[i * 4 + 2] & 0xFF) << 16) |
                    ((bytes[i * 4 + 3] & 0xFF) << 24);
        }
        return result;
    }

    public static byte[] toByteArray(int[] ints) {
        byte[] result = new byte[ints.length * 4];
        for (int i = 0; i < ints.length; i++) {
            int value = ints[i];
            result[i * 4] = (byte) (value & 0xFF);
            result[i * 4 + 1] = (byte) ((value >>> 8) & 0xFF);
            result[i * 4 + 2] = (byte) ((value >>> 16) & 0xFF);
            result[i * 4 + 3] = (byte) ((value >>> 24) & 0xFF);
        }
        return result;
    }

    public static byte[] int16ToBuffer(int value) {
        return numberToBuffer(value, 2);
    }

    public static byte[] int32ToBuffer(int value) {
        return numberToBuffer(value, 4);
    }


    public static byte[] base64ToBuffer(String base64String) {
        return Base64.getDecoder().decode(base64String);
    }

    public static byte[] joinBuffers(byte[] buffer1, byte[] buffer2) {
        byte[] tmp = new byte[buffer1.length + buffer2.length];
        System.arraycopy(buffer1, 0, tmp, 0, buffer1.length);
        System.arraycopy(buffer2, 0, tmp, buffer1.length, buffer2.length);
        return tmp;
    }

    public static byte[] resizeBuffers(byte[] buffer, int newSize) {
        byte[] tmp = new byte[newSize];
        System.arraycopy(buffer, 0, tmp, 0, Math.min(buffer.length, newSize));
        return tmp;
    }


    private static byte[] numberToBuffer(long number, int returnBytes) {
        byte[] byteBuffer = new byte[returnBytes];
        long num = number;
        for (int index = 0; index < returnBytes; index++) {
            int byteVal = (int) (num & 0xff);
            byteBuffer[index] = (byte) byteVal;
            num = (num - byteVal) / 256;
        }
        return byteBuffer;
    }


    // Base64Url encoding
    public static byte[] base64UrlDecode(String base64Url) {
        return Base64.getUrlDecoder().decode(base64Url);
    }

    public static String base64UrlEncode(byte[] input) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(input);
    }

    // Convert byte array to hex string
    public static String byteArrayToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }


    // Helper function to convert 4 bytes to int
    public static int byteArrayToIntLE(byte[] data, int offset) {
        return ((data[offset] & 0xFF)) |
                ((data[offset + 1] & 0xFF) << 8) |
                ((data[offset + 2] & 0xFF) << 16) |
                ((data[offset + 3] & 0xFF) << 24);
    }

    public static String bufferToString(byte[] buffer) {
        return new String(buffer);
    }
}
