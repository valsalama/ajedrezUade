package com.chess.adapters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.chess.core.Difficulty;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class ApiServer {

    private static final String SESSION_HEADER = "X-Session-Id";
    private static final String DEFAULT_SESSION = "default";
    private static final Pattern VALID_SESSION_ID = Pattern.compile("[A-Za-z0-9_-]{8,64}");
    private static final long IDLE_MILLIS = 30L * 60 * 1000;
    private static final int MAX_SESSIONS = 500;
    private static final int WORKER_THREADS = 8;

    private final HttpServer server;
    private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();

    public ApiServer(int port) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/state", this::handleState);
        server.createContext("/api/move", this::handleMove);
        server.createContext("/api/reset", this::handleReset);
        server.createContext("/api/undo", this::handleUndo);
        server.createContext("/api/hint", this::handleHint);
        server.createContext("/api/positions", this::handlePositions);
        server.setExecutor(Executors.newFixedThreadPool(WORKER_THREADS));

        ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "session-cleaner");
            thread.setDaemon(true);
            return thread;
        });
        cleaner.scheduleAtFixedRate(this::evictIdleSessions, 5, 5, TimeUnit.MINUTES);
    }

    public void start() {
        server.start();
        System.out.println("API de ajedrez escuchando en http://localhost:" + server.getAddress().getPort());
    }

    public int activeSessions() {
        return sessions.size();
    }

    private GameSession sessionFor(HttpExchange exchange) {
        String id = exchange.getRequestHeaders().getFirst(SESSION_HEADER);
        if (id == null || !VALID_SESSION_ID.matcher(id).matches()) id = DEFAULT_SESSION;

        GameSession existing = sessions.get(id);
        if (existing != null) return existing;

        if (sessions.size() >= MAX_SESSIONS) evictOldestSession();
        return sessions.computeIfAbsent(id, key -> GameSession.create());
    }

    private void evictIdleSessions() {
        long limit = System.currentTimeMillis() - IDLE_MILLIS;
        sessions.entrySet().removeIf(entry -> entry.getValue().getLastUsed() < limit);
    }

    private void evictOldestSession() {
        String oldestKey = null;
        long oldest = Long.MAX_VALUE;
        for (Map.Entry<String, GameSession> entry : sessions.entrySet()) {
            long used = entry.getValue().getLastUsed();
            if (used < oldest) {
                oldest = used;
                oldestKey = entry.getKey();
            }
        }
        if (oldestKey != null) sessions.remove(oldestKey);
    }

    private void handleState(HttpExchange exchange) throws IOException {
        if (withCors(exchange, "GET")) return;
        GameSession session = sessionFor(exchange);
        String json;
        synchronized (session) {
            session.touch();
            json = session.stateJson();
        }
        sendJson(exchange, 200, json);
    }

    private void handleReset(HttpExchange exchange) throws IOException {
        if (withCors(exchange, "POST")) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"method not allowed\"}");
            return;
        }
        String body = readBody(exchange);
        Difficulty requested = extractDifficulty(body);

        GameSession session = sessionFor(exchange);
        String json;
        synchronized (session) {
            session.touch();
            json = session.resetJson(requested);
        }
        sendJson(exchange, 200, json);
    }

    private void handleHint(HttpExchange exchange) throws IOException {
        if (withCors(exchange, "GET")) return;
        GameSession session = sessionFor(exchange);
        String json;
        synchronized (session) {
            session.touch();
            json = session.hintJson();
        }
        sendJson(exchange, 200, json);
    }

    private void handlePositions(HttpExchange exchange) throws IOException {
        if (withCors(exchange, "GET")) return;
        GameSession session = sessionFor(exchange);
        String json;
        synchronized (session) {
            session.touch();
            json = session.positionsJson();
        }
        sendJson(exchange, 200, json);
    }

    private void handleUndo(HttpExchange exchange) throws IOException {
        if (withCors(exchange, "POST")) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"method not allowed\"}");
            return;
        }
        GameSession session = sessionFor(exchange);
        String json;
        synchronized (session) {
            session.touch();
            json = session.undoJson();
        }
        sendJson(exchange, 200, json);
    }

    private void handleMove(HttpExchange exchange) throws IOException {
        if (withCors(exchange, "POST")) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"method not allowed\"}");
            return;
        }

        String body = readBody(exchange);
        int fromRow = extractInt(body, "fromRow");
        int fromCol = extractInt(body, "fromCol");
        int toRow = extractInt(body, "toRow");
        int toCol = extractInt(body, "toCol");
        String promotionName = extractString(body, "promotion");

        GameSession session = sessionFor(exchange);
        String json;
        synchronized (session) {
            session.touch();
            json = session.moveJson(fromRow, fromCol, toRow, toCol, promotionName);
        }
        sendJson(exchange, 200, json);
    }

    private Difficulty extractDifficulty(String body) {
        if (body == null) return null;
        int key = body.indexOf("\"difficulty\"");
        if (key == -1) return null;
        for (Difficulty candidate : Difficulty.values()) {
            if (body.indexOf("\"" + candidate.name() + "\"", key) != -1) return candidate;
        }
        return null;
    }

    private boolean withCors(HttpExchange exchange, String allowedMethod) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", allowedMethod + ", OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, " + SESSION_HEADER);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    private void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[1024];
            int read;
            while ((read = is.read(data)) != -1) buffer.write(data, 0, read);
            return buffer.toString(StandardCharsets.UTF_8);
        }
    }

    private String extractString(String json, String key) {
        if (json == null) return null;
        int keyIndex = json.indexOf("\"" + key + "\"");
        if (keyIndex == -1) return null;
        int colon = json.indexOf(":", keyIndex);
        if (colon == -1) return null;
        int open = json.indexOf("\"", colon);
        if (open == -1) return null;
        int close = json.indexOf("\"", open + 1);
        if (close == -1) return null;
        return json.substring(open + 1, close).trim().toUpperCase();
    }

    private int extractInt(String json, String key) {
        String marker = "\"" + key + "\"";
        int keyIndex = json.indexOf(marker);
        if (keyIndex == -1) return -1;
        int colon = json.indexOf(":", keyIndex);
        if (colon == -1) return -1;
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        try {
            return Integer.parseInt(json.substring(start, end).trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
