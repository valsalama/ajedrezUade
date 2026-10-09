package com.chess.core;

import com.chess.ports.ChessBoard;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameManager {
    private ChessBoard board;
    private Color currentTurn;
    private CheckDetector checkDetector;
    private Deque<Command> history;
    private Map<Color, List<Piece>> capturedPieces; // Añadido para los tests de AgosL

    public GameManager(ChessBoard board) {
        this.board = board;
        this.currentTurn = Color.WHITE;
        this.checkDetector = new CheckDetector();
        this.history = new ArrayDeque<>();
        this.capturedPieces = new HashMap<>();
        this.capturedPieces.put(Color.WHITE, new ArrayList<>());
        this.capturedPieces.put(Color.BLACK, new ArrayList<>());
    }

    public Color getCurrentTurn() { return currentTurn; }

    public List<Piece> getCapturedPieces(Color color) { 
        return capturedPieces.get(color); 
    }

    public MoveResult tryMove(Position from, Position to) {
        Piece piece = board.getPieceAt(from);
        if (piece == null || piece.getColor() != currentTurn) return MoveResult.NOT_YOUR_TURN;

        ChessBoard clonedBoard = board.clone();
        Piece clonedPiece = clonedBoard.getPieceAt(from);
        clonedBoard.removePiece(from);
        clonedBoard.placePiece(clonedPiece, to);

        if (checkDetector.isInCheck(currentTurn, clonedBoard)) {
            return MoveResult.INVALID_MOVE;
        }

        // Si hay una pieza enemiga en el destino, registrarla como capturada
        Piece targetPiece = board.getPieceAt(to);
        if (targetPiece != null) {
            capturedPieces.get(currentTurn).add(targetPiece);
        }

        board.removePiece(from);
        board.placePiece(piece, to);
        piece.setPosition(to);
        
        currentTurn = (currentTurn == Color.WHITE) ? Color.BLACK : Color.WHITE;
        return MoveResult.SUCCESS;
    }

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
    public void undoLastMove() {
        if (!history.isEmpty()) {
            Command lastCommand = history.pop();
            lastCommand.undo();
            currentTurn = (currentTurn == Color.WHITE) ? Color.BLACK : Color.WHITE;
        }
    }
}