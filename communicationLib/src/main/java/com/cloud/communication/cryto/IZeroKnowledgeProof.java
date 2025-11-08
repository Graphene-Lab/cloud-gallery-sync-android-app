package com.cloud.communication.cryto;

import java.io.File;
import java.io.IOException;

public interface IZeroKnowledgeProof {

    void EncryptFile(File inputFile, String outputFile) throws IOException;

    void DecryptFile(File inputFile, String outputFile) throws IOException;

    String EncryptFullFileName(String fullFileName);

    String DecryptFullFileName(String fullFileName);

    byte[] encryptBytes(byte[] inputBytes, String originalFilename, long timestamp) throws IOException;

    byte[] decryptBytes(byte[] encryptedBytes, String originalFilename, long timestamp) throws IOException;
}
