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

    public MoveResult tryCastle(Color color, boolean kingSide) {
    if (color != currentTurn) {
        return MoveResult.NOT_YOUR_TURN;
    }

    int row = backRank(color);
    int rookCol = kingSide ? 7 : 0;

    Piece king = board.getPieceAt(new Position(row, 4));
    Piece rook = board.getPieceAt(new Position(row, rookCol));

    // 1) tienen que ser rey y torre propios
    if (king == null || rook == null
            || king.getType() != PieceType.KING || rook.getType() != PieceType.ROOK
            || king.getColor() != color || rook.getColor() != color) {
        return MoveResult.INVALID_MOVE;
    }

    // 2) ninguno se movió antes
    if (king.hasMoved() || rook.hasMoved()) {
        return MoveResult.INVALID_MOVE;
    }

    // 3) camino libre entre rey y torre
    int from = Math.min(4, rookCol) + 1;
    int to = Math.max(4, rookCol) - 1;
    for (int col = from; col <= to; col++) {
        if (!board.isEmpty(new Position(row, col))) {
            return MoveResult.INVALID_MOVE;
        }
    }

    // 4) ejecutar: el rey va 2 casilleros hacia la torre, la torre pasa del otro lado
    int kingDest = kingSide ? 6 : 2;
    int rookDest = kingSide ? 5 : 3;
    relocate(king, new Position(row, 4), new Position(row, kingDest));
    relocate(rook, new Position(row, rookCol), new Position(row, rookDest));

    switchTurn();
    return MoveResult.SUCCESS;
}

private int backRank(Color color) {
    return color == Color.WHITE ? 7 : 0;      // cambiar si tu equipo usa la convención inversa
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