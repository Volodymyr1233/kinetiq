package com.kinetiq.account.domain.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;

/** The secret in a confirmation link. Only its hash is stored. */
public record ConfirmationToken(String value) {

    private static final SecureRandom RANDOM = new SecureRandom();

    public ConfirmationToken {
        Objects.requireNonNull(value);
    }

    public static ConfirmationToken generate() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return new ConfirmationToken(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
    }

    // 256 random bits need no salt or slow hash: a copy of the table cannot be turned back into links
    public String hash() {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("every JDK provides SHA-256", e);
        }
    }

    @Override
    public String toString() {
        return "***";
    }
}
