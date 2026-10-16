package com.chess.core;

import com.chess.ports.ChessBoard;

public class CheckmateDetector {
    private final CheckDetector checkDetector = new CheckDetector();

    public boolean isCheckmate(Color color, ChessBoard board) {
        if (!checkDetector.isInCheck(color, board)) return false;

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Position pos = new Position(r, c);
                Piece piece = board.getPieceAt(pos);
                if (piece != null && piece.getColor() == color) {
                    for (Position dest : piece.getPossibleMoves(board)) {
                        ChessBoard clone = board.clone();
                        Piece clonePiece = clone.getPieceAt(pos);
                        clone.removePiece(pos);
                        clone.placePiece(clonePiece, dest);
                        if (!checkDetector.isInCheck(color, clone)) {
                            return false; 
                        }
                    }
                }
            }
        }
        return true;
    }
}