package com.chess.core;

import java.util.List;

public class CheckmateDetector {

    private final CheckDetector checkDetector = new CheckDetector();

    public boolean isCheckmate(Color color, ChessBoard board) {

        if (!checkDetector.isInCheck(color, board)) {
            return false;
        }

        for (int rowFrom = 0; rowFrom < 8; rowFrom++) {
            for (int colFrom = 0; colFrom < 8; colFrom++) {
                Position from = new Position(rowFrom, colFrom);
                Piece piece = board.getPieceAt(from);

                if (piece != null && piece.getColor() == color) {
                    List<Position> possibleMoves = piece.getMovementStrategy().getValidMoves(from, board);

                    for (Position to : possibleMoves) {

                        ChessBoard clonedBoard = board.clone();
                        clonedBoard.movePiece(from, to);

                        if (!checkDetector.isInCheck(color, clonedBoard)) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }
}