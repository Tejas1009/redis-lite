package com.tejas.redis.resp;

public final class RespError implements RespObject {
    private final String message;

    public RespError(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }

    @Override
    public String toString() {
        return "-" + message;
    }
}
