package com.tejas.redis.resp;

public final class RespSimpleString implements RespObject {
    private final String value;

    public RespSimpleString(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return "+" + value;
    }
}
