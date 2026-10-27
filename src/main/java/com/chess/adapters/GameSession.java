package com.chess.adapters;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.chess.core.ArrayChessBoard;
import com.chess.core.Bishop;
import com.chess.core.CheckDetector;
import com.chess.core.Color;
import com.chess.core.Difficulty;
import com.chess.core.DrawReason;
import com.chess.core.GameManager;
import com.chess.core.GameState;
import com.chess.core.King;
import com.chess.core.Knight;
import com.chess.core.Move;
import com.chess.core.MoveResult;
import com.chess.core.MoveSearcher;
import com.chess.core.MoveValidator;
import com.chess.core.Pawn;
import com.chess.core.Piece;
import com.chess.core.PieceType;
import com.chess.core.Position;
import com.chess.core.Queen;
import com.chess.core.Rook;
import com.chess.ports.ChessBoard;

public class GameSession {

    private static final Color HUMAN_COLOR = Color.WHITE;

    private final ChessBoard board;
    private final GameManager gameManager;
    private final CheckDetector checkDetector;
    private final Random random;
    private Difficulty difficulty = Difficulty.MEDIUM;
    private volatile long lastUsed = System.currentTimeMillis();

    GameSession(ChessBoard board, GameManager gameManager, CheckDetector checkDetector, Random random) {
        this.board = board;
        this.gameManager = gameManager;
        this.checkDetector = checkDetector;
        this.random = random;
    }

    public static GameSession create() {
        ChessBoard board = new ArrayChessBoard();
        placeInitialPieces(board);
        CheckDetector checkDetector = new CheckDetector();
        GameManager gameManager = new GameManager(board, checkDetector, new MoveValidator());
        return new GameSession(board, gameManager, checkDetector, new Random());
    }

    public void touch() {
        lastUsed = System.currentTimeMillis();
    }

    public long getLastUsed() {
        return lastUsed;
    }

    public String stateJson() {
        return buildStateJson();
    }

    public String resetJson(Difficulty requested) {
        if (requested != null) difficulty = requested;
        resetBoard();
        return buildStateJson();
    }

    public String undoJson() {
        gameManager.undoLastRound(HUMAN_COLOR);
        return buildStateJson();
    }

    public String hintJson() {
        String json = "{\"hint\":null}";
        if (gameManager.getCurrentTurn() == HUMAN_COLOR && !isGameOver()) {
            Move best = new MoveSearcher(gameManager, board, checkDetector, random).findBestMove(4, 2000, 0);
            if (best != null) {
                json = "{\"hint\":{\"fromRow\":" + best.getFrom().getRow() + ",\"fromCol\":" + best.getFrom().getColumn()
                        + ",\"toRow\":" + best.getTo().getRow() + ",\"toCol\":" + best.getTo().getColumn() + "}}";
            }
        }
        return json;
    }

    public String positionsJson() {
        StringBuilder json = new StringBuilder("{\"boards\":[");
        List<Piece[][]> positions = gameManager.getPositions();
        for (int i = 0; i < positions.size(); i++) {
            if (i > 0) json.append(",");
            json.append("[");
            for (int row = 0; row < 8; row++) {
                if (row > 0) json.append(",");
                json.append("[");
                for (int col = 0; col < 8; col++) {
                    if (col > 0) json.append(",");
                    json.append(pieceJson(positions.get(i)[row][col]));
                }
                json.append("]");
            }
            json.append("]");
        }
        json.append("],\"moves\":[");
        List<Move> moves = gameManager.getMovesPlayed();
        for (int i = 0; i < moves.size(); i++) {
            if (i > 0) json.append(",");
            Move m = moves.get(i);
            json.append("{\"fromRow\":").append(m.getFrom().getRow()).append(",\"fromCol\":").append(m.getFrom().getColumn())
                    .append(",\"toRow\":").append(m.getTo().getRow()).append(",\"toCol\":").append(m.getTo().getColumn()).append("}");
        }
        json.append("]}");
        return json.toString();
    }

