package com.chess.core;

import com.chess.ports.ChessBoard;
import java.util.List;

public class CheckDetector {

    public boolean isInCheck(Color color, ChessBoard board) {
        Position kingPos = findKingPosition(color, board);
        if (kingPos == null) return false;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board.getPieceAt(new Position(row, col));
                if (piece != null && piece.getColor() != color) {
                    List<Position> enemyMoves = piece.getPossibleMoves(board);
                    for (Position move : enemyMoves) {
                        if (move.getRow() == kingPos.getRow() && move.getColumn() == kingPos.getColumn()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private Position findKingPosition(Color color, ChessBoard board) {
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
}