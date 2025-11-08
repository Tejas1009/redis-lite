package com.tejas.redis.store;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe key-value store with optional expiry (epoch ms).
 * Expired keys are removed lazily on access.
 */
public final class DataStore {

    private final ConcurrentHashMap<String, Value> map = new ConcurrentHashMap<>();

    public DataStore() {
    }

    /**
     * Set key -> value with optional expiry timestamp (epoch ms). If value==null we treat it as removal.
     * <p>
     * If expiresAtMs <= now the method will not store the value (immediate expiry).
     */
    public void set(String key, byte[] value, long expiresAtMs) {
        if (key == null) throw new IllegalArgumentException("key cannot be null");
        if (value == null) {
            map.remove(key);
            return;
        }
        long now = System.currentTimeMillis();
        if (expiresAtMs > 0 && expiresAtMs <= now) {
            // immediate expiry: do not store
            map.remove(key);
            return;
        }
        map.put(key, new Value(value, expiresAtMs));
    }

    /**
     * Get value if present and not expired. Removes expired key lazily.
     */
    public Optional<byte[]> get(String key) {
        if (key == null) return Optional.empty();
        Value v = map.get(key);
        if (v == null) return Optional.empty();
        long now = System.currentTimeMillis();
        if (v.isExpired(now)) {
            map.remove(key, v);
            return Optional.empty();
        }
        return Optional.of(v.data());
    }

    /**
     * Return the raw Value object (for internal use), removing it if expired.
     */
    public Optional<Value> getValue(String key) {
        if (key == null) return Optional.empty();
        Value v = map.get(key);
        if (v == null) return Optional.empty();
        long now = System.currentTimeMillis();
        if (v.isExpired(now)) {
            map.remove(key, v);
            return Optional.empty();
        }
        return Optional.of(v);
    }

    /**
     * Delete a key; return true if existed.
     */
    public boolean del(String key) {
        return map.remove(key) != null;
    }

    /**
     * Optional: background scan to remove expired keys (useful for long-lived idle keys).
     * Not required, but can be scheduled by the server if desired.
     */
    public void sweepExpired() {
        long now = System.currentTimeMillis();
        for (var entry : map.entrySet()) {
            Value v = entry.getValue();
            if (v != null && v.isExpired(now)) {
                map.remove(entry.getKey(), v);
            }
        }
    }
}
