package com.tejas.redis.resp;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class RespWriter {
    private final BufferedOutputStream out;

    public RespWriter(OutputStream out) {
        this.out = new BufferedOutputStream(out);
    }

    public synchronized void writeSimpleString(String s) throws IOException {
        writeBytes("+");
        writeBytes(s);
        writeBytes("\r\n");
        flush();
    }

    public synchronized void writeError(String err) throws IOException {
        writeBytes("-");
        writeBytes(err);
        writeBytes("\r\n");
        flush();
    }

    public synchronized void writeInteger(long v) throws IOException {
        writeBytes(":" + v + "\r\n");
        flush();
    }

    public synchronized void writeBulkString(String s) throws IOException {
        if (s == null) {
            writeBytes("$-1\r\n");
        } else {
            byte[] b = s.getBytes(StandardCharsets.UTF_8);
            writeBytes("$" + b.length + "\r\n");
            out.write(b);
            writeBytes("\r\n");
        }
        flush();
    }

    public synchronized void writeBulkBytes(byte[] b) throws IOException {
        if (b == null) {
            writeBytes("$-1\r\n");
        } else {
            writeBytes("$" + b.length + "\r\n");
            out.write(b);
            writeBytes("\r\n");
        }
        flush();
    }

    private void writeBytes(String s) throws IOException {
        out.write(s.getBytes(StandardCharsets.UTF_8));
    }

    private void flush() throws IOException {
        out.flush();
    }
}
