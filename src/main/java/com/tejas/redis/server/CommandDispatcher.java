package com.tejas.redis.server;

import com.tejas.redis.resp.*;
import com.tejas.redis.store.DataStore;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Dispatcher for RESP commands that operate on a DataStore.
 * Supports: PING, ECHO, SET, GET (minimal).
 */
public final class CommandDispatcher {

    private final DataStore store;

    public CommandDispatcher(DataStore store) {
        this.store = store;
    }

    /**
     * Dispatch an array-of-resp-objects (the command + args) and return a RespObject response.
     */
    public RespObject dispatch(List<RespObject> parts) {
        if (parts == null || parts.isEmpty()) return new RespError("ERR empty command");
        RespObject maybeCmd = parts.get(0);
        if (!(maybeCmd instanceof RespBulkString cmdBs)) return new RespError("ERR expected bulk string for command");
        Optional<String> cmdOpt = cmdBs.asString();
        if (cmdOpt.isEmpty()) return new RespError("ERR empty command");
        String cmd = cmdOpt.get().toUpperCase(Locale.ROOT);

        try {
            return switch (cmd) {
                case "PING" -> handlePing(parts);
                case "ECHO" -> handleEcho(parts);
                case "SET" -> handleSet(parts);
                case "GET" -> handleGet(parts);
                default -> new RespError("ERR unknown command '" + cmd + "'");
            };
        } catch (IllegalArgumentException iae) {
            return new RespError("ERR " + iae.getMessage());
        } catch (Exception e) {
            return new RespError("ERR internal error");
        }
    }

    private RespObject handlePing(List<RespObject> parts) {
        if (parts.size() == 1) return new RespSimpleString("PONG");
        RespObject arg = parts.get(1);
        if (!(arg instanceof RespBulkString bs)) return new RespError("ERR expected bulk string as PING message");
        return new RespBulkString(bs.data());
    }

    private RespObject handleEcho(List<RespObject> parts) {
        if (parts.size() != 2) return new RespError("ERR wrong number of arguments for 'ECHO' command");
        RespObject arg = parts.get(1);
        if (!(arg instanceof RespBulkString bs)) return new RespError("ERR expected bulk string for ECHO");
        return new RespBulkString(bs.data());
    }

    /**
     * SET key value [EX seconds] [PX milliseconds] [EXAT unix-seconds] [PXAT unix-ms]
     * <p>
     * We only implement expiry options here (no NX/XX/KEEPTTL/GET).
     * If multiple expiry options appear, the last one wins.
     */
    private RespObject handleSet(List<RespObject> parts) {
        if (parts.size() < 3) return new RespError("ERR wrong number of arguments for 'SET' command");

        // key and value
        RespObject k = parts.get(1);
        RespObject v = parts.get(2);
        if (!(k instanceof RespBulkString keyBs)) return new RespError("ERR expected bulk string for key");
        if (!(v instanceof RespBulkString valBs)) return new RespError("ERR expected bulk string for value");

        String key = keyBs.asString().orElse("");
        byte[] value = valBs.data(); // may be null -> treat as delete

        // parse optional args
        long expiresAtMs = 0; // 0 means no expiry
        int idx = 3;
        while (idx < parts.size()) {
            RespObject p = parts.get(idx);
            if (!(p instanceof RespBulkString optBs)) return new RespError("ERR expected bulk string for option");
            String opt = optBs.asString().orElse("").toUpperCase(Locale.ROOT);

            try {
                switch (opt) {
                    case "EX" -> {
                        // relative seconds
                        idx++;
                        if (idx >= parts.size()) return new RespError("ERR syntax error: EX needs an argument");
                        RespObject arg = parts.get(idx);
                        if (!(arg instanceof RespBulkString argBs))
                            return new RespError("ERR expected bulk string for EX argument");
                        long seconds = Long.parseLong(argBs.asString().orElseThrow());
                        long now = System.currentTimeMillis();
                        expiresAtMs = now + seconds * 1000L;
                    }
                    case "PX" -> {
                        idx++;
                        if (idx >= parts.size()) return new RespError("ERR syntax error: PX needs an argument");
                        RespObject arg = parts.get(idx);
                        if (!(arg instanceof RespBulkString argBs))
                            return new RespError("ERR expected bulk string for PX argument");
                        long ms = Long.parseLong(argBs.asString().orElseThrow());
                        long now = System.currentTimeMillis();
                        expiresAtMs = now + ms;
                    }
                    case "EXAT" -> {
                        idx++;
                        if (idx >= parts.size()) return new RespError("ERR syntax error: EXAT needs an argument");
                        RespObject arg = parts.get(idx);
                        if (!(arg instanceof RespBulkString argBs))
                            return new RespError("ERR expected bulk string for EXAT argument");
                        long unixSeconds = Long.parseLong(argBs.asString().orElseThrow());
                        expiresAtMs = unixSeconds * 1000L;
                    }
                    case "PXAT" -> {
                        idx++;
                        if (idx >= parts.size()) return new RespError("ERR syntax error: PXAT needs an argument");
                        RespObject arg = parts.get(idx);
                        if (!(arg instanceof RespBulkString argBs))
                            return new RespError("ERR expected bulk string for PXAT argument");
                        long unixMs = Long.parseLong(argBs.asString().orElseThrow());
                        expiresAtMs = unixMs;
                    }
                    default -> {
                        // for now we don't support other options (NX/XX/KEEPTTL/GET); return syntax error
                        return new RespError("ERR unsupported option '" + opt + "' in SET");
                    }
                }
            } catch (NumberFormatException nfe) {
                return new RespError("ERR value is not an integer or out of range");
            }
            idx++;
        }

        // commit to store. DataStore handles immediate-expiry logic.
        store.set(key, value, expiresAtMs);
        return new RespSimpleString("OK");
    }


    /**
     * GET key
     * Returns bulk string with value or null bulk ($-1) if absent.
     */
    private RespObject handleGet(List<RespObject> parts) {
        if (parts.size() != 2) return new RespError("ERR wrong number of arguments for 'GET' command");
        RespObject k = parts.get(1);
        if (!(k instanceof RespBulkString keyBs)) return new RespError("ERR expected bulk string for key");

        Optional<String> keyOpt = keyBs.asString();
        String key = keyOpt.orElse("");
        Optional<byte[]> val = store.get(key);
        if (val.isEmpty()) return new RespBulkString(null); // $-1
        return new RespBulkString(val.get());
    }
}
