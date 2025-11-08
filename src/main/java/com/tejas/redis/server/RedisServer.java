package com.tejas.redis.server;

import com.tejas.redis.resp.*;
import com.tejas.redis.store.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Small RESP-aware TCP server. For now supports PING and ECHO commands over RESP.
 */
public class RedisServer {

    private static final Logger log = LoggerFactory.getLogger(RedisServer.class);

    private final int port;
    private final ExecutorService clientPool;
    private volatile boolean running = true;
    private ServerSocket serverSocket;
    private final DataStore store;

    public RedisServer(int port) {
        this.port = port;
        this.clientPool = Executors.newCachedThreadPool();
        this.store = new DataStore(); // single shared store
    }

    public void start() {
        try (ServerSocket ss = new ServerSocket(port)) {
            this.serverSocket = ss;
            log.info("RESP server listening on port {}", port);

            while (running) {
                try {
                    Socket s = ss.accept();
                    s.setSoTimeout(30000);
                    log.info("Client connected: {}", s.getRemoteSocketAddress());
                    clientPool.submit(new ClientHandler(s, store));
                } catch (SocketException se) {
                    if (running) log.error("Socket exception: {}", se.getMessage(), se);
                    break;
                }
            }
        } catch (IOException e) {
            log.error("Failed to start server on port {}: {}", port, e.getMessage(), e);
        } finally {
            shutdown();
        }
    }

    private void shutdown() {
        running = false;
        log.info("Shutting down RedisServer...");
        clientPool.shutdown();
        try {
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (IOException e) {
            log.warn("Error closing server socket: {}", e.getMessage());
        }
        try {
            if (!clientPool.awaitTermination(2, TimeUnit.SECONDS)) clientPool.shutdownNow();
        } catch (InterruptedException ie) {
            log.error("Interrupted during shutdown", ie);
            Thread.currentThread().interrupt();
        }
        log.info("Shutdown complete.");
    }

    private static class ClientHandler implements Runnable {
        private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);
        private final Socket socket;
        private final CommandDispatcher dispatcher;

        // constructor receives DataStore
        ClientHandler(Socket socket, DataStore store) {
            this.socket = socket;
            this.dispatcher = new CommandDispatcher(store);
        }

        @Override
        public void run() {
            try (InputStream in = socket.getInputStream(); OutputStream out = socket.getOutputStream()) {
                RespParser parser = new RespParser(in);
                RespWriter writer = new RespWriter(out);

                while (!socket.isClosed()) {
                    RespObject obj;
                    try {
                        obj = parser.parse();
                    } catch (EOFException eof) {
                        log.debug("Client closed connection: {}", socket.getRemoteSocketAddress());
                        break;
                    }

                    if (!(obj instanceof RespArray arr)) {
                        writer.writeError("ERR expected array of bulk strings");
                        continue;
                    }

                    List<RespObject> items = arr.items().orElse(null);
                    if (items == null) {
                        writer.writeError("ERR null array not supported");
                        continue;
                    }

                    // Use dispatcher instance that references the shared DataStore
                    RespObject resp = dispatcher.dispatch(items);

                    // existing response mapping code here...
                    if (resp instanceof RespSimpleString s) writer.writeSimpleString(s.value());
                    else if (resp instanceof RespError e) writer.writeError(e.message());
                    else if (resp instanceof RespInteger i) writer.writeInteger(i.value());
                    else if (resp instanceof RespBulkString b) writer.writeBulkBytes(b.data());
                    else writer.writeError("ERR unsupported response type");
                }
            } catch (IOException e) {
                log.warn("IO error with client {}: {}", socket.getRemoteSocketAddress(), e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
                log.info("Client disconnected: {}", socket.getRemoteSocketAddress());
            }
        }
    }

    public static void main(String[] args) {
        int port = 6379;
        new RedisServer(port).start();
    }
}
