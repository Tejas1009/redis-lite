package com.tejas.redis.resp;

public final class RespInteger implements RespObject {
    private final long value;

    public RespInteger(long value) {
        this.value = value;
    }

    public long value() {
        return value;
    }

    @Override
    public String toString() {
        return ":" + value;
    }
}
