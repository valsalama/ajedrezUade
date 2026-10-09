package com.chess.core;

public class MoveValidator {

    public boolean isValidMove(ChessBoard board, Piece piece, Position destination) {
        if (board == null || piece == null || destination == null) {
            return false;
        }

        // 1) el destino tiene que estar dentro de lo que la pieza puede hacer
        if (!piece.getPossibleMoves(board).contains(destination)) {
            return false;
        }

        // 2) no se puede capturar una pieza propia
        Piece target = board.getPieceAt(destination);
        return target == null || target.getColor() != piece.getColor();
    }
}