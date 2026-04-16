package com.graphenelab.communication.crypto;

import java.io.File;
import java.io.IOException;

public interface IZeroKnowledgeProof {

    void EncryptFile(File inputFile, String outputFile) throws IOException;

    default void EncryptFile(File inputFile, String outputFile, String virtualRelativeName) throws IOException {
        EncryptFile(inputFile, outputFile);
    }

    void DecryptFile(File inputFile, String outputFile) throws IOException;

    default void DecryptFile(File inputFile, String outputFile, String virtualRelativeName) throws IOException {
        DecryptFile(inputFile, outputFile);
    }

    String EncryptFullFileName(String fullFileName);

    String DecryptFullFileName(String fullFileName);

    byte[] encryptBytes(byte[] inputBytes, String originalFilename, long timestamp) throws IOException;

    byte[] decryptBytes(byte[] encryptedBytes, String originalFilename, long timestamp) throws IOException;
}
