package kr.chuyong;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

public class FileUtils {
    public static String extractFileHashSHA256(File jarFile) throws Exception {
        MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");

        try (FileInputStream inputStream = new FileInputStream(jarFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                sha256Digest.update(buffer, 0, bytesRead);
            }
        }

        byte[] sha256Bytes = sha256Digest.digest();
        StringBuilder sha256Hex = new StringBuilder();
        for (byte b : sha256Bytes) {
            sha256Hex.append(String.format("%02x", b));
        }

        return sha256Hex.toString();
    }
}
