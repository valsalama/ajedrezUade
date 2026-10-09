package chess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class GameManager {

    private final ChessBoard board;
    private final MoveValidator moveValidator;
    private Color currentTurn;
    private final Map<Color, List<Piece>> capturedPieces = new EnumMap<>(Color.class);

    public GameManager(ChessBoard board, MoveValidator moveValidator) {
        if (board == null || moveValidator == null) {
            throw new IllegalArgumentException("board y moveValidator son obligatorios");
        }
        this.board = board;
        this.moveValidator = moveValidator;
        this.currentTurn = Color.WHITE;                       // siempre arrancan las blancas
        capturedPieces.put(Color.WHITE, new ArrayList<>());
        capturedPieces.put(Color.BLACK, new ArrayList<>());
    }

    public MoveResult tryMove(Position from, Position to) {
        Piece piece = board.getPieceAt(from);

        if (piece == null) {
            return MoveResult.INVALID_MOVE;                   // casillero vacío
        }
        if (piece.getColor() != currentTurn) {
            return MoveResult.NOT_YOUR_TURN;
        }
        if (!moveValidator.isValidMove(board, piece, to)) {
            return MoveResult.INVALID_MOVE;
        }

        Piece captured = board.getPieceAt(to);
        if (captured != null) {
            board.removePiece(to);                            // la saca del tablero
            capturedPieces.get(captured.getColor()).add(captured);
        }

        relocate(piece, from, to);
        switchTurn();
        return MoveResult.SUCCESS;
    }

    public Color getCurrentTurn() {
        return currentTurn;
    }

    /** Piezas de ese color que ya fueron capturadas (las "perdidas"). */
    public List<Piece> getCapturedPieces(Color color) {
        return Collections.unmodifiableList(capturedPieces.get(color));
    }

    // ---------- helpers privados (los reutiliza tryCastle en C5) ----------

    private void relocate(Piece piece, Position from, Position to) {
        board.removePiece(from);
        board.placePiece(piece, to);
        piece.setPosition(to);                                // acá se marca hasMoved
    }

    private void switchTurn() {
        currentTurn = (currentTurn == Color.WHITE) ? Color.BLACK : Color.WHITE;
    }
}