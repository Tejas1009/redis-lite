package com.tejas.redis.resp;

import java.util.List;
import java.util.Optional;

public final class RespArray implements RespObject {
    // items == null means null array (*-1)
    private final List<RespObject> items;

    public RespArray(List<RespObject> items) {
        this.items = items;
    }

    public Optional<List<RespObject>> items() {
        return Optional.ofNullable(items);
    }

    @Override
    public String toString() {
        return items == null ? "*-1" : "*" + items.size();
    }
}
