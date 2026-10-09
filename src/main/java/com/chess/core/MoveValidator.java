package com.chess.core;

import com.chess.ports.ChessBoard;

public class MoveValidator {
    public boolean isValidMove(ChessBoard board, Piece piece, Position destination) {
        if (piece == null) return false;
        Piece destPiece = board.getPieceAt(destination);
        if (destPiece != null && destPiece.getColor() == piece.getColor()) return false;
        
        for (Position p : piece.getPossibleMoves(board)) {
            if (p.getRow() == destination.getRow() && p.getColumn() == destination.getColumn()) {
                return true;
            }
        }
        return false;
    }
}