package com.tejas.redis.store;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Value container: stores bytes and expiry timestamp (ms since epoch).
 * expiresAt == 0 means no expiry.
 */
public final class Value implements Serializable {
    private static final long serialVersionUID = 1L;
    private final byte[] data;
    private final long expiresAt; // epoch ms; 0 means no expiry

    public Value(byte[] data, long expiresAt) {
        this.data = data;
        this.expiresAt = expiresAt;
    }

    public byte[] data() {
        return data;
    }

    public long expiresAt() {
        return expiresAt;
    }

    public Optional<String> asString() {
        return data == null ? Optional.empty() : Optional.of(new String(data, StandardCharsets.UTF_8));
    }

    public boolean isExpired(long nowMs) {
        return expiresAt > 0 && nowMs >= expiresAt;
    }
}