    public String moveJson(int fromRow, int fromCol, int toRow, int toCol, String promotionName) {
        MoveResult result;
        boolean gaveCheck = false;

        if (!inBounds(fromRow, fromCol) || !inBounds(toRow, toCol)) {
            result = MoveResult.INVALID_MOVE;
        } else {
            Position from = new Position(fromRow, fromCol);
            Position to = new Position(toRow, toCol);

            PieceType promotion = PieceType.QUEEN;
            boolean promotionInvalid = false;
            if (promotionName != null) {
                try {
                    promotion = PieceType.valueOf(promotionName);
                } catch (IllegalArgumentException e) {
                    promotionInvalid = true;
                }
            }

            result = promotionInvalid ? MoveResult.INVALID_MOVE : gameManager.tryMove(from, to, promotion);

            gaveCheck = result == MoveResult.SUCCESS
                    && gameManager.getCurrentTurn() == Color.BLACK
                    && checkDetector.isInCheck(Color.BLACK, board);

            if (result == MoveResult.SUCCESS && gameManager.getCurrentTurn() == Color.BLACK && !isGameOver()) {
                Move aiMove = chooseAiMove();
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
        return json.toString();
    }

    private boolean inBounds(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }

    private boolean isGameOver() {
        GameState state = gameManager.getGameState();
        return state == GameState.CHECKMATE || state == GameState.DRAW;
    }

    private Move chooseAiMove() {
        switch (difficulty) {
            case EASY:
                return pickEasyMove();
            case HARD:
                return new MoveSearcher(gameManager, board, checkDetector, random).findBestMove(4, 2500, 0);
            case MEDIUM:
            default:
                return new MoveSearcher(gameManager, board, checkDetector, random).findBestMove(2, 1500, 30);
        }
    }

    private Move pickEasyMove() {
        List<Move> captures = new ArrayList<>();
        List<Move> others = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Position from = new Position(r, c);
                for (Position to : gameManager.getLegalMoves(from)) {
                    Move move = new Move(from, to);
                    if (board.getPieceAt(to) != null) captures.add(move);
                    else others.add(move);
                }
            }
        }
        List<Move> all = new ArrayList<>(captures);
        all.addAll(others);
        if (all.isEmpty()) return null;
        if (!captures.isEmpty() && random.nextInt(100) < 40) {
            return captures.get(random.nextInt(captures.size()));
        }
        return all.get(random.nextInt(all.size()));
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
        json.append("\"difficulty\":\"").append(difficulty).append("\",");
        json.append("\"canUndo\":").append(gameManager.canUndo()).append(",");
        json.append("\"history\":").append(historyJson()).append(",");
        json.append("\"lastMove\":").append(lastMoveJson()).append(",");
        json.append("\"legalMoves\":").append(legalMovesJson()).append(",");
        json.append("\"checkingPiecePositions\":").append(checkingPiecePositionsJson(turn)).append(",");
        json.append("\"kingInCheckPosition\":").append(inCheck ? kingInCheckPositionJson(turn) : "null");

        json.append("}");
        return json.toString();
    }

    private String historyJson() {
        List<String> moves = gameManager.getMoveHistory();
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < moves.size(); i++) {
            if (i > 0) json.append(",");
            json.append("\"").append(moves.get(i)).append("\"");
        }
        json.append("]");
        return json.toString();
    }

    private String lastMoveJson() {
        Move last = gameManager.getLastMove();
        if (last == null) return "null";
        return "{\"fromRow\":" + last.getFrom().getRow() + ",\"fromCol\":" + last.getFrom().getColumn()
                + ",\"toRow\":" + last.getTo().getRow() + ",\"toCol\":" + last.getTo().getColumn() + "}";
    }

    private String legalMovesJson() {
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
                if (piece != null && piece.getColor() == color && piece.getType() == PieceType.KING) {
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
        placeInitialPieces(board);
        gameManager.reset();
    }

    private static void placeInitialPieces(ChessBoard board) {
        place(board, new Rook(Color.WHITE, p(0, 0)), 0, 0);
        place(board, new Knight(Color.WHITE, p(0, 1)), 0, 1);
        place(board, new Bishop(Color.WHITE, p(0, 2)), 0, 2);
        place(board, new Queen(Color.WHITE, p(0, 3)), 0, 3);
        place(board, new King(Color.WHITE, p(0, 4)), 0, 4);
        place(board, new Bishop(Color.WHITE, p(0, 5)), 0, 5);
        place(board, new Knight(Color.WHITE, p(0, 6)), 0, 6);
        place(board, new Rook(Color.WHITE, p(0, 7)), 0, 7);
        for (int i = 0; i < 8; i++) place(board, new Pawn(Color.WHITE, p(1, i)), 1, i);

        place(board, new Rook(Color.BLACK, p(7, 0)), 7, 0);
        place(board, new Knight(Color.BLACK, p(7, 1)), 7, 1);
        place(board, new Bishop(Color.BLACK, p(7, 2)), 7, 2);
        place(board, new Queen(Color.BLACK, p(7, 3)), 7, 3);
        place(board, new King(Color.BLACK, p(7, 4)), 7, 4);
        place(board, new Bishop(Color.BLACK, p(7, 5)), 7, 5);
        place(board, new Knight(Color.BLACK, p(7, 6)), 7, 6);
        place(board, new Rook(Color.BLACK, p(7, 7)), 7, 7);
        for (int i = 0; i < 8; i++) place(board, new Pawn(Color.BLACK, p(6, i)), 6, i);
    }

    private static Position p(int r, int c) {
        return new Position(r, c);
    }

    private static void place(ChessBoard board, Piece piece, int r, int c) {
        board.placePiece(piece, new Position(r, c));
    }
}
