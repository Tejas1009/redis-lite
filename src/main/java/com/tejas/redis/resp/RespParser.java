package com.tejas.redis.resp;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple RESP parser. Reads a single RESP object per call.
 * It uses a BufferedInputStream to allow unread bytes to remain for the next parse.
 */
public class RespParser {

    private final BufferedInputStream in;

    public RespParser(InputStream in) {
        this.in = new BufferedInputStream(in);
    }

    private String readLineCRLF() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int prev = -1;
        while (true) {
            int b = in.read();
            if (b == -1) throw new EOFException("EOF while reading line");
            if (prev == '\r' && b == '\n') {
                byte[] line = baos.toByteArray();
                // remove trailing '\r'
                return new String(line, 0, Math.max(0, line.length - 1), StandardCharsets.UTF_8);
            }
            baos.write(b);
            prev = b;
        }
    }

    public RespObject parse() throws IOException {
        int lead = in.read();
        if (lead == -1) throw new EOFException("Stream closed");
        char t = (char) lead;
        return switch (t) {
            case '+' -> new RespSimpleString(readLineCRLF());
            case '-' -> new RespError(readLineCRLF());
            case ':' -> new RespInteger(Long.parseLong(readLineCRLF()));
            case '$' -> parseBulkString();
            case '*' -> parseArray();
            default -> throw new IOException("Unknown RESP type: " + t);
        };
    }

    private RespBulkString parseBulkString() throws IOException {
        String lenLine = readLineCRLF();
        int len = Integer.parseInt(lenLine);
        if (len == -1) return new RespBulkString(null);
        byte[] data = new byte[len];
        int read = 0;
        while (read < len) {
            int r = in.read(data, read, len - read);
            if (r == -1) throw new EOFException("EOF while reading bulk string data");
            read += r;
        }
        // consume CRLF
        int r1 = in.read();
        int r2 = in.read();
        if (r1 != '\r' || r2 != '\n') throw new IOException("Missing CRLF after bulk string");
        return new RespBulkString(data);
    }

    private RespArray parseArray() throws IOException {
        String nLine = readLineCRLF();
        int n = Integer.parseInt(nLine);
        if (n == -1) return new RespArray(null);
        List<RespObject> items = new ArrayList<>(n);
        for (int i = 0; i < n; i++) items.add(parse());
        return new RespArray(items);
    }
}
