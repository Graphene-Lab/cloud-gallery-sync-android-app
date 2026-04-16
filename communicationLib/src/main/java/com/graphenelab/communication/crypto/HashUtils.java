package com.graphenelab.communication.crypto;

import static com.graphenelab.communication.crypto.ConversionUtils.resizeBuffers;
import static com.graphenelab.communication.crypto.ConversionUtils.toByteArray;
import static com.graphenelab.communication.crypto.ConversionUtils.toIntArray;

import java.security.MessageDigest;


public class HashUtils {

    // SHA-256 hash
    public static byte[] hash256(byte[] buffer) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(buffer);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    public static byte[] fastHash256(byte[] input) {
        int bl = input.length;
        int newSize = ((bl + 31) / 32) * 32;
        byte[] bytes = resizeBuffers(input, newSize);
        int[] data = toIntArray(bytes);

        int p0 = 0b01010101_01010101_01010101_01010101;
        int p1 = 0b00110011_00110011_00110011_00110011;
        int p2 = 0b00100100_10010010_00100100_10010010;
        int p3 = 0b00011100_01110001_11000111_00011100;
        int p4 = ~p0;
        int p5 = ~p1;
        int p6 = ~p2;
        int p7 = ~p3;

        int x = bl ^ 0x55555555;
        x = x ^ (x << (1 + bl % 30));
        x = x ^ 0x55555555;
        x = x ^ (x >> (1 + bl % 29));

        int i = 0;
        while (i < data.length) {
            int v0 = getSafe(data, i);
            int v1 = getSafe(data, i + 1);
            int v2 = getSafe(data, i + 2);
            int v3 = getSafe(data, i + 3);
            int v4 = getSafe(data, i + 4);
            int v5 = getSafe(data, i + 5);
            int v6 = getSafe(data, i + 6);
            int v7 = getSafe(data, i + 7);

            x = x ^ (v0 ^ v1 ^ v2 ^ v3 ^ v4 ^ v5 ^ v6 ^ v7);
            x = x ^ 0x55555555;
            x = x ^ (x << (1 + x % 28));
            x = x ^ 0x55555555;
            x = x ^ (x >> (1 + x % 29));
            x = x ^ 0x55555555;
            x = x ^ (x << (1 + x % 30));

            p0 = p0 ^ v0 ^ x;
            p1 = p1 ^ v1 ^ x;
            p2 = p2 ^ v2 ^ x;
            p3 = p3 ^ v3 ^ x;
            p4 = p4 ^ v4 ^ x;
            p5 = p5 ^ v5 ^ x;
            p6 = p6 ^ v6 ^ x;
            p7 = p7 ^ v7 ^ x;

            i += 8;
        }

        int[] result = new int[] { p0, p1, p2, p3, p4, p5, p6, p7 };
        return toByteArray(result);
    }

    private static int getSafe(int[] data, int index) {
        return (index < data.length) ? data[index] : 0;
    }




}
