package com.chess.adapters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.chess.core.Bishop;
import com.chess.core.CheckDetector;
import com.chess.core.Color;
import com.chess.core.DrawReason;
import com.chess.core.GameManager;
import com.chess.core.GameState;
import com.chess.core.King;
import com.chess.core.Knight;
import com.chess.core.Move;
import com.chess.core.MoveResult;
import com.chess.core.Pawn;
import com.chess.core.Piece;
import com.chess.core.Position;
import com.chess.core.Queen;
import com.chess.core.Rook;
import com.chess.ports.ChessBoard;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class ApiServer {

    private final GameManager gameManager;
    private final ChessBoard board;
    private final AIStrategy aiStrategy;
    private final CheckDetector checkDetector = new CheckDetector();
    private final HttpServer server;
    private static final Color HUMAN_COLOR = Color.WHITE;

    public ApiServer(GameManager gameManager, ChessBoard board, AIStrategy aiStrategy, int port) throws IOException {
        this.gameManager = gameManager;
        this.board = board;
        this.aiStrategy = aiStrategy;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/state", this::handleState);
        server.createContext("/api/move", this::handleMove);
        server.createContext("/api/reset", this::handleReset);
        server.createContext("/api/undo", this::handleUndo);
        server.setExecutor(null);
    }

    public void start() {
        server.start();
        System.out.println("API de ajedrez escuchando en http://localhost:" + server.getAddress().getPort());
    }

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

    private void handleUndo(HttpExchange exchange) throws IOException {
        if (withCors(exchange, "POST")) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"method not allowed\"}");
            return;
        }

        // Una sola vuelta atrás por click: la respuesta de la IA y la jugada
        // tuya que la provocó (o solo la tuya si la IA no llegó a responder).
        gameManager.undoLastRound(HUMAN_COLOR);

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

        MoveResult result;
        boolean gaveCheck = false;

        if (!inBounds(fromRow, fromCol) || !inBounds(toRow, toCol)) {
            // Coordenadas faltantes o fuera del tablero: se rechaza sin tocar el juego
            result = MoveResult.INVALID_MOVE;
        } else {
            Position from = new Position(fromRow, fromCol);
            Position to = new Position(toRow, toCol);

            result = gameManager.tryMove(from, to);

            gaveCheck = result == MoveResult.SUCCESS
                    && gameManager.getCurrentTurn() == Color.BLACK
                    && checkDetector.isInCheck(Color.BLACK, board);

            // La IA solo responde si la partida sigue (no hubo mate ni tablas)
            if (result == MoveResult.SUCCESS && gameManager.getCurrentTurn() == Color.BLACK && !isGameOver()) {
                Move aiMove = pickLegalAiMove();
                if (aiMove != null) {
                    gameManager.tryMove(aiMove.getFrom(), aiMove.getTo());
                }
            }
        }

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"result\":\"").append(result).append("\",");
        json.append("\"gaveCheck\":").append(gaveCheck).append(",");
        json.append("\"state\":").append(buildStateJson());
        json.append("}");

        sendJson(exchange, 200, json.toString());
    }

    private boolean inBounds(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }

    private boolean isGameOver() {
        GameState state = gameManager.getGameState();
        return state == GameState.CHECKMATE || state == GameState.DRAW;
    }

    /**
     * Elige la jugada de la IA entre las jugadas legales que da el GameManager
     * (así incluye enroque, peón al paso y coronación): si puede comer, come;
     * si no, juega una al azar.
     */
    private Move pickLegalAiMove() {
        List<Move> legalCaptures = new java.util.ArrayList<>();
        List<Move> legalOthers = new java.util.ArrayList<>();

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Position from = new Position(r, c);
                for (Position to : gameManager.getLegalMoves(from)) {
                    Move move = new Move(from, to);
                    if (board.getPieceAt(to) != null) legalCaptures.add(move);
                    else legalOthers.add(move);
                }
            }
        }

        if (!legalCaptures.isEmpty()) return legalCaptures.get(0);
        if (!legalOthers.isEmpty()) return legalOthers.get(new java.util.Random().nextInt(legalOthers.size()));
        return null;
    }

    private String buildStateJson() {
        StringBuilder json = new StringBuilder();
        json.append("{");

        Color turn = gameManager.getCurrentTurn();
        boolean inCheck = checkDetector.isInCheck(turn, board);
        GameState state = gameManager.getGameState();
        DrawReason drawReason = state == GameState.DRAW ? gameManager.getDrawReason() : DrawReason.NONE;

        json.append("\"turn\":\"").append(turn).append("\",");
        json.append("\"gameState\":\"").append(state).append("\",");
        json.append("\"drawReason\":\"").append(drawReason).append("\",");

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
        json.append("\"capturedBlack\":").append(capturedJson(Color.BLACK)).append(",");
        json.append("\"canUndo\":").append(gameManager.canUndo()).append(",");
        json.append("\"legalMoves\":").append(legalMovesJson(turn)).append(",");
        json.append("\"checkingPiecePositions\":").append(checkingPiecePositionsJson(turn)).append(",");
        json.append("\"kingInCheckPosition\":").append(inCheck ? kingInCheckPositionJson(turn) : "null");

        json.append("}");
        return json.toString();
    }

    private String legalMovesJson(Color color) {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Position from = new Position(row, col);
                for (Position to : gameManager.getLegalMoves(from)) {
                    if (!first) json.append(",");
                    json.append("{\"fromRow\":").append(row)
                            .append(",\"fromCol\":").append(col)
                            .append(",\"toRow\":").append(to.getRow())
                            .append(",\"toCol\":").append(to.getColumn()).append("}");
                    first = false;
                }
            }
        }
        json.append("]");
        return json.toString();
    }

    private String checkingPiecePositionsJson(Color color) {
        Position king = findKing(color);
        if (king == null) return "[]";

        Color opponent = color == Color.WHITE ? Color.BLACK : Color.WHITE;
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board.getPieceAt(new Position(row, col));
                if (piece != null && piece.getColor() == opponent
                        && piece.getPossibleMoves(board).stream()
                                .anyMatch(move -> move.equals(king))) {
                    if (!first) json.append(",");
                    json.append("\"").append(row).append("-").append(col).append("\"");
                    first = false;
                }
            }
        }
        json.append("]");
        return json.toString();
    }

    private String kingInCheckPositionJson(Color color) {
        Position king = findKing(color);
        return king == null ? "null"
                : "{\"row\":" + king.getRow() + ",\"col\":" + king.getColumn() + "}";
    }

    private Position findKing(Color color) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board.getPieceAt(new Position(row, col));
                if (piece != null && piece.getColor() == color && piece.getType() == com.chess.core.PieceType.KING) {
                    return new Position(row, col);
                }
            }
        }
        return null;
    }

    private String pieceJson(Piece piece) {
        if (piece == null) return "null";
        return "{\"id\":\"" + piece.getId() + "\",\"type\":\"" + piece.getType()
                + "\",\"color\":\"" + piece.getColor() + "\"}";
    }

    private String capturedJson(Color color) {
        List<Piece> captured = gameManager.getCapturedPieces(color);
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < captured.size(); i++) {
            if (i > 0) json.append(",");
            json.append(pieceJson(captured.get(i)));
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
        gameManager.reset();
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
