package com.graphenelab.communication.crypto.encryption;

import java.util.Arrays;

import static com.graphenelab.communication.crypto.ConversionUtils.resizeBuffers;
import static com.graphenelab.communication.crypto.ConversionUtils.toByteArray;
import static com.graphenelab.communication.crypto.ConversionUtils.toLongArray;
import static com.graphenelab.communication.crypto.HashUtils.fastHash256;

public class XorEncryption {
    //Encryption
    public static byte[] decryptXorAB(byte[] key, byte[] data) {
        return encryptXorAB(key, data);
    }

    // XOR-AB encryption
    public static byte[] encryptXorAB(byte[] key, byte[] data) {
        byte[] tmpKey = Arrays.copyOf(key, key.length);
        int dl = data.length;
        int newSize = ((dl + 3) / 4) * 4;  // round up to multiple of 4

        byte[] resizedData = resizeBuffers(data, newSize);
        long[] dt = toLongArray(resizedData);

        if (tmpKey.length < 4) {
            tmpKey = resizeBuffers(tmpKey, 4);
        }

        long[] k = toLongArray(tmpKey);
        k[0] = k[0] ^ dl;
        tmpKey = toByteArray(k);

        long[] target = new long[dt.length];

        for (int i = 0; i < dt.length; i++) {
            int p = i % k.length;
            if (p == 0) {
                tmpKey = fastHash256(tmpKey);
                k = toLongArray(tmpKey);
            }
            target[i] = dt[i] ^ k[p];
        }

        byte[] result = toByteArray(target);
        return Arrays.copyOf(result, dl);
    }
}
