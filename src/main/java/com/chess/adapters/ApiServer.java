package com.chess.adapters;

import com.chess.core.*;
import com.chess.ports.ChessBoard;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;


public class ApiServer {

    private final GameManager gameManager;
    private final ChessBoard board;
    private final CheckDetector checkDetector = new CheckDetector();
    private final CheckmateDetector checkmateDetector = new CheckmateDetector();
    private final HttpServer server;

    public ApiServer(GameManager gameManager, ChessBoard board, int port) throws IOException {
        this.gameManager = gameManager;
        this.board = board;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/state", this::handleState);
        server.createContext("/api/move", this::handleMove);
        server.createContext("/api/reset", this::handleReset);
        server.setExecutor(null);
    }

    public void start() {
        server.start();
        System.out.println("API de ajedrez escuchando en http://localhost:" + server.getAddress().getPort());
    }

    // ---------- handlers ----------

    private void handleState(HttpExchange exchange) throws IOException {
        if (withCors(exchange, "GET")) return;
        sendJson(exchange, 200, buildStateJson());
    }

    private void handleReset(HttpExchange exchange) throws IOException {
        if (withCors(exchange, "POST")) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"method not allowed\"}");
            return;
        }
        resetBoard();
        sendJson(exchange, 200, buildStateJson());
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

        Position from = new Position(fromRow, fromCol);
        Position to = new Position(toRow, toCol);

        MoveResult result = gameManager.tryMove(from, to);

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"result\":\"").append(result).append("\",");
        json.append("\"state\":").append(buildStateJson());
        json.append("}");

        sendJson(exchange, 200, json.toString());
    }

    // ---------- JSON building ----------

    private String buildStateJson() {
        StringBuilder json = new StringBuilder();
        json.append("{");

        Color turn = gameManager.getCurrentTurn();
        boolean inCheck = checkDetector.isInCheck(turn, board);
        boolean isCheckmate = inCheck && checkmateDetector.isCheckmate(turn, board);

        GameState state = isCheckmate ? GameState.CHECKMATE
                : inCheck ? GameState.CHECK
                : GameState.IN_PROGRESS;

        json.append("\"turn\":\"").append(turn).append("\",");
        json.append("\"gameState\":\"").append(state).append("\",");

        json.append("\"board\":[");
        for (int row = 0; row < 8; row++) {
            if (row > 0) json.append(",");
            json.append("[");
            for (int col = 0; col < 8; col++) {
                if (col > 0) json.append(",");
                Piece piece = board.getPieceAt(new Position(row, col));
                json.append(pieceJson(piece));
            }
            json.append("]");
        }
        json.append("],");

        json.append("\"capturedWhite\":").append(capturedJson(Color.WHITE)).append(",");
        json.append("\"capturedBlack\":").append(capturedJson(Color.BLACK));

        json.append("}");
        return json.toString();
    }

    private String pieceJson(Piece piece) {
        if (piece == null) return "null";
        return "{\"type\":\"" + piece.getType() + "\",\"color\":\"" + piece.getColor() + "\"}";
    }

    private String capturedJson(Color color) {
        List<Piece> captured = gameManager.getCapturedPieces(color);
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < captured.size(); i++) {
            if (i > 0) json.append(",");
            json.append("\"").append(captured.get(i).getType()).append("\"");
        }
        json.append("]");
        return json.toString();
    }

    private void resetBoard() {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                board.removePiece(new Position(r, c));
            }
        }
        placeInitialPieces();
    }

    private void placeInitialPieces() {
        place(new Rook(Color.WHITE, p(0, 0)), 0, 0);
        place(new Knight(Color.WHITE, p(0, 1)), 0, 1);
        place(new Bishop(Color.WHITE, p(0, 2)), 0, 2);
        place(new Queen(Color.WHITE, p(0, 3)), 0, 3);
        place(new King(Color.WHITE, p(0, 4)), 0, 4);
        place(new Bishop(Color.WHITE, p(0, 5)), 0, 5);
        place(new Knight(Color.WHITE, p(0, 6)), 0, 6);
        place(new Rook(Color.WHITE, p(0, 7)), 0, 7);
        for (int i = 0; i < 8; i++) place(new Pawn(Color.WHITE, p(1, i)), 1, i);

        place(new Rook(Color.BLACK, p(7, 0)), 7, 0);
        place(new Knight(Color.BLACK, p(7, 1)), 7, 1);
        place(new Bishop(Color.BLACK, p(7, 2)), 7, 2);
        place(new Queen(Color.BLACK, p(7, 3)), 7, 3);
        place(new King(Color.BLACK, p(7, 4)), 7, 4);
        place(new Bishop(Color.BLACK, p(7, 5)), 7, 5);
        place(new Knight(Color.BLACK, p(7, 6)), 7, 6);
        place(new Rook(Color.BLACK, p(7, 7)), 7, 7);
        for (int i = 0; i < 8; i++) place(new Pawn(Color.BLACK, p(6, i)), 6, i);
    }

    private Position p(int r, int c) { return new Position(r, c); }
    private void place(Piece piece, int r, int c) { board.placePiece(piece, new Position(r, c)); }

    // ---------- small HTTP helpers ----------

    /** Adds CORS headers and answers OPTIONS preflight. Returns true if the exchange was already handled. */
    private boolean withCors(HttpExchange exchange, String allowedMethod) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", allowedMethod + ", OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
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

    /** Minimal number extractor for flat JSON bodies like {"fromRow":1,"fromCol":0}. Good enough for this API's needs. */
    private int extractInt(String json, String key) {
        String marker = "\"" + key + "\"";
        int keyIndex = json.indexOf(marker);
        if (keyIndex == -1) return -1;
        int colon = json.indexOf(":", keyIndex);
        int start = colon + 1;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        return Integer.parseInt(json.substring(start, end).trim());
    }
}