package com.graphenelab.communication.crypto;

import static com.graphenelab.communication.crypto.ConversionUtils.byteArrayToIntLE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;




public class CryptoUtils {

    // Strip leading zero byte if present
    public static byte[] stripLeadingZero(byte[] input) {
        if (input.length > 0 && input[0] == 0) {
            return Arrays.copyOfRange(input, 1, input.length);
        }
        return input;
    }

    public static List<byte[]> splitData(byte[] data) {
        List<byte[]> datas = new ArrayList<>();
        int offset = 0;

        while (offset < data.length) {
            // Read 4 bytes as integer (big-endian)
            int len = byteArrayToIntLE(data, offset);
            offset += 4;

            // Extract 'len' bytes
            byte[] part = Arrays.copyOfRange(data, offset, offset + len);
            datas.add(part);
            offset += len;
        }

        return datas;
    }

    public static void alertBox(String message) {
        System.out.println("Alert: " + message);
        // Replace with  UI mechanism
    }
}
