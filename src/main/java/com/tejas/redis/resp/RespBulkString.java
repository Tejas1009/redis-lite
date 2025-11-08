package com.tejas.redis.resp;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public final class RespBulkString implements RespObject {
    // data == null means RESP null bulk string ($-1)
    private final byte[] data;

    public RespBulkString(byte[] data) {
        this.data = data;
    }

    public Optional<String> asString() {
        return data == null ? Optional.empty() : Optional.of(new String(data, StandardCharsets.UTF_8));
    }

    public byte[] data() {
        return data;
    }

    @Override
    public String toString() {
        return data == null ? "$-1" : "$" + data.length + " " + asString().orElse("");
    }
}
