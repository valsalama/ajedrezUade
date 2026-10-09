package com.chess.core;

import com.chess.ports.ChessBoard;
import java.util.ArrayDeque;
import java.util.Deque;

public class GameManager {
    private ChessBoard board;
    private Color currentTurn;
    private CheckDetector checkDetector;
    private Deque<Command> history;

    public GameManager(ChessBoard board) {
        this.board = board;
        this.currentTurn = Color.WHITE;
        this.checkDetector = new CheckDetector();
        this.history = new ArrayDeque<>();
    }

    public Color getCurrentTurn() { return currentTurn; }

    public MoveResult tryMove(Position from, Position to) {
        Piece piece = board.getPieceAt(from);
        if (piece == null || piece.getColor() != currentTurn) return MoveResult.NOT_YOUR_TURN;

        // D6: Simulación en clon para rechazar auto-jaque
        ChessBoard clonedBoard = board.clone();
        Piece clonedPiece = clonedBoard.getPieceAt(from);
        clonedBoard.removePiece(from);
        clonedBoard.placePiece(clonedPiece, to);

        if (checkDetector.isInCheck(currentTurn, clonedBoard)) {
            return MoveResult.INVALID_MOVE;
        }

        // Ejecutar movimiento real
        board.removePiece(from);
        board.placePiece(piece, to);
        piece.setPosition(to);
        
        currentTurn = (currentTurn == Color.WHITE) ? Color.BLACK : Color.WHITE;
        return MoveResult.SUCCESS;
    }

    // C5: Validación de Enroque
    public MoveResult tryCastle(Position kingFrom, Position rookFrom) {
        Piece king = board.getPieceAt(kingFrom);
        Piece rook = board.getPieceAt(rookFrom);

        if (king == null || rook == null || king.getType() != PieceType.KING || rook.getType() != PieceType.ROOK) 
            return MoveResult.INVALID_MOVE;
        if (king.getColor() != currentTurn || rook.getColor() != currentTurn) 
            return MoveResult.NOT_YOUR_TURN;
        if (king.hasMoved() || rook.hasMoved() || checkDetector.isInCheck(currentTurn, board)) 
            return MoveResult.INVALID_MOVE;

        int direction = kingFrom.getColumn() < rookFrom.getColumn() ? 1 : -1;
        for (int col = kingFrom.getColumn() + direction; col != rookFrom.getColumn(); col += direction) {
            if (!board.isEmpty(new Position(kingFrom.getRow(), col))) return MoveResult.INVALID_MOVE;
        }

        board.removePiece(kingFrom);
        board.removePiece(rookFrom);
        
        Position newKingPos = new Position(kingFrom.getRow(), kingFrom.getColumn() + (direction * 2));
        Position newRookPos = new Position(kingFrom.getRow(), newKingPos.getColumn() - direction);
        
        king.setPosition(newKingPos);
        rook.setPosition(newRookPos);
        board.placePiece(king, newKingPos);
        board.placePiece(rook, newRookPos);

        currentTurn = (currentTurn == Color.WHITE) ? Color.BLACK : Color.WHITE;
        return MoveResult.SUCCESS;
    }
}