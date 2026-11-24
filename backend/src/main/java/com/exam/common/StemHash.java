package com.exam.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class StemHash {

    private StemHash() {
    }

    public static String normalize(String stem) {
        if (stem == null) {
            return "";
        }
        return stem.trim().replaceAll("\\s+", " ");
    }

    public static String of(String stem) {
        String normalized = normalize(stem);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
