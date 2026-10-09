package main.java.com.chess.core;

import java.util.List;

public class CheckDetector {

    public boolean isInCheck(Color color, ChessBoard board) {
        Position kingPosition = findKingPosition(color, board);
        if (kingPosition == null) {
            return false;
        }

        Color opponentColor = (color == Color.WHITE) ? Color.BLACK : Color.WHITE;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Position from = new Position(row, col);
                Piece piece = board.getPieceAt(from);

                if (piece != null && piece.getColor() == opponentColor) {
                    MovementStrategy strategy = piece.getMovementStrategy();
                    List<Position> validMoves = strategy.getValidMoves(from, board);

                    if (validMoves.contains(kingPosition)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private Position findKingPosition(Color color, ChessBoard board) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Position pos = new Position(row, col);
                Piece piece = board.getPieceAt(pos);

                if (piece != null && piece.getColor() == color && piece.getType() == PieceType.KING) {
                    return pos;
                }
            }
        }
        return null;
    }
}